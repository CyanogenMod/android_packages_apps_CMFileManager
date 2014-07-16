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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} for display a list with an optional check.
 */
public class CheckableListAdapter extends ArrayAdapter<CheckableListAdapter.CheckableItem> {

    /**
     * A class that wrap an item with checkable options.
     */
    public static class CheckableItem {
        /**
         * Constructor of <code>CheckableItem</code>.
         *
         * @param label The text of the item
         * @param checkable If the item has a check
         * @param checked If the item is checked
         */
        public CheckableItem(String label, boolean checkable, boolean checked) {
            super();
            this.mLabel = label;
            this.mCheckable = checkable;
            this.mChecked = checked;
        }
        String mLabel;
        boolean mCheckable;
        boolean mChecked;
    }

    /**
     * A class that conforms with the ViewHolder pattern to performance.
     * the list view rendering
     */
    private static class ViewHolder {
        /**
         * @hide
         */
        public ViewHolder() {
            super();
        }
        ImageView mDwCheck;
        TextView mTvTitle;
    }

    private final boolean mIsDialog;

    //The resource of the item check
    private static final int RESOURCE_ITEM_CHECK = R.id.option_list_item_check;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.option_list_item_text;

    /**
     * Constructor of <code>CheckableListAdapter</code>.
     *
     * @param context The current context
     * @param items An array of items to add to the current list
     */
    public CheckableListAdapter(
            Context context, List<CheckableListAdapter.CheckableItem> items) {
        this(context, items, false);
    }

    /**
     * Constructor of <code>CheckableListAdapter</code>.
     *
     * @param context The current context
     * @param items An array of items to add to the current list
     * @param isDialog Indicates if the owner is a dialog (not a popup). In this case,
     * use the background of the dialog.
     */
    public CheckableListAdapter(
            Context context, List<CheckableListAdapter.CheckableItem> items, boolean isDialog) {
        super(context, RESOURCE_ITEM_NAME, items);
        this.mIsDialog = isDialog;
    }

    /**
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        clear();
    }

    /**
     * Method that returns the identifier of the setting.
     *
     * @param position The position of the item
     * @return int The identifier of the setting
     */
    @SuppressWarnings("static-method")
    public int getId(int position) {
        return position;
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
            v = li.inflate(R.layout.option_list_item, parent, false);
            ViewHolder viewHolder = new CheckableListAdapter.ViewHolder();
            viewHolder.mTvTitle = (TextView)v.findViewById(RESOURCE_ITEM_NAME);
            viewHolder.mDwCheck = (ImageView)v.findViewById(RESOURCE_ITEM_CHECK);
            v.setTag(viewHolder);

            // Apply theme
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            theme.setBackgroundDrawable(
                    getContext(), v,
                    (this.mIsDialog) ?
                            "selectors_deselected_drawable" : //$NON-NLS-1$
                            "menu_checkable_selector_drawable"); //$NON-NLS-1$
            theme.setTextColor(
                    getContext(), viewHolder.mTvTitle, "text_color"); //$NON-NLS-1$
            theme.setImageDrawable(
                    getContext(), viewHolder.mDwCheck,
                    "popup_checkable_selector_drawable"); //$NON-NLS-1$
        }

        //Retrieve the item
        CheckableListAdapter.CheckableItem item = getItem(position);

        //Retrieve the view holder and fill the views
        ViewHolder viewHolder = (ViewHolder)v.getTag();
        viewHolder.mTvTitle.setText(item.mLabel);
        viewHolder.mDwCheck.setVisibility(item.mCheckable ? View.VISIBLE : View.GONE);
        if (item.mCheckable) {
            viewHolder.mDwCheck.setSelected(item.mChecked);
        }

        return v;
    }

    /**
     * Method that sets the selected item
     *
     * @param position The position of the selected item
     */
    public void setSelectedItem(int position) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            getItem(i).mChecked = (i == position);
        }
        notifyDataSetChanged();
    }

}
