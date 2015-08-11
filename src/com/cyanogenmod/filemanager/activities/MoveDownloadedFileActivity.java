package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.cyanogenmod.filemanager.service.MoveFileService;

public class MoveDownloadedFileActivity extends Activity {
    private static final String TAG = MoveDownloadedFileActivity.class.getSimpleName();

    private static final String EXTRA_FILE_PATH = "extra_file_path";

    private static final int REQUEST_MOVE = 1000;

    private String mFilePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_FILE_PATH)) {
            Log.w(TAG, "Null intent or no file specified");
            finish();
        }
        mFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
        Intent moveIntent = new Intent(this, PickerActivity.class);
        moveIntent.setAction(PickerActivity.INTENT_FOLDER_SELECT);
        moveIntent.putExtra(PickerActivity.EXTRA_ACTION, PickerActivity.ACTION_MODE.MOVE.ordinal());
        startActivityForResult(moveIntent, REQUEST_MOVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MOVE) {
            if (resultCode == RESULT_OK && data.hasExtra(PickerActivity.EXTRA_FOLDER_PATH)) {
                // TODO: do something magical
                String destinationPath = data.getStringExtra(PickerActivity.EXTRA_FOLDER_PATH);
                Log.d(TAG, String.format("Moving %s to %s", mFilePath, destinationPath));
                Intent intent = new Intent(this, MoveFileService.class);
                intent.putExtra(MoveFileService.EXTRA_SOURCE_FILE_PATH, mFilePath);
                intent.putExtra(MoveFileService.EXTRA_DESTINATION_FILE_PATH, destinationPath);
                startService(intent);
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}