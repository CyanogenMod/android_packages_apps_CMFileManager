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

package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.cyanogenmod.filemanager.service.MoveFileService;

public class MoveDownloadedFileActivity extends Activity {
    private static final String TAG = MoveDownloadedFileActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String EXTRA_FILE_PATH = "extra_file_path";

    private static final int REQUEST_MOVE = 1000;

    private String mFilePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent == null || !intent.hasExtra(EXTRA_FILE_PATH)) {
                Log.w(TAG, "Null intent or no file specified");
                finish();
            } else {
                mFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
                Intent moveIntent = new Intent(this, PickerActivity.class);
                moveIntent.setAction(PickerActivity.INTENT_FOLDER_SELECT);
                moveIntent.putExtra(PickerActivity.EXTRA_ACTION,
                        PickerActivity.ACTION_MODE.MOVE.ordinal());
                startActivityForResult(moveIntent, REQUEST_MOVE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MOVE) {
            if (resultCode == RESULT_OK && data.hasExtra(PickerActivity.EXTRA_FOLDER_PATH)) {
                String destinationPath = data.getStringExtra(PickerActivity.EXTRA_FOLDER_PATH);
                if (DEBUG) Log.d(TAG, String.format("Moving %s to %s", mFilePath, destinationPath));
                Intent intent = new Intent(this, MoveFileService.class);
                intent.putExtra(MoveFileService.EXTRA_SOURCE_FILE_PATH, mFilePath);
                intent.putExtra(MoveFileService.EXTRA_DESTINATION_FILE_PATH, destinationPath);
                startService(intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        finish();
    }
}