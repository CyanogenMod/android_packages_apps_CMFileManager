/*
* Copyright (C) 2015 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.providers.secure;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * SecureCacheCleanupService
 * <pre>
 *    Service that cleans up cache
 * </pre>
 *
 * @see {@link android.app.IntentService}
 */
public class SecureCacheCleanupService extends IntentService {

    // Constants
    private static final String ACTION_START = "com.cyanogenmod.filemanager.ACTION_START_CLEANUP";
    private static final String NAME = "cleanup-service";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public SecureCacheCleanupService() {
        super(NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            scheduleCleanup(this);
        } else if (ACTION_START.equals(action)) {
            cleanupOperation();
        }
    }

    private void cleanupOperation() {
        File cacheDir = new File(getExternalCacheDir(), SecureChoiceClickListener.CACHE_DIR);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.HOUR, -1);
        Date cutoff = calendar.getTime();

        if (!cacheDir.exists()) {
            return;
        }

        // Get list of files
        File[] files = cacheDir.listFiles();

        if (files == null) {
            return;
        }

        // Delete all, won't run if list is empty
        for (File file : files) {
            FileSystemObject fso = FileHelper.createFileSystemObject(file);
            Date lastAccessDate = fso.getLastAccessedTime();
            if (lastAccessDate.before(cutoff)) {
                file.delete();
            }
        }

        // Check again after deletion
        files = cacheDir.listFiles();

        // If no files, cancel alarm
        if (files == null || files.length < 1) {
            cancelAlarm(this);
        }

    }

    /**
     * Schedule a cleanup alarm
     *
     * @param context {@link android.content.Context}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public static void scheduleCleanup(Context context) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SecureCacheCleanupService.class);
        intent.setAction(ACTION_START);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 1000,
                AlarmManager.INTERVAL_HOUR, pendingIntent);
    }

    /**
     * Cancel a cleanup alarm
     *
     * @param context {@link android.content.Context}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public static void cancelAlarm(Context context) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SecureCacheCleanupService.class);
        intent.setAction(ACTION_START);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }
}
