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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.SearchResult;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.widgets.RelevanceView;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.SearchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} for display search results.
 */
public class SearchResultAdapter extends ArrayAdapter<SearchResult> {

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
    }

    private DataHolder[] mData;
    private IconHolder mIconHolder;
    private final int mItemViewResourceId;

    private final boolean mHighlightTerms;
    private final boolean mShowRelevanceWidget;

    private final List<String> mQueries;

    private boolean mDisposed;

    //The resource of the item icon
    private static final int RESOURCE_ITEM_ICON = R.id.search_item_icon;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.search_item_name;
    //The resource of the item path
    private static final int RESOURCE_ITEM_PARENT_DIR = R.id.search_item_parent_dir;
    //The resource of the item relevance
    private static final int RESOURCE_ITEM_RELEVANCE = R.id.search_item_relevance;

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

        //Do cache of the data for better performance
        loadDefaultIcons();
        processData();
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
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        if (this.mIconHolder != null) {
            this.mIconHolder.cleanup();
        }
        this.mDisposed = true;
        clear();
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
                    (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(this.mItemViewResourceId, parent, false);
            ViewHolder viewHolder = new SearchResultAdapter.ViewHolder();
            viewHolder.mIvIcon = (ImageView)v.findViewById(RESOURCE_ITEM_ICON);
            viewHolder.mTvName = (TextView)v.findViewById(RESOURCE_ITEM_NAME);
            viewHolder.mTvParentDir = (TextView)v.findViewById(RESOURCE_ITEM_PARENT_DIR);
            viewHolder.mWgRelevance = (RelevanceView)v.findViewById(RESOURCE_ITEM_RELEVANCE);
            v.setTag(viewHolder);

            // Apply the current theme
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            theme.setBackgroundDrawable(
                    getContext(), v, "selectors_deselected_drawable"); //$NON-NLS-1$
            theme.setTextColor(
                    getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$
            if (viewHolder.mTvParentDir != null) {
                theme.setTextColor(
                        getContext(), viewHolder.mTvParentDir, "text_color"); //$NON-NLS-1$
            }
        }

        //Retrieve data holder
        final DataHolder dataHolder = this.mData[position];

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

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

        //Return the view
        return v;
    }

}
