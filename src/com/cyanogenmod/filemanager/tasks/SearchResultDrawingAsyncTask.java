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

package com.cyanogenmod.filemanager.tasks;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.cyanogenmod.filemanager.FileManagerApplication;
import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.adapters.SearchResultAdapter;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.SearchResult;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.DisplayRestrictions;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationSortMode;
import com.cyanogenmod.filemanager.preferences.ObjectStringIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.preferences.SearchSortResultMode;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.SearchHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for paint the resulting file system object of a search.
 */
public class SearchResultDrawingAsyncTask extends AsyncTask<Object, Integer, Boolean> {

    /**
     * @hide
     */
    final ListView mSearchListView;
    /**
     * @hide
     */
    final ProgressBar mSearchWaiting;
    private final List<FileSystemObject> mFiles;
    /**
     * @hide
     */
    final Query mQueries;
    private boolean mRunning;

    /**
     * Constructor of <code>SearchResultDrawingAsyncTask</code>.
     *
     * @param searchListView The {@link ListView} reference
     * @param searchWaiting A {@link ProgressBar} reference
     * @param files The files to draw
     * @param queries The terms of the search
     */
    public SearchResultDrawingAsyncTask(
            ListView searchListView, ProgressBar searchWaiting,
            List<FileSystemObject> files, Query queries) {
        super();
        this.mSearchListView = searchListView;
        this.mSearchWaiting = searchWaiting;
        this.mFiles = files;
        this.mQueries = queries;
        this.mRunning = false;
    }

    /**
     * Method that returns if there is a task running.
     *
     * @return boolean If there is a task running
     */
    public boolean isRunning() {
        return this.mRunning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean doInBackground(Object... params) {
        try {
            //Running
            this.mRunning = true;
            showHideWaiting(true);

            // Get sort mode
            String defaultValue = ((ObjectStringIdentifier)FileManagerSettings.
                    SETTINGS_SORT_SEARCH_RESULTS_MODE.getDefaultValue()).getId();
            String value = Preferences.getSharedPreferences().getString(
                                FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE.getId(),
                                defaultValue);
            SearchSortResultMode mode = SearchSortResultMode.fromId(value);

            // Are we in ChRooted environment?
            boolean chRooted =
                    FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;

            // Create display restrictions
            Map<DisplayRestrictions, Object> restrictions =
                    new HashMap<DisplayRestrictions, Object>();
            restrictions.put(
                    DisplayRestrictions.MIME_TYPE_RESTRICTION, MimeTypeHelper.ALL_MIME_TYPES);

            //Process all the data
            final List<SearchResult> result =
                    SearchHelper.convertToResults(
                            FileHelper.applyUserPreferences(
                                    this.mFiles, restrictions, true, chRooted),
                            this.mQueries);
            if (mode.compareTo(SearchSortResultMode.NAME) == 0) {
                Collections.sort(result, new Comparator<SearchResult>() {
                    @Override
                    public int compare(SearchResult lhs, SearchResult rhs) {
                        return FileHelper.doCompare(
                                lhs.getFso(), rhs.getFso(), NavigationSortMode.NAME_ASC);
                    }
                });
            } else if (mode.compareTo(SearchSortResultMode.RELEVANCE) == 0) {
                Collections.sort(result);
            }

            this.mSearchListView.post(new Runnable() {
                @Override
                public void run() {
                  //Add list to the listview
                    if (SearchResultDrawingAsyncTask.this.mSearchListView.getAdapter() != null) {
                        ((SearchResultAdapter)SearchResultDrawingAsyncTask.this.
                                mSearchListView.getAdapter()).clear();
                    }

                    // Recreate the adapter
                    SearchResultAdapter adapter = new SearchResultAdapter(
                            SearchResultDrawingAsyncTask.this.mSearchListView.getContext(),
                            result,
                            R.layout.search_item,
                            SearchResultDrawingAsyncTask.this.mQueries);

                    // Configure the listview
                    SearchResultDrawingAsyncTask.this.mSearchListView.setAdapter(adapter);
                    SearchResultDrawingAsyncTask.this.mSearchListView.setSelection(0);
                }
            });

            //Operation complete
            return Boolean.TRUE;

        } catch (Throwable ex) {
            //Capture and show the exception
            ExceptionUtil.translateException(this.mSearchListView.getContext(), ex);

        } finally {
            this.mRunning = false;
            showHideWaiting(false);
        }

        //Something went wrong
        return Boolean.FALSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled(Boolean result) {
        this.mRunning = false;
        super.onCancelled(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled() {
        this.mRunning = false;
        super.onCancelled();
    }

    /**
     * Method that shows or hides the waiting icon.
     *
     * @param show If the waiting icon must be shown
     */
    private void showHideWaiting(final boolean show) {
        if (this.mSearchWaiting != null) {
            this.mSearchWaiting.post(new Runnable() {
                @Override
                public void run() {
                    SearchResultDrawingAsyncTask.this.mSearchWaiting.setVisibility(
                            show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

}
