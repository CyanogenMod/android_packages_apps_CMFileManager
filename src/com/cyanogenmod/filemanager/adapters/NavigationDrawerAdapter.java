/*
 * Copyright (C) 2015 The CyanogenMod Project
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
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cyanogenmod.filemanager.model.NavigationDrawerItem;
import com.cyanogenmod.filemanager.model.NavigationDrawerItem.NavigationDrawerItemType;
import com.cyanogenmod.filemanager.R;

import java.util.List;

public class NavigationDrawerAdapter extends BaseAdapter {
    private Context mContext;
    private List<NavigationDrawerItem> mEntries;

    private static final int RESOURCE_SINGLE_LINE_ITEM =
            R.layout.navigation_drawer_single_line_item;
    private static final int RESOURCE_DOUBLE_LINE_ITEM =
            R.layout.navigation_drawer_double_line_item;
    private static final int RESOURCE_DIVIDER_LINE_ITEM = R.layout.navigation_drawer_divider;
    private static final int RESOURCE_HEADER = R.layout.navigation_header;

    public NavigationDrawerAdapter(Context context, List<NavigationDrawerItem> objects) {
        mContext = context;
        mEntries = objects;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mEntries.indexOf(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int color = 0;
        ImageView itemIcon = null;
        TextView itemTitle = null;
        TextView itemSummary = null;
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        // Inflate the correct view for this item
        NavigationDrawerItem item = (NavigationDrawerItem) getItem(position);
        switch (item.getType()) {
            case SINGLE:
                convertView = layoutInflater.inflate(RESOURCE_SINGLE_LINE_ITEM, parent, false);
                itemIcon = (ImageView) convertView.findViewById(R.id.navigation_drawer_item_icon);
                itemTitle = (TextView) convertView.findViewById(R.id.navigation_drawer_item_title);
                break;
            case DOUBLE:
                convertView = layoutInflater.inflate(RESOURCE_DOUBLE_LINE_ITEM, parent, false);
                itemIcon = (ImageView) convertView.findViewById(R.id.navigation_drawer_item_icon);
                itemTitle = (TextView) convertView.findViewById(R.id.navigation_drawer_item_title);
                itemSummary =
                        (TextView) convertView.findViewById(R.id.navigation_drawer_item_summary);
                break;
            case DIVIDER:
                convertView = layoutInflater.inflate(RESOURCE_DIVIDER_LINE_ITEM, parent, false);
                break;
            case HEADER:
                convertView = layoutInflater.inflate(RESOURCE_HEADER, parent, false);
                break;
            default:
                break;
        }

        convertView.setId(item.getId());
        if (item.isSelected()) {
            color = mContext.getResources().getColor(R.color.navigation_drawer_selected);
            convertView.setSelected(true);
            convertView.setBackgroundColor(color);
        }
        if (itemIcon != null) {
            if (item.getIconId() != -1) {
                itemIcon.setImageResource(item.getIconId());
            } else if (item.getIconDrawable() != null) {
                itemIcon.setImageDrawable(item.getIconDrawable());
            }
            if (item.isSelected()) {
                color = item.getSelectedColor();
            } else {
                color = mContext.getResources().getColor(R.color.navigation_drawer_icon_default);
            }
            itemIcon.setColorFilter(color, Mode.MULTIPLY);
        }
        if (itemTitle != null) {
            itemTitle.setText(item.getTitle());
            if (item.isSelected()) {
                color = item.getSelectedColor();
                itemTitle.setTextColor(color);
            }
        }
        if (itemSummary != null) {
            itemSummary.setText(item.getSummary());
        }
        if (item.getType() == NavigationDrawerItemType.DIVIDER ||
                item.getType() == NavigationDrawerItemType.HEADER) {
            convertView.setEnabled(false);
            convertView.setOnClickListener(null);
        }

        return convertView;
    }
}
