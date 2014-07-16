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
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;

/**
 * A class that manages the editor options
 */
public class EditorPreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "EditorPreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private CheckBoxPreference mNoSuggestions;
    private CheckBoxPreference mWordWrap;
    private CheckBoxPreference mHexdump;

    private CheckBoxPreference mSyntaxHighlight;


    /**
     * @hide
     */
    boolean mLoaded = false;

    private final OnPreferenceChangeListener mOnChangeListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            boolean ret = true;

            String key = preference.getKey();
            if (DEBUG) {
                Log.d(TAG,
                    String.format("New value for %s: %s",  //$NON-NLS-1$
                            key,
                            String.valueOf(newValue)));
            }

            // Notify the change (only if fragment is loaded. Default values are loaded
            // while not in loaded mode)
            if (EditorPreferenceFragment.this.mLoaded && ret) {
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
        this.mLoaded = false;

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_editor);

        // No suggestions
        this.mNoSuggestions =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.getId());
        this.mNoSuggestions.setOnPreferenceChangeListener(this.mOnChangeListener);

        // WordWrap
        this.mWordWrap =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.getId());
        this.mWordWrap.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Hexdump
        this.mHexdump =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_EDITOR_HEXDUMP.getId());
        this.mHexdump.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Syntax highlight
        this.mSyntaxHighlight =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.getId());
        this.mSyntaxHighlight.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Loaded
        this.mLoaded = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_editor);
    }
}
