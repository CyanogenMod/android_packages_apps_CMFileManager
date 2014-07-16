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

/**
 * A class that represents a button from an action bar that can't gain focus
 * without background.
 */
public class TransparentNonFocusableButtonItem extends NonFocusableButtonItem {

    /**
     * Constructor of <code>TransparentNonFocusableButtonItem</code>.
     *
     * @param context The current context
     */
    public TransparentNonFocusableButtonItem(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>TransparentNonFocusableButtonItem</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public TransparentNonFocusableButtonItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>TransparentNonFocusableButtonItem</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public TransparentNonFocusableButtonItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the widget.
     */
    private void init() {
        //Remove focus
        setBackgroundDrawable(null);
    }
}
