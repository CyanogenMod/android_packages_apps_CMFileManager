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

package com.cyanogenmod.filemanager.ui.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

/**
 * A view that display information about a {@link Theme}
 */
public class ThemeView extends RelativeLayout {

    private TextView mName;
    private TextView mAuthor;
    private TextView mDescription;
    private ImageView mPreview;

    /**
     * Constructor of <code>ThemeView</code>.
     *
     * @param context The current context
     */
    public ThemeView(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>ThemeView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ThemeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor of <code>ThemeView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public ThemeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mName = (TextView)findViewById(R.id.theme_name);
        this.mAuthor = (TextView)findViewById(R.id.theme_author);
        this.mDescription = (TextView)findViewById(R.id.theme_desc);
        this.mPreview = (ImageView)findViewById(R.id.theme_preview);

        // Apply current theme
        Theme theme = ThemeManager.getCurrentTheme(getContext());
        theme.setTextColor(getContext(), this.mName, "text_color"); //$NON-NLS-1$
        theme.setTextColor(getContext(), this.mAuthor, "text_color"); //$NON-NLS-1$
        theme.setTextColor(getContext(), this.mDescription, "text_color"); //$NON-NLS-1$
    }

    /**
     * Method that return the view for the name of the theme
     *
     * @return TextView The view that holds the name of the theme
     */
    public TextView getName() {
        return this.mName;
    }

    /**
     * Method that return the view for the author of the theme
     *
     * @return TextView The view that holds the author of the theme
     */
    public TextView getAuthor() {
        return this.mAuthor;
    }

    /**
     * Method that return the view for the description of the theme
     *
     * @return TextView The view that holds the description of the theme
     */
    public TextView getDescription() {
        return this.mDescription;
    }

    /**
     * Method that return the view for the preview image of the theme
     *
     * @return TextView The view that holds the preview image of the theme
     */
    public ImageView getPreview() {
        return this.mPreview;
    }

}
