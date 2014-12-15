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

package com.cyanogenmod.filemanager.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.cyanogenmod.filemanager.providers.MimeTypeIndexProvider;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * MimeTypeIndexService
 * <pre>
 *    Service intended to index space used by mime type
 * </pre>
 *
 * @see {@link android.app.IntentService}
 */
public class MimeTypeIndexService extends IntentService {

    // Constants
    private static final String TAG = MimeTypeIndexService.class.getSimpleName();
    public static final String ACTION_START_INDEX = "com.cyanogenmod.filemanager" +
            ".ACTION_START_INDEX";
    public static final String EXTRA_FILE_ROOT = "extra_file_root";

    /**
     * Constructor
     */
    public MimeTypeIndexService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent(" + intent + ")");
        if (intent == null) {
            Log.w(TAG, "Intent passed was null");
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "Action: " + action);
        if (TextUtils.isEmpty(action)) {
            Log.w(TAG, "Failed to parse action");
            return;
        }
        String fileRoot = intent.getStringExtra(EXTRA_FILE_ROOT);
        if (TextUtils.isEmpty(fileRoot)) {
            Log.w(TAG, "Empty file root, bailing out");
            return;
        }
        if (ACTION_START_INDEX.equalsIgnoreCase(action)) {
            performIndexAction(fileRoot);
        }
    }

    private void performIndexAction(String fileRoot) {
        Log.v(TAG, "performIndexAction(" + fileRoot + ")");
        if (TextUtils.isEmpty(fileRoot)) {
            Log.w(TAG, "Empty or null file root '" + fileRoot + "'");
            return;
        }
        Log.i(TAG, "Starting mime type usage indexing on '" + fileRoot + "'");
        fileRoot = fileRoot.trim();
        File rootFile = new File(fileRoot);
        Map<MimeTypeCategory, Long> spaceCalculationMap =
                new HashMap<MimeTypeCategory, Long>();
        calculateUsageByType(rootFile, spaceCalculationMap);
        ContentValues[] valuesList = new ContentValues[spaceCalculationMap.keySet().size()];
        int i = 0;
        for (MimeTypeCategory category : spaceCalculationMap.keySet()) {
            Log.d(TAG, "" + category + " = " + spaceCalculationMap.get(category));
            ContentValues values = new ContentValues();
            values.put(MimeTypeIndexProvider.COLUMN_FILE_ROOT, fileRoot);
            values.put(MimeTypeIndexProvider.COLUMN_CATEGORY, category.name());
            values.put(MimeTypeIndexProvider.COLUMN_SIZE, spaceCalculationMap.get(category));
            valuesList[i] = values;
            i++;
        }
        MimeTypeIndexProvider.clearMountPointUsages(this, fileRoot); // Clear old data
        getContentResolver().bulkInsert(MimeTypeIndexProvider.getContentUri(), valuesList);
    }

    private class FileOnlyFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file != null && !file.isDirectory() && file.isFile();
        }
    }

    private class DirectoryOnlyFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file != null && file.isDirectory();
        }
    }

    private void calculateUsageByType(File root, Map<MimeTypeCategory, Long> groupUsageMap) {
        File[] dirs = root.listFiles(new DirectoryOnlyFileFilter());
        File[] files = root.listFiles(new FileOnlyFileFilter());
        if (dirs != null) {
            // Recurse directories
            for (File dir : dirs) {
                calculateUsageByType(dir, groupUsageMap);
            }
        }
        if (files != null) {
            // Iterate every file
            for (File file : files) {
                MimeTypeCategory category = MimeTypeHelper.getCategory(this, file);
                long size = file.length();
                if (!groupUsageMap.containsKey(category)) {
                    groupUsageMap.put(category, size);
                } else {
                    long newSum = groupUsageMap.get(category) + size;
                    groupUsageMap.put(category, newSum);
                }
            }
        }
    }

    /**
     * Kick off an indexing job for the provided file root or mount point root
     *
     * @param context  {@link android.content.Context}
     * @param fileRoot {@link java.lang.String}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public static void indexFileRoot(Context context, String fileRoot) throws
            IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null");
        }
        // Start indexing the external storage
        Intent intent = new Intent(context, MimeTypeIndexService.class);
        intent.setAction(MimeTypeIndexService.ACTION_START_INDEX);
        intent.putExtra(MimeTypeIndexService.EXTRA_FILE_ROOT, fileRoot);
        context.startService(intent);
    }

}
