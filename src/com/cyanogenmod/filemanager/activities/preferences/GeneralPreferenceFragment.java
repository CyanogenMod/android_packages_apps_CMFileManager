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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import com.cyanogenmod.filemanager.FileManagerApplication;
import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.ObjectStringIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.util.AndroidHelper;

/**
 * A class that manages the commons options of the application
 */
public class GeneralPreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "GeneralPreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private CheckBoxPreference mCaseSensitiveSort;
    private ListPreference mFiletimeFormatMode;
    private ListPreference mFreeDiskSpaceWarningLevel;
    private CheckBoxPreference mComputeFolderStatistics;
    private CheckBoxPreference mDisplayThumbs;
    private CheckBoxPreference mUseFlinger;
    private ListPreference mAccessMode;
    private CheckBoxPreference mRestrictSecondaryUsersAccess;
    private CheckBoxPreference mDebugTraces;

    /**
     * @hide
     */
    boolean mLoaded = false;

    private final OnPreferenceChangeListener mOnChangeListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            boolean ret = true;
            boolean notify = false;

            String key = preference.getKey();
            if (DEBUG) {
                Log.d(TAG,
                    String.format("New value for %s: %s",  //$NON-NLS-1$
                            key,
                            String.valueOf(newValue)));
            }

            // Filetime format mode
            if (FileManagerSettings.SETTINGS_FILETIME_FORMAT_MODE.
                    getId().compareTo(key) == 0) {
                String value = (String)newValue;
                int valueId = Integer.valueOf(value).intValue();
                String[] labels = getResources().getStringArray(
                        R.array.filetime_format_mode_labels);
                                    preference.setSummary(labels[valueId]);
            }

            // Disk usage warning level
            else if (FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.
                    getId().compareTo(key) == 0) {
                String value = (String)newValue;
                preference.setSummary(
                        getResources().getString(
                                R.string.pref_disk_usage_warning_level_summary, value));
            }

            // Access mode
            else if (FileManagerSettings.SETTINGS_ACCESS_MODE.getId().compareTo(key) == 0) {
                Activity activity = GeneralPreferenceFragment.this.getActivity();

                String value = (String)newValue;
                AccessMode oldMode = FileManagerApplication.getAccessMode();
                AccessMode newMode = AccessMode.fromId(value);
                if (oldMode.compareTo(newMode) != 0) {
                    // The mode was changes. Change the console
                    if (newMode.compareTo(AccessMode.ROOT) == 0) {
                        if (!ConsoleBuilder.changeToPrivilegedConsole(
                                activity.getApplicationContext())) {
                            value = String.valueOf(oldMode.ordinal());
                            ret = false;
                        }
                    } else {
                        if (!ConsoleBuilder.changeToNonPrivilegedConsole(
                                activity.getApplicationContext())) {
                            value = String.valueOf(oldMode.ordinal());
                            ret = false;
                        }
                    }
                }

                int valueId = Integer.valueOf(value).intValue();
                String[] summary = getResources().getStringArray(
                        R.array.access_mode_summaries);
                                    preference.setSummary(summary[valueId]);
            }

            // Restricted secondary users access
            else if (FileManagerSettings.SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS.getId().
                    compareTo(key) == 0) {
                String value = String.valueOf(newValue);
                if (Preferences.writeWorldReadableProperty(getActivity(), key, value)) {
                    ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
                    updateAccessModeStatus();
                    notify = true;
                }
                ret = false;
            }

            // Notify the change (only if fragment is loaded. Default values are loaded
            // while not in loaded mode)
            if (GeneralPreferenceFragment.this.mLoaded && (ret || notify)) {
                Intent intent = new Intent(FileManagerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(
                        FileManagerSettings.EXTRA_SETTING_CHANGED_KEY, preference.getKey());
                getActivity().sendBroadcast(intent);
            }

            return ret;
        }
    };

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
        addPreferencesFromResource(R.xml.preferences_general);

        // Case sensitive sort
        this.mCaseSensitiveSort =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_CASE_SENSITIVE_SORT.getId());
        this.mCaseSensitiveSort.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Filetime format mode
        this.mFiletimeFormatMode =
                (ListPreference)findPreference(
                        FileManagerSettings.SETTINGS_FILETIME_FORMAT_MODE.getId());
        String defaultValue = ((ObjectStringIdentifier)FileManagerSettings.
                SETTINGS_FILETIME_FORMAT_MODE.getDefaultValue()).getId();
        String value = Preferences.getSharedPreferences().getString(
                            FileManagerSettings.SETTINGS_FILETIME_FORMAT_MODE.getId(),
                            defaultValue);
        this.mOnChangeListener.onPreferenceChange(this.mFiletimeFormatMode, value);
        this.mFiletimeFormatMode.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Disk usage warning level
        this.mFreeDiskSpaceWarningLevel =
                (ListPreference)findPreference(
                        FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId());
        defaultValue = ((String)FileManagerSettings.
                            SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
        value = Preferences.getSharedPreferences().getString(
                            FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                            defaultValue);
        this.mOnChangeListener.onPreferenceChange(this.mFreeDiskSpaceWarningLevel, value);
        this.mFreeDiskSpaceWarningLevel.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Compute folder statistics
        this.mComputeFolderStatistics =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_COMPUTE_FOLDER_STATISTICS.getId());
        this.mComputeFolderStatistics.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Display thumbs
        this.mDisplayThumbs =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_DISPLAY_THUMBS.getId());
        this.mDisplayThumbs.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Use flinger
        this.mUseFlinger =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_USE_FLINGER.getId());
        this.mUseFlinger.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Access mode
        this.mAccessMode =
                (ListPreference)findPreference(
                        FileManagerSettings.SETTINGS_ACCESS_MODE.getId());
        this.mAccessMode.setOnPreferenceChangeListener(this.mOnChangeListener);
        defaultValue = ((ObjectStringIdentifier)FileManagerSettings.
                            SETTINGS_ACCESS_MODE.getDefaultValue()).getId();
        value = Preferences.getSharedPreferences().getString(
                            FileManagerSettings.SETTINGS_ACCESS_MODE.getId(),
                            defaultValue);
        this.mOnChangeListener.onPreferenceChange(this.mAccessMode, value);
        updateAccessModeStatus();

        // Capture Debug traces
        this.mRestrictSecondaryUsersAccess =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS.getId());
        if (!AndroidHelper.hasSupportForMultipleUsers(getActivity()) ||
                AndroidHelper.isSecondaryUser(getActivity())) {
            // Remove if device doesn't support multiple users accounts or the current user
            // is a secondary user
            PreferenceCategory category = (PreferenceCategory) findPreference(
                    "general_advanced_settings");
            category.removePreference(this.mRestrictSecondaryUsersAccess);
        } else {
            this.mRestrictSecondaryUsersAccess.setChecked(
                    FileManagerApplication.isRestrictSecondaryUsersAccess(getActivity()));
            this.mRestrictSecondaryUsersAccess.setOnPreferenceChangeListener(this.mOnChangeListener);
        }

        // Capture Debug traces
        this.mDebugTraces =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_SHOW_TRACES.getId());
        this.mDebugTraces.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Loaded
        this.mLoaded = true;
    }

    private void updateAccessModeStatus() {
        // If device is not rooted, or is a restricted user, this setting cannot be changed
        final Context context = getActivity();
        boolean restrictedAccess = AndroidHelper.isSecondaryUser(context) &&
                FileManagerApplication.isRestrictSecondaryUsersAccess(context);
        this.mAccessMode.setEnabled(FileManagerApplication.isDeviceRooted() && !restrictedAccess);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_general);
    }
}
