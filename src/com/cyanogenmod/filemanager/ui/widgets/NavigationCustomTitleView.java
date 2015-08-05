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
import android.widget.ViewFlipper;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.listeners.OnHistoryListener;

/**
 * A {@link ViewFlipper} implementation for the navigation custom title.
 */
public class NavigationCustomTitleView extends ViewFlipper {

    private OnHistoryListener mOnHistoryListener;

    /**
     * Constructor of <code>NavigationCustomTitleView</code>.
     *
     * @param context The current context
     */
    public NavigationCustomTitleView(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>NavigationCustomTitleView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public NavigationCustomTitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Method that sets the listener for communicate history changes.
     *
     * @param onHistoryListener The listener for communicate history changes
     */
    public void setOnHistoryListener(OnHistoryListener onHistoryListener) {
        this.mOnHistoryListener = onHistoryListener;
    }

    /**
     * Method that resets the view flipper positioning the current view
     * in the breadcrumb view.
     */
    public void restoreView() {
        hideConfigurationView();
    }

    /**
     * Method that shows the configuration view.
     */
    public void showConfigurationView() {
        //Transition
        setInAnimation(getContext(), R.anim.translate_to_left_in);
        setOutAnimation(getContext(), R.anim.translate_to_left_out);
        showNext();
    }

    /**
     * Method that hides the configuration view.
     */
    public void hideConfigurationView() {
        //Check history
        if (this.mOnHistoryListener != null) {
            this.mOnHistoryListener.onCheckHistory();
        }

        //Transition
        setInAnimation(getContext(), R.anim.translate_to_right_in);
        setOutAnimation(getContext(), R.anim.translate_to_right_out);
        showPrevious();
    }

    /**
     * Method that returns if the breadcrumb view is visible.
     *
     * @return boolean If the breadcrumb view is visible
     */
    public boolean isConfigurationViewShowing() {
        return Boolean.TRUE;
    }

}
