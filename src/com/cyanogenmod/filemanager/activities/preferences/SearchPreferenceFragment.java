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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.widget.Toast;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.ObjectStringIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.providers.RecentSearchesContentProvider;
import com.cyanogenmod.filemanager.util.DialogHelper;

/**
 * A class that manages the search options
 */
public class SearchPreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "SearchPreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    // Internal keys
    private static final String REMOVE_SEARCH_TERMS_KEY =
                                        "cm_filemanager_remove_saved_search_terms"; //$NON-NLS-1$

    private CheckBoxPreference mHighlightTerms;
    private CheckBoxPreference mShowRelevanceWidget;
    private ListPreference mSortSearchResultMode;
    private CheckBoxPreference mSaveSearchTerms;
    private Preference mRemoveSearchTerms;

    /**
     * @hide
     */
    boolean mLoaded = false;

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

            // Saved search terms
            if (preference.getKey().compareTo(
                    FileManagerSettings.SETTINGS_SAVE_SEARCH_TERMS.getId()) == 0) {
                if (!((Boolean)newValue).booleanValue()) {
                    // Remove search terms if saved search terms
                    // is not active by the user
                    clearRecentSearchTerms();
                }

            // Sort search result mode
            } else if (FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE.
                    getId().compareTo(key) == 0) {
                int value = Integer.valueOf((String)newValue).intValue();
                String[] summary = getResources().getStringArray(
                        R.array.sort_search_results_mode_labels);
                preference.setSummary(summary[value]);
            }

            // Notify the change (only if fragment is loaded. Default values are loaded
            // while not in loaded mode)
            if (SearchPreferenceFragment.this.mLoaded) {
                Intent intent = new Intent(FileManagerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(
                        FileManagerSettings.EXTRA_SETTING_CHANGED_KEY, preference.getKey());
                getActivity().sendBroadcast(intent);
            }

            return true;
        }
    };

    private final OnPreferenceClickListener mOnClickListener =
            new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().compareTo(REMOVE_SEARCH_TERMS_KEY) == 0) {
                // Remove search terms
                clearRecentSearchTerms();

                // Advise the user
                DialogHelper.showToast(
                        getActivity(),
                        getActivity().getString(R.string.pref_remove_saved_search_terms_msg),
                        Toast.LENGTH_SHORT);
            }
            return false;
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
        addPreferencesFromResource(R.xml.preferences_search);

        // Highlight terms
        this.mHighlightTerms =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_HIGHLIGHT_TERMS.getId());
        this.mHighlightTerms.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Relevance widget
        this.mShowRelevanceWidget =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_SHOW_RELEVANCE_WIDGET.getId());
        this.mShowRelevanceWidget.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Sort search result mode
        this.mSortSearchResultMode =
                (ListPreference)findPreference(
                        FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE.getId());
        this.mSortSearchResultMode.setOnPreferenceChangeListener(this.mOnChangeListener);
        String defaultValue = ((ObjectStringIdentifier)FileManagerSettings.
                                SETTINGS_SORT_SEARCH_RESULTS_MODE.getDefaultValue()).getId();
        String value = Preferences.getSharedPreferences().getString(
                                FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE.getId(),
                                defaultValue);
        this.mOnChangeListener.onPreferenceChange(this.mSortSearchResultMode, value);

        // Saved search terms
        this.mSaveSearchTerms =
                (CheckBoxPreference)findPreference(
                        FileManagerSettings.SETTINGS_SAVE_SEARCH_TERMS.getId());
        this.mSaveSearchTerms.setOnPreferenceChangeListener(this.mOnChangeListener);

        // Remove search terms
        this.mRemoveSearchTerms = findPreference(REMOVE_SEARCH_TERMS_KEY);
        this.mRemoveSearchTerms.setOnPreferenceClickListener(this.mOnClickListener);

        // Loaded
        this.mLoaded = true;
    }

    /**
     * Method that removes the recent suggestions on search activity
     * @hide
     */
    void clearRecentSearchTerms() {
        SearchRecentSuggestions suggestions =
                new SearchRecentSuggestions(getActivity(),
                        RecentSearchesContentProvider.AUTHORITY,
                        RecentSearchesContentProvider.MODE);
        suggestions.clearHistory();
        Preferences.setLastSearch(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_search);
    }
}
