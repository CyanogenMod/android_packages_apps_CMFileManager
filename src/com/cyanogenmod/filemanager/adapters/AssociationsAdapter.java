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
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} for display associations.
 */
public class AssociationsAdapter
    extends ArrayAdapter<ResolveInfo> implements View.OnClickListener {

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
        int mPosition;
        ImageView mIvIcon;
        TextView mTvName;
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
        String mName;
    }



    private DataHolder[] mData;
    private AdapterView<?> mParent;
    private final OnItemClickListener mOnItemClickListener;

    //The resource item layout
    private static final int RESOURCE_LAYOUT = R.layout.associations_item;

    //The resource of the item icon
    private static final int RESOURCE_ITEM_ICON = R.id.associations_item_icon;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.associations_item_text;

    /**
     * Constructor of <code>AssociationsAdapter</code>.
     *
     * @param context The current context
     * @param parent The adapter view
     * @param intents The intents info
     * @param onItemClickListener The listener for listen action clicks
     */
    public AssociationsAdapter(
            Context context, AdapterView<?> parent,
            List<ResolveInfo> intents, OnItemClickListener onItemClickListener) {
        super(context, RESOURCE_ITEM_NAME, intents);
        this.mOnItemClickListener = onItemClickListener;
        this.mParent = parent;

        //Do cache of the data for better performance
        processData(intents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        processData(null);
        super.notifyDataSetChanged();
    }

    /**
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        clear();
        this.mData = null;
    }

    /**
     * Method that process the data before use {@link #getView} method.
     *
     * @param intents The list of intents (to better performance) or null.
     */
    private void processData(List<ResolveInfo> intents) {
        this.mData = new DataHolder[getCount()];
        int cc = (intents == null) ? getCount() : intents.size();
        for (int i = 0; i < cc; i++) {
            //Intent info
            ResolveInfo intentInfo = (intents == null) ? getItem(i) : intents.get(i);

            //Build the data holder
            this.mData[i] = new AssociationsAdapter.DataHolder();
            this.mData[i].mDwIcon = intentInfo.loadIcon(getContext().getPackageManager());
            this.mData[i].mName =
                    intentInfo.loadLabel(getContext().getPackageManager()).toString();
        }
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
            v = li.inflate(RESOURCE_LAYOUT, parent, false);
            ViewHolder viewHolder = new AssociationsAdapter.ViewHolder();
            viewHolder.mPosition = position;
            viewHolder.mIvIcon = (ImageView)v.findViewById(RESOURCE_ITEM_ICON);
            viewHolder.mTvName = (TextView)v.findViewById(RESOURCE_ITEM_NAME);
            v.setTag(viewHolder);

            // Apply theme
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            theme.setBackgroundDrawable(getContext(), v, "selection_drawable"); //$NON-NLS-1$
            theme.setTextColor(getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$
        }

        //Retrieve data holder
        final DataHolder dataHolder = this.mData[position];

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

        //Set the data
        viewHolder.mPosition = position;
        viewHolder.mIvIcon.setImageDrawable(dataHolder.mDwIcon);
        viewHolder.mTvName.setText(dataHolder.mName);
        v.setOnClickListener(this);

        //Return the view
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        ViewHolder viewHolder = (ViewHolder)v.getTag();
        this.mOnItemClickListener.onItemClick(this.mParent, v, viewHolder.mPosition, v.getId());
    }

}
