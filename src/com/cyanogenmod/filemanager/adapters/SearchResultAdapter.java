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

package com.cyanogenmod.filemanager.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
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
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.widgets.RelevanceView;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.SearchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link ArrayAdapter} for display search results.
 */
public class SearchResultAdapter extends ArrayAdapter<SearchResult> {

    //The resource of the item icon
    private static final int RESOURCE_ITEM_ICON = R.id.search_item_icon;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.search_item_name;
    //The resource of the item path
    private static final int RESOURCE_ITEM_PARENT_DIR = R.id.search_item_parent_dir;
    //The resource of the item relevance
    private static final int RESOURCE_ITEM_RELEVANCE = R.id.search_item_relevance;
    //The resource of the item mime type
    private static final int RESOURCE_ITEM_MIME_TYPE = R.id.search_item_mime_type;

    /**
     * A class that conforms with the ViewHolder pattern to performance
     * the list view rendering.
     */
    private static class ViewHolder {
        /**
         * @hide
         */
        public ViewHolder() {
            super();
        }
        ImageView mIvIcon;
        TextView mTvName;
        TextView mTvParentDir;
        RelevanceView mWgRelevance;
        TextView mMimeType;
    }

    /**
     * A class that holds the full data information.
     */
    private static class DataHolder {
        /**
         * @hide
         */
        public DataHolder() {
            super();
        }
        Drawable mDwIcon;
        CharSequence mName;
        String mParentDir;
        Float mRelevance;
        MimeTypeHelper.MimeTypeCategory mimeTypeCategory;
    }

    // delay for when the new items, if any, will be incorporated
    // used to ensure that UI remains responsive
    private final int STREAMING_MODE_REFRESH_DELAY = 500;   // in ms

    private DataHolder[] mData;
    private IconHolder mIconHolder;
    private final int mItemViewResourceId;

    private final boolean mHighlightTerms;
    private final boolean mShowRelevanceWidget;

    private final List<String> mQueries;
    private final List<SearchResult> mOriginalList;

    private boolean mDisposed;

    private Handler mHandler;
    private boolean mInStreamingMode;
    private List<SearchResult> mNewItems = new ArrayList<SearchResult>();
    private SearchSortResultMode mSearchSortResultMode;
    private Comparator<SearchResult> mSearchResultComparator;

    private Runnable mParseNewResults = new Runnable() {
        @Override
        public void run() {
            addPendingSearchResults();
            if (mInStreamingMode) {
                mHandler.postDelayed(mParseNewResults, STREAMING_MODE_REFRESH_DELAY);
            }
        }
    };

    /**
     * Constructor of <code>SearchResultAdapter</code>.
     *
     * @param context The current context
     * @param files The list of file system objects
     * @param itemViewResourceId The identifier of the layout that represents an item
     * of the list adapter
     * @param queries The query object used to make the result of this search
     */
    public SearchResultAdapter(
            Context context, List<SearchResult> files, int itemViewResourceId, Query queries) {
        super(context, RESOURCE_ITEM_NAME, files);
        mHandler = new Handler(context.getMainLooper());
        mOriginalList = new ArrayList<SearchResult>(files);

        this.mDisposed = false;
        final boolean displayThumbs = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_DISPLAY_THUMBS.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_DISPLAY_THUMBS.getDefaultValue()).booleanValue());
        this.mIconHolder = new IconHolder(context, displayThumbs);
        this.mItemViewResourceId = itemViewResourceId;
        this.mQueries = queries.getQueries();

        // Load settings
        this.mHighlightTerms = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_HIGHLIGHT_TERMS.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_HIGHLIGHT_TERMS.
                        getDefaultValue()).booleanValue());
        this.mShowRelevanceWidget = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_SHOW_RELEVANCE_WIDGET.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_SHOW_RELEVANCE_WIDGET.
                        getDefaultValue()).booleanValue());

        // determine the sort order of search results
        setSortResultMode();

        //Do cache of the data for better performance
        loadDefaultIcons();
        processData();
    }

    /**
     * A heads-up to the adapter to indicate that new items might be added
     * Allows the adapter to setup a buffer to take in the new results and incorporate the new
     * items periodically
     */
    public void startStreaming() {
        mInStreamingMode = true;
        mHandler.postDelayed(mParseNewResults, STREAMING_MODE_REFRESH_DELAY);
    }

    /**
     * Called to indicate that the search has completed and new results won't be streamed to the
     * adapter
     */
    public void stopStreaming() {
        mInStreamingMode = false;
    }

    /**
     * Method that loads the default icons (known icons and more common icons).
     */
    private void loadDefaultIcons() {
        this.mIconHolder.getDrawable("ic_fso_folder_drawable"); //$NON-NLS-1$
        this.mIconHolder.getDrawable("ic_fso_default_drawable"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        if (this.mDisposed) {
            return;
        }
        processData();
        super.notifyDataSetChanged();
    }

    /**
     * Adds a new Search Result to the buffer
     */
    public synchronized void addNewItem(SearchResult newResult) {
        mNewItems.add(newResult);
    }

    /**
     * Adds search results in the buffer to the adapter list
     */
    public synchronized void addPendingSearchResults() {
        if (mNewItems.size() < 1) return;

        // TODO: maintain a sorted buffer and implement Merge of two sorted lists
        addAll(mNewItems);
        sort(mSearchResultComparator);
        mOriginalList.addAll(mNewItems);    // cache files so enable mime type filtering later on
        Collections.sort(mOriginalList, mSearchResultComparator);

        // reset buffer
        mNewItems.clear();
    }

    /**
     * Determine the sort order for Search Results
     */
    public void setSortResultMode() {
        String defaultValue = ((ObjectStringIdentifier)FileManagerSettings.
                SETTINGS_SORT_SEARCH_RESULTS_MODE.getDefaultValue()).getId();
        String currValue = Preferences.getSharedPreferences().getString(
                FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE.getId(),
                defaultValue);
        mSearchSortResultMode = SearchSortResultMode.fromId(currValue);

        if (mSearchSortResultMode.compareTo(SearchSortResultMode.NAME) == 0) {
            mSearchResultComparator = new Comparator<SearchResult>() {
                @Override
                public int compare(SearchResult lhs, SearchResult rhs) {
                    return FileHelper.doCompare(
                            lhs.getFso(), rhs.getFso(), NavigationSortMode.NAME_ASC);
                }
            };

        } else if (mSearchSortResultMode.compareTo(SearchSortResultMode.RELEVANCE) == 0) {
            mSearchResultComparator = new Comparator<SearchResult>() {
                @Override
                public int compare(SearchResult lhs, SearchResult rhs) {
                    return lhs.compareTo(rhs);
                }
            };
        }
    }

    /**
     * Size of the search results list
     */
    public synchronized int resultsSize() {
        return getCount() + mNewItems.size();
    }

    /**
     * Method that allows filtering the results by {@link MimeTypeHelper.MimeTypeCategory}
     * @param mimeFilter the MimeTypeCategory to filter by
     */
    public void setMimeFilter(String mimeFilter) {
        // Are we in ChRooted environment?
        boolean chRooted =
                FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;

        // Create display restrictions
        Map<DisplayRestrictions, Object> restrictions =
                new HashMap<DisplayRestrictions, Object>();
        restrictions.put(
                DisplayRestrictions.MIME_TYPE_RESTRICTION, MimeTypeHelper.ALL_MIME_TYPES);

        List<SearchResult> newResults = SearchHelper.convertToResults(
                FileHelper.applyUserPreferences(
                        getFiles(), restrictions, true, chRooted), new Query().fillSlots(mQueries));

        clear();
        for (SearchResult result : newResults) {
            // Only show results that are within our category, or all if no filter is set
            if (TextUtils.equals(mimeFilter, MimeTypeHelper.MimeTypeCategory.NONE.name()) ||
                    MimeTypeHelper.getCategory(getContext(), result.getFso()) ==
                            MimeTypeHelper.MimeTypeCategory.valueOf(mimeFilter)) {
                add(result);
            }
        }

        this.notifyDataSetChanged();
    }

    /**
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        if (this.mIconHolder != null) {
            this.mIconHolder.cleanup();
        }
        this.mDisposed = true;
        clear();
        this.mOriginalList.clear();
        this.mData = null;
        this.mIconHolder = null;
    }

    /**
     * Method that process the data before use {@link #getView} method.
     */
    private void processData() {
        Theme theme = ThemeManager.getCurrentTheme(getContext());
        int highlightedColor =
                theme.getColor(getContext(), "search_highlight_color"); //$NON-NLS-1$

        this.mData = new DataHolder[getCount()];
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            //File system object info
            SearchResult result = getItem(i);

            //Build the data holder
            final FileSystemObject fso = result.getFso();
            this.mData[i] = new SearchResultAdapter.DataHolder();
            this.mData[i].mDwIcon = this.mIconHolder.getDrawable(
                    MimeTypeHelper.getIcon(getContext(), fso));
            if (this.mHighlightTerms) {
                this.mData[i].mName =
                        SearchHelper.getHighlightedName(result, this.mQueries, highlightedColor);
            } else {
                this.mData[i].mName = SearchHelper.getNonHighlightedName(result);
            }
            this.mData[i].mParentDir = new File(result.getFso().getFullPath()).getParent();
            if (this.mShowRelevanceWidget) {
                this.mData[i].mRelevance =
                        Float.valueOf(
                                (float)(result.getRelevance() * 100) / SearchResult.MAX_RELEVANCE);
            } else {
                this.mData[i].mRelevance = null;
            }
            this.mData[i].mimeTypeCategory = MimeTypeHelper.getCategory(getContext(), fso);
        }
    }

    /**
     * Method that returns the data of the adapter.
     *
     * @return List<SearchResult> The adapter data
     */
    public List<SearchResult> getData() {
        int cc = getCount();
        final List<SearchResult> data = new ArrayList<SearchResult>(cc);
        for (int i = 0; i < cc; i++) {
            data.add(getItem(i));
        }
        return data;
    }

    /**
     * Method that returns the files of the adapter.
     *
     * @return List<FileSystemObject> The adapter data
     */
    public List<FileSystemObject> getFiles()  {
        final List<FileSystemObject> data = new ArrayList<FileSystemObject>();
        for (SearchResult result : mOriginalList) {
            data.add(result.getFso());
        }
        return data;
    }

    /**
     * Returns the position of the specified item in the array.
     *
     * @param item The item to retrieve the position of.
     * @return The position of the specified item.
     */
    public int getPosition(FileSystemObject item) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            SearchResult sr = getItem(i);
            if (sr.getFso().compareTo(item) == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Check to reuse view
        View v = convertView;

        if (v == null) {
            //Create the view holder
            LayoutInflater li =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(this.mItemViewResourceId, parent, false);
            ViewHolder viewHolder = new SearchResultAdapter.ViewHolder();
            viewHolder.mIvIcon = (ImageView) v.findViewById(RESOURCE_ITEM_ICON);
            viewHolder.mTvName = (TextView) v.findViewById(RESOURCE_ITEM_NAME);
            viewHolder.mTvParentDir = (TextView) v.findViewById(RESOURCE_ITEM_PARENT_DIR);
            viewHolder.mWgRelevance = (RelevanceView) v.findViewById(RESOURCE_ITEM_RELEVANCE);
            viewHolder.mMimeType = (TextView) v.findViewById(RESOURCE_ITEM_MIME_TYPE);

            // Apply the current theme
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            theme.setTextColor(
                    getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$
            if (viewHolder.mTvParentDir != null) {
                theme.setTextColor(
                        getContext(), viewHolder.mTvParentDir, "text_color"); //$NON-NLS-1$
            }
            v.setTag(viewHolder);
        }

        //Retrieve data holder
        final DataHolder dataHolder = this.mData[position];

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        //Set the data
        if (convertView != null) {
            mIconHolder.cancelLoad(viewHolder.mIvIcon);
        }
        mIconHolder.loadDrawable(viewHolder.mIvIcon,
                getItem(position).getFso(), dataHolder.mDwIcon);

        viewHolder.mTvName.setText(dataHolder.mName, TextView.BufferType.SPANNABLE);
        viewHolder.mTvParentDir.setText(dataHolder.mParentDir);
        if (dataHolder.mRelevance != null) {
            viewHolder.mWgRelevance.setRelevance(dataHolder.mRelevance.floatValue());
        }
        viewHolder.mWgRelevance.setVisibility(
                dataHolder.mRelevance != null ? View.VISIBLE : View.GONE);
        if (dataHolder.mimeTypeCategory != MimeTypeHelper.MimeTypeCategory.NONE) {
            viewHolder.mMimeType.setVisibility(View.VISIBLE);
            viewHolder.mMimeType.setText(dataHolder.mimeTypeCategory.name());
        } else {
            viewHolder.mMimeType.setVisibility(View.GONE);
        }
        //Return the view
        return v;
    }
}
