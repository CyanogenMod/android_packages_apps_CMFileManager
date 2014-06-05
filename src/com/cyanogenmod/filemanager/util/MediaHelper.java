/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.cyanogenmod.filemanager.compat.CompatUtils;

/**
 * A helper class with useful methods to extract media data.
 */
public final class MediaHelper {

    private static final String EMULATED_STORAGE_SOURCE = System.getenv("EMULATED_STORAGE_SOURCE");
    private static final String EMULATED_STORAGE_TARGET = System.getenv("EMULATED_STORAGE_TARGET");
    private static final String EXTERNAL_STORAGE = System.getenv("EXTERNAL_STORAGE");

    private static final String INTERNAL_VOLUME = "internal";
    private static final String EXTERNAL_VOLUME = "external";

    /**
     * URIs that are relevant for determining album art;
     * useful for content observer registration
     */
    public static final Uri[] RELEVANT_URIS = new Uri[] {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    };

    /**
     * Method that returns an array with all the unique albums paths and ids.
     *
     * @param cr The ContentResolver
     * @return Map<String, Long> The albums map
     */
    public static Map<String, Long> getAllAlbums(ContentResolver cr) {
        Map<String, Long> albums = new HashMap<String, Long>();
        final String[] projection =
                {
                    "distinct " + MediaStore.Audio.Media.ALBUM_ID,
                    "substr(" + MediaStore.Audio.Media.DATA + ", 0, length(" +
                            MediaStore.Audio.Media.DATA + ") - length(" +
                            MediaStore.Audio.Media.DISPLAY_NAME + "))"
                };
        final String where = MediaStore.Audio.Media.IS_MUSIC + " = ?";
        Cursor c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, where, new String[]{"1"}, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    long albumId = c.getLong(0);
                    String albumPath = c.getString(1);
                    albums.put(albumPath, albumId);
                }
            } finally {
                c.close();
            }
        }
        return albums;
    }

    /**
     * Method that returns the album thumbnail path by its identifier.
     *
     * @param cr The ContentResolver
     * @param albumId The album identifier to search
     * @return String The album thumbnail path
     */
    public static String getAlbumThumbnailPath(ContentResolver cr, long albumId) {
        final String[] projection = {MediaStore.Audio.Albums.ALBUM_ART};
        final String where = BaseColumns._ID + " = ?";
        Cursor c = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection, where, new String[]{String.valueOf(albumId)}, null);
        try {
            if (c != null && c.moveToNext()) {
                return c.getString(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    /**
     * Method that converts a file reference to a content uri reference
     *
     * @param cr A content resolver
     * @param file The file reference
     * @return Uri The content uri or null if file not exists in the media database
     */
    public static Uri fileToContentUri(ContentResolver cr, File file) {
        // Normalize the path to ensure media search
        final String normalizedPath = normalizeMediaPath(file.getAbsolutePath());

        // Check in external and internal storages
        Uri uri = fileToContentUri(cr, normalizedPath, EXTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        uri = fileToContentUri(cr, normalizedPath, INTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        return null;
    }

    /**
     * Method that converts a file reference to a content uri reference
     *
     * @param cr A content resolver
     * @param path The path to search
     * @param volume The volume
     * @return Uri The content uri or null if file not exists in the media database
     */
    private static Uri fileToContentUri(ContentResolver cr, String path, String volume) {
        final String[] projection = {BaseColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};
        final String where = MediaColumns.DATA + " = ?";
        Uri baseUri = MediaStore.Files.getContentUri(volume);
        Cursor c = cr.query(baseUri, projection, where, new String[]{path}, null);
        try {
            if (c != null && c.moveToNext()) {
                int type = c.getInt(c.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.MEDIA_TYPE));
                if (type != 0) {
                    // Do not force to use content uri for no media files
                    long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
                    return Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    /**
     * Method that converts a content uri to a file system path
     *
     * @param cr The content resolver
     * @param uri The content uri
     * @return File The file reference
     */
    public static File contentUriToFile(ContentResolver cr, Uri uri) {
        // Sanity checks
        if (uri == null || uri.getScheme() == null || uri.getScheme().compareTo("content") != 0) {
            return null;
        }

        // Retrieve the request id
        long id = 0;
        try {
            id = Long.parseLong(new File(uri.getPath()).getName());
        } catch (NumberFormatException nfex) {
            return null;
        }

        // Check in external and internal storages
        File file = mediaIdToFile(cr, id, EXTERNAL_VOLUME);
        if (file != null) {
            return file;
        }
        file = mediaIdToFile(cr, id, INTERNAL_VOLUME);
        if (file != null) {
            return file;
        }
        return null;
    }

    /**
     * Method that converts a content uri to a file system path
     *
     * @param cr The content resolver
     * @param id The media database id
     * @param volume The volume
     * @return File The file reference
     */
    private static File mediaIdToFile(ContentResolver cr, long id, String volume) {
        final String[] projection = {MediaColumns.DATA};
        final String where = MediaColumns._ID + " = ?";
        Uri baseUri = MediaStore.Files.getContentUri(volume);
        Cursor c = cr.query(baseUri, projection, where, new String[]{String.valueOf(id)}, null);
        try {
            if (c != null && c.moveToNext()) {
                return new File(c.getString(c.getColumnIndexOrThrow(MediaColumns.DATA)));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    /**
     * Method that converts a not standard media mount path to a standard media path
     *
     * @param path The path to normalize
     * @return String The normalized media path
     */
    public static String normalizeMediaPath(String path) {
        // Retrieve all the paths and check that we have this environment vars
        if (TextUtils.isEmpty(EMULATED_STORAGE_SOURCE) ||
                TextUtils.isEmpty(EMULATED_STORAGE_TARGET) ||
                TextUtils.isEmpty(EXTERNAL_STORAGE)) {
            return path;
        }

        // We need to convert EMULATED_STORAGE_SOURCE -> EMULATED_STORAGE_TARGET
        if (path.startsWith(EMULATED_STORAGE_SOURCE)) {
            path = path.replace(EMULATED_STORAGE_SOURCE, EMULATED_STORAGE_TARGET);
        }
        // We need to convert EXTERNAL_STORAGE -> EMULATED_STORAGE_TARGET / userId
        if (path.startsWith(EXTERNAL_STORAGE)) {
        	final String userId = String.valueOf(CompatUtils.getClsHideIntStaticMethod(UserHandle.class, "myUserId", Process.myUid()));
//            final String userId = String.valueOf(UserHandle.myUserId());
            final String target = new File(EMULATED_STORAGE_TARGET, userId).getAbsolutePath();
            path = path.replace(EXTERNAL_STORAGE, target);
        }
        return path;
    }
}
