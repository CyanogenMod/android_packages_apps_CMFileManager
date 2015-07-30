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

package com.cyanogenmod.filemanager.model;

import android.graphics.drawable.Drawable;

public class NavigationDrawerItem {
    private int mId;
    private NavigationDrawerItemType mType;
    private String mTitle;
    private String mSummary;
    private Drawable mIconDrawable;
    private int mIconId;
    private int mSelectedColor;
    private boolean mSelected;

    /**
     * Fragment types
     */
    public enum NavigationDrawerItemType {
        // Single line
        SINGLE,

        // Double line
        DOUBLE,

        // Divider
        DIVIDER,

        // Header
        HEADER,
    }

    public NavigationDrawerItem(int id, NavigationDrawerItemType type, String title,
            String summary, int iconId, int selectedColor) {
        mId = id;
        mType = type;
        mTitle = title;
        mSummary = summary;
        mIconId = iconId;
        mIconDrawable = null;
        mSelectedColor = selectedColor;
        mSelected = false;
    }

    public NavigationDrawerItem(int id, NavigationDrawerItemType type, String title,
            String summary, Drawable iconDrawable, int selectedColor) {
        mId = id;
        mType = type;
        mTitle = title;
        mSummary = summary;
        mIconId = -1;
        mIconDrawable = iconDrawable;
        mSelectedColor = selectedColor;
        mSelected = false;
    }

    public void update(String title,
            String summary, Drawable iconDrawable, int selectedColor) {
        mTitle = title;
        mSummary = summary;
        mIconId = -1;
        mIconDrawable = iconDrawable;
        mSelectedColor = selectedColor;
    }

    public void update(String title,
            String summary, int iconId, int selectedColor) {
        mTitle = title;
        mSummary = summary;
        mIconId = iconId;
        mIconDrawable = null;
        mSelectedColor = selectedColor;
    }

    public int getId() {
        return mId;
    }

    public NavigationDrawerItemType getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSummary() {
        return mSummary;
    }

    public int getIconId() {
        return mIconId;
    }

    public Drawable getIconDrawable() {
        return mIconDrawable;
    }

    public int getSelectedColor() {
        return mSelectedColor;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NavigationDrawerItem)) {
            return false;
        }

        NavigationDrawerItem that = (NavigationDrawerItem) o;

        return mId == that.mId;

    }
}
