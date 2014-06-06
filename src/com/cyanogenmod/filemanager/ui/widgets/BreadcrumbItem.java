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

package com.cyanogenmod.filemanager.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.Toast;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.util.DialogHelper;

/**
 * A class that represents a item of a breadcrumb.
 */
public class BreadcrumbItem extends TextView implements OnLongClickListener {

    private String mItemPath;

    /**
     * Constructor of <code>BreadcrumbItem</code>.
     *
     * @param context The current context
     */
    public BreadcrumbItem(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>BreadcrumbItem</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public BreadcrumbItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>BreadcrumbItem</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public BreadcrumbItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the widget.
     */
    private void init() {
        //Register the long click listener
        setOnLongClickListener(this);

        //Set the default resource background effect
        setBackgroundResource(R.drawable.holo_selector);
    }

    /**
     * Method that returns the item path associated with with this breadcrumb item.
     *
     * @return String The item path associated
     */
    public String getItemPath() {
        return this.mItemPath;
    }

    /**
     * Method that sets the item path associated with with this breadcrumb item.
     *
     * @param itemPath The item path
     */
    protected void setItemPath(String itemPath) {
        this.mItemPath = itemPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(View v) {
        DialogHelper.showToast(getContext(), this.mItemPath, Toast.LENGTH_SHORT);
        return true;
    }

}
