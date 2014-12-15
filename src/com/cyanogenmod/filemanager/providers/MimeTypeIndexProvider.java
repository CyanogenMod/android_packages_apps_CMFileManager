/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.cyanogenmod.filemanager.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import static android.content.UriMatcher.NO_MATCH;

/**
 * MimeTypeIndexProvider
 * <pre>
 *     Provider for handling access of mime type indexes
 * </pre>
 *
 * @see {@link android.content.ContentProvider}
 */
public class MimeTypeIndexProvider extends ContentProvider {

    // Constants
    private static final String TAG = MimeTypeIndexProvider.class.getSimpleName();
    private static final String AUTHORITY = "com.cyanogenmod.filemanager.providers.index";
    private static final int ID_INDEX = 1;
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +
            DatabaseHelper.INDEX_TABLE);

    private static final UriMatcher sUriMatcher = new UriMatcher(NO_MATCH);
    public static final String COLUMN_FILE_ROOT = "file_root";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_SIZE = "size";

    public static Uri getContentUri() {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).path
                (DatabaseHelper.INDEX_TABLE).build();
    }

    static {
        sUriMatcher.addURI(AUTHORITY, DatabaseHelper.INDEX_TABLE, ID_INDEX);
    }

    private SQLiteDatabase mSQLiteDatabase;

    @Override
    public boolean onCreate() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        mSQLiteDatabase = dbHelper.getWritableDatabase();
        return mSQLiteDatabase != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String tableName = null;
        switch (sUriMatcher.match(uri)) {
            case ID_INDEX:
                tableName = DatabaseHelper.INDEX_TABLE;
                break;
            default:
                throw new RuntimeException("URI not supported!");
        }

        Cursor cursor = mSQLiteDatabase.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        String tableName = null;
        switch (sUriMatcher.match(uri)) {
            case ID_INDEX:
                tableName = DatabaseHelper.INDEX_TABLE;
                break;
            default:
                throw new RuntimeException("URI not supported!");
        }
        long rowId = mSQLiteDatabase.insert(tableName, null, contentValues);
        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Failed to add new record");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String tableName = null;
        switch (sUriMatcher.match(uri)) {
            case ID_INDEX:
                tableName = DatabaseHelper.INDEX_TABLE;
                break;
            default:
                throw new RuntimeException("URI not supported!");
        }
        int count = mSQLiteDatabase.delete(tableName, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[]
            selectionArgs) {
        throw new RuntimeException("MimeTypeIndexProvider::update(): Not implemented!");
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        // Constants
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "mime_type_index";
        static final String INDEX_TABLE = "type_index";
        static final String CREATE_INDEX_TABLE = "CREATE TABLE IF NOT EXISTS " +
                INDEX_TABLE +
                " ( " +
                "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`" + COLUMN_FILE_ROOT + "` TEXT, " +
                "`" + COLUMN_CATEGORY + "` TEXT, " +
                "`" + COLUMN_SIZE + "` INTEGER " +
                ")";

        /**
         * Constructor
         *
         * @param context {@link android.content.Context}
         */
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_INDEX_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + INDEX_TABLE);
            onCreate(sqLiteDatabase);
        }
    }

    /**
     * Get the mount point usage data via sql cursor
     *
     * @param context  {@link android.content.Context} not null
     * @param fileRoot {@link java.lang.String} not null or empty
     *
     * @return {@link android.database.Cursor}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public static Cursor getMountPointUsage(Context context, String fileRoot) throws
            IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        if (TextUtils.isEmpty(fileRoot)) {
            throw new IllegalArgumentException("'fileRoot' cannot be null or empty!");
        }
        String selection = COLUMN_FILE_ROOT + " = ?";
        String[] selectionArgs = new String[] { fileRoot };
        return context.getContentResolver().query(MimeTypeIndexProvider.getContentUri(),
                null, selection, selectionArgs, null);
    }

    /**
     * Clear the mount point usage data for the file root
     *
     * @param context  {@link android.content.Context} not null
     * @param fileRoot {@link java.lang.String} not null or empty
     *
     * @return {@link java.lang.Integer}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public static int clearMountPointUsages(Context context, String fileRoot)  throws
            IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        if (TextUtils.isEmpty(fileRoot)) {
            throw new IllegalArgumentException("'fileRoot' cannot be null or empty!");
        }
                String selection = COLUMN_FILE_ROOT + " = ?";
        String[] selectionArgs = new String[] { fileRoot };
        return context.getContentResolver().delete(MimeTypeIndexProvider.getContentUri(), selection, selectionArgs);
    }

}
