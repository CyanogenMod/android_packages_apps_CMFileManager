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

package com.cyanogenmod.filemanager.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.CommandHelper;

import java.io.File;

public class MoveFileService extends IntentService {
    private static final String TAG = MoveFileService.class.getSimpleName();

    private static final String FILE_MANAGER_PACKAGE = "com.cyanogenmod.filemanager";
    private static final String FILE_MANAGER_ACTIVITY =
            FILE_MANAGER_PACKAGE + ".activities.MainActivity";
    private static final String EXTRA_NAVIGATE_TO = "extra_navigate_to";

    public static final String EXTRA_SOURCE_FILE_PATH = "extra_source_file_path";
    public static final String EXTRA_DESTINATION_FILE_PATH = "extra_destination_file_path";

    private static final String TAG_SEPARATOR = ":::";

    public MoveFileService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_SOURCE_FILE_PATH)) {
            Log.w(TAG, "Source file path not provided");
            return;
        }
        if (!intent.hasExtra(EXTRA_DESTINATION_FILE_PATH)) {
            Log.w(TAG, "Destination file path not provided");
            return;
        }
        String srcPath = intent.getStringExtra(EXTRA_SOURCE_FILE_PATH);
        String dstPath = intent.getStringExtra(EXTRA_DESTINATION_FILE_PATH);
        String tag = getTagForNotification(srcPath, dstPath);

        showMovingNotification(tag);
        new MoveFileTask().execute(srcPath, dstPath);
    }

    private void showMovingNotification(String tag) {
        NotificationManager nm = NotificationManager.from(this);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.moving_file_notification_title));
        builder.setSmallIcon(R.drawable.ic_object_move);
        builder.setProgress(0, 100, true);
        nm.notify(tag, 0, builder.build());
    }

    private void showFileMovedNotification(String tag, String filePath) {
        NotificationManager nm = NotificationManager.from(this);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.move_complete_notification_title));
        builder.setContentInfo(getString(R.string.move_complete_notification_description));
        builder.setSmallIcon(R.drawable.ic_object_move);

        Intent intent = createFileManagerNavigateToIntent(filePath);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pi);
        nm.notify(tag, 0, builder.build());
    }

    private Intent createFileManagerNavigateToIntent(String filePath) {
        File downloadedFile = new File(filePath);
        String path = downloadedFile.getParent();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(FILE_MANAGER_PACKAGE, FILE_MANAGER_ACTIVITY));
        intent.putExtra(EXTRA_NAVIGATE_TO, path);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    private static String getTagForNotification(String srcPath, String dstPath) {
        return srcPath + TAG_SEPARATOR + dstPath;
    }

    class MoveFileTask extends AsyncTask<String, Integer, Boolean> {
        private String mSrcPath;
        private String mDstPath;

        @Override
        protected Boolean doInBackground(String... params) {
            mSrcPath = params[0];
            mDstPath = params[1];
            String fileName = mSrcPath.substring(mSrcPath.lastIndexOf(File.separator) + 1);
            try {
                CommandHelper.move(MoveFileService.this, mSrcPath, mDstPath, fileName, null, null);
            } catch (Exception e) {
                return Boolean.FALSE;
            }

            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (Boolean.TRUE.equals(result)) {
                showFileMovedNotification(getTagForNotification(mSrcPath, mDstPath), mDstPath);
            }
        }
    }
}
