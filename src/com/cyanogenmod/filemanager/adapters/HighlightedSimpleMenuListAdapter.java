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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

/**
 * An implementation of {@link SimpleMenuListAdapter} with highlighted text
 */
public class HighlightedSimpleMenuListAdapter extends SimpleMenuListAdapter {
    private final Context mContext;

    /**
     * Constructor of <code>HighlightedSimpleMenuListAdapter</code>.
     *
     * @param context The current context
     * @param menuResourceId The resource identifier
     */
    public HighlightedSimpleMenuListAdapter(Context context, int menuResourceId) {
        super(context, menuResourceId);
        this.mContext = context;
    }

    /**
     * Constructor of <code>SimpleMenuListAdapter</code><br/>
     * <br/>.
     * This constructors uses only the menus of the group passed
     *
     * @param context The current context
     * @param menuResourceId The resource identifier
     * @param menuGroupResourceId The menu group resource identifier
     */
    public HighlightedSimpleMenuListAdapter(
            Context context, int menuResourceId, int menuGroupResourceId) {
        this(context, menuResourceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Highlight the text
        View v = super.getView(position, convertView, parent);
        if (v != null) {
            Theme theme = ThemeManager.getCurrentTheme(this.mContext);
            TextView tvText = (TextView)v.findViewById(R.id.menu_item_text);
            if (tvText != null) {
                tvText.setTextAppearance(this.mContext, R.style.primary_text_appearance);
                theme.setTextColor(this.mContext, tvText, "text_color"); //$NON-NLS-1$
            }
        }
        //Return the view
        return v;
    }

}
