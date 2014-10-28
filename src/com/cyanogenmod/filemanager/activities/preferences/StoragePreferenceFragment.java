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

package com.cyanogenmod.filemanager.activities.preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;

/**
 * A class that manages the storage options
 */
public class StoragePreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "StoragePreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private static final String KEY_RESET_PASSWORD = "secure_storage_reset_password";
    private static final String KEY_DROP_STORAGE = "secure_storage_drop_storage";

    private Preference mResetPassword;
    private Preference mDropStorage;
    private CheckBoxPreference mDelayedSync;

    private final BroadcastReceiver mMountStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().compareTo(
                    FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED) == 0) {
                updatePreferences();
            }
        }
    };

    private final OnPreferenceChangeListener mOnChangeListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) {
                Log.d(TAG,
                    String.format("New value for %s: %s",  //$NON-NLS-1$
                            key,
                            String.valueOf(newValue)));
            }

            return true;
        }
    };

    private final OnPreferenceClickListener mOnClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.equals(mResetPassword)) {
                getSecureConsole().requestReset(getActivity());
            } else if (preference.equals(mDropStorage)) {
                getSecureConsole().requestDrop(getActivity());
            }
            return false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
        getActivity().registerReceiver(mMountStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mMountStatusReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update the preferences
        updatePreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(Preferences.SETTINGS_FILENAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_storage);

        // Reset password
        mResetPassword = findPreference(KEY_RESET_PASSWORD);
        mResetPassword.setOnPreferenceClickListener(mOnClickListener);

        // Drop storage
        mDropStorage = findPreference(KEY_DROP_STORAGE);
        mDropStorage.setOnPreferenceClickListener(mOnClickListener);

        // Delayed sync
        this.mDelayedSync =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_SECURE_STORAGE_DELAYED_SYNC.getId());
        this.mDelayedSync.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Update the preferences
        updatePreferences();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_storage);
    }

    /**
     * Method that returns the secure console instance
     *
     * @return SecureConsole The secure console
     */
    private SecureConsole getSecureConsole() {
        int bufferSize = getActivity().getResources().getInteger(R.integer.buffer_size);
        return SecureConsole.getInstance(getActivity(), bufferSize);
    }

    /**
     * Check the preferences status
     */
    @SuppressWarnings("deprecation")
    private void updatePreferences() {
        boolean secureStorageExists = SecureConsole.getSecureStorageRoot().getFile().exists();
        if (mResetPassword != null) {
            mResetPassword.setEnabled(secureStorageExists);
        }
        if (mDropStorage != null) {
            mDropStorage.setEnabled(secureStorageExists);
        }
    }
}
