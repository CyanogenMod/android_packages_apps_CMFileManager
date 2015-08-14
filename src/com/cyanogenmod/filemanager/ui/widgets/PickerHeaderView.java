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
package com.cyanogenmod.filemanager.ui.widgets;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.FileHelper;

public class PickerHeaderView extends LinearLayout implements OnClickListener {
    private ViewGroup mSceneRoot;
    private Scene mRootTitleScene;
    private View mRootTitleLayout;
    private Scene mBrowseTitleScene;
    private View mBrowseTitleLayout;
    private Scene mCurrentScene;

    private OnClickListener mOnClickListener;

    public PickerHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PickerHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        View content = inflate(getContext(), R.layout.picker_header_view, null);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mSceneRoot = (ViewGroup) content.findViewById(R.id.picker_header_title_scene_root);
        mRootTitleLayout = inflate(getContext(), R.layout.picker_header_root, null);
        mRootTitleScene = new Scene(mSceneRoot, mRootTitleLayout);
        mBrowseTitleLayout = inflate(getContext(), R.layout.picker_header_browse, null);
        mBrowseTitleScene = new Scene(mSceneRoot, mBrowseTitleLayout);
        ImageView imageView = (ImageView) mBrowseTitleLayout.findViewById(R.id.header_new_folder);
        imageView.setOnClickListener(this);

        mCurrentScene = mRootTitleScene;
        TransitionManager.go(mCurrentScene);

        addView(content);
    }

    public void setActionText(int stringId) {
        TextView textView = (TextView) mRootTitleLayout.findViewById(R.id.header_text_action);
        textView.setText(stringId);
        textView = (TextView) mBrowseTitleLayout.findViewById(R.id.header_text_action);
        textView.setText(stringId);
        TransitionManager.go(mCurrentScene);
    }

    public void setDirectory(String directory) {
        TextView textView = (TextView) mBrowseTitleLayout.findViewById(R.id.header_text_location);
        if (TextUtils.equals(directory, FileHelper.ROOTS_LIST)) {
            textView.setText(null);
            mCurrentScene = mRootTitleScene;
        } else {
            textView.setText(directory);
            mCurrentScene = mBrowseTitleScene;
        }
        TransitionManager.go(mCurrentScene);
    }

    public void setPrimaryColor(int color) {
        mBrowseTitleLayout.setBackgroundColor(color);
        TransitionManager.go(mCurrentScene);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v);
        }
    }
}