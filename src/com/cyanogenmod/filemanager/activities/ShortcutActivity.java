/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import os.ransj.filemanager.R;
import os.ransj.tracker.TrackerActivity;

import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;

/**
 * The activity for handle the desktop shortcuts create by the app.
 */
public class ShortcutActivity extends TrackerActivity implements OnCancelListener, OnDismissListener {

    private static final String TAG = "ShortcutActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();
                }
            }
        }
    };

    /**
     * Constant for extra information about the type of the shortcut.<br/>
     * <br/>
     * <ul>
     * <li><code>navigate</code>. For folders</li>
     * <li><code>open</code>. For files</li>
     * </ul>
     */
    public static final String EXTRA_TYPE = "extra_shortcut_type"; //$NON-NLS-1$

    /**
     * Constant for extra information about the absolute path
     */
    public static final String EXTRA_FSO = "extra_shortcut_fso"; //$NON-NLS-1$

    /**
     * Navigable shortcut type.
     */
    public static final String SHORTCUT_TYPE_NAVIGATE = "navigate"; //$NON-NLS-1$

    /**
     * Openable shortcut type.
     */
    public static final String SHORTCUT_TYPE_OPEN = "open"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        if (DEBUG) {
            Log.d(TAG, "ShortcutActivity.onCreate"); //$NON-NLS-1$
        }

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileManagerSettings.INTENT_THEME_CHANGED);
        registerReceiver(this.mNotificationReceiver, filter);

        // Set the theme before setContentView
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, true);

        //Save state
        super.onCreate(state);

        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "ShortcutActivity.onDestroy"); //$NON-NLS-1$
        }

        // Unregister the receiver
        try {
            unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * Initialize the activity. This method handles the passed intent, opens
     * the appropriate activity and ends.
     */
    private void init() {
        try {
            String type = getIntent().getStringExtra(EXTRA_TYPE);
            String path = getIntent().getStringExtra(EXTRA_FSO);

            // Check that i have a valid information
            if (type == null || type.length() == 0 || path == null || path.length() == 0) {
                // Something is wrong
                Log.w(TAG,
                        String.format(
                            "The shortcut intent couldn't be handled. Intent: %s", //$NON-NLS-1$
                            getIntent()));
                DialogHelper.showToast(this, R.string.shortcut_failed_msg, Toast.LENGTH_SHORT);
                finish();
                return;
            }
            if (type.compareTo(SHORTCUT_TYPE_NAVIGATE) != 0 &&
                type.compareTo(SHORTCUT_TYPE_OPEN) != 0) {
                // Something is wrong
                Log.w(TAG,
                        String.format(
                                "The shortcut intent type is unknown: %s", type)); //$NON-NLS-1$
                DialogHelper.showToast(this, R.string.shortcut_failed_msg, Toast.LENGTH_SHORT);
                finish();
                return;
            }

            // Initializes the console
            initializeConsole();

            // Retrieve the fso
            FileSystemObject fso =
                    CommandHelper.getFileInfo(getApplicationContext(), path, true, null);
            if (fso == null) {
                // Something is wrong
                Log.w(TAG,
                        String.format(
                                "The fso not exists: %s", path)); //$NON-NLS-1$
                DialogHelper.showToast(this, R.string.shortcut_failed_msg, Toast.LENGTH_SHORT);
                finish();
                return;
            }

            // Check what type of shortcut is registered and apply the best action
            if (type.compareTo(SHORTCUT_TYPE_NAVIGATE) == 0) {
                // We have to finish here; this activity is only a wrapper
                finish();

                // Forward to the NavigationActivity
                Intent intent = new Intent(this, NavigationActivity.class);
                intent.putExtra(NavigationActivity.EXTRA_NAVIGATE_TO, fso.getFullPath());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

            } else {
                // Open the file. Delegate in action policy
                IntentsActionPolicy.openFileSystemObject(this, fso, false, this, this);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle the shortcut intent.", e); //$NON-NLS-1$
            DialogHelper.showToast(this, R.string.shortcut_failed_msg, Toast.LENGTH_SHORT);
            finish();
        }
    }

    /**
     * Method that initializes a console
     */
    private boolean initializeConsole() {
        try {
            ConsoleBuilder.getConsole(this);
            // There is a console allocated. Use it.
            return true;
        } catch (Throwable _throw) {
            // Capture the exception
            ExceptionUtil.translateException(this, _throw, true, false);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        // We have to finish here; this activity is only a wrapper
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        // We have to finish here; this activity is only a wrapper
        finish();
    }

    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, true);
    }

}
