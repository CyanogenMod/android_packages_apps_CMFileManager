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
import android.provider.BaseColumns;
import android.provider.MediaStore;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class with useful methods to extract media data.
 */
public final class MediaHelper {

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
}
