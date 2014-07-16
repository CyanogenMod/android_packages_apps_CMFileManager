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

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.preferences.ThemeSelectorPreference;

/**
 * A class that manages the theme selection
 */
public class ThemesPreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "ThemesPreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private ThemeSelectorPreference mThemeSelector;

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
        addPreferencesFromResource(R.xml.preferences_themes);
        getActivity().getActionBar().setIcon(R.drawable.ic_launcher_themes);

        // Theme selector
        this.mThemeSelector =
                (ThemeSelectorPreference)findPreference(
                        FileManagerSettings.SETTINGS_THEME.getId());
        this.mThemeSelector.setOnPreferenceChangeListener(this.mOnChangeListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_themes);
    }
}
