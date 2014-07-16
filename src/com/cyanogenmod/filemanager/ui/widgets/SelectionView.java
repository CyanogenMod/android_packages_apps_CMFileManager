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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.TextView;
import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.FileHelper;
import java.util.List;

/**
 * A view that holds the selection of files
 */
public class SelectionView extends LinearLayout {

    /**
     * @hide
     */
    int mViewHeight;
    private TextView mStatus;
    private int mEffectDuration;

    /**
     * Constructor of <code>SelectionView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public SelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>SelectionView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public SelectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        //Add the view of the breadcrumb
        View content = inflate(getContext(), R.layout.navigation_view_selectionbar, null);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Obtain the height of the view for use in expand/collapse animation
        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @SuppressLint("NewApi") 
                    @Override
                    public void onGlobalLayout() {
                        SelectionView.this.mViewHeight = getHeight();
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        	getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }else{
                        	getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        setVisibility(View.GONE);
                        LayoutParams params = (LayoutParams)SelectionView.this.getLayoutParams();
                        params.height = 0;
                    }
            });

        //Recovery all views
        this.mStatus = (TextView)content.findViewById(R.id.navigation_status_selection_label);

        // Obtain the duration of the effect
        this.mEffectDuration =
                getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);

        addView(content);
    }

    /**
     * Method that computes the selection and returns a text message.
     *
     * @param selection The selection
     * @return String The computed text from the selection
     */
    private String computeSelection(List<FileSystemObject> selection) {
        int folders = 0;
        int files = 0;
        int cc = selection.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = selection.get(i);
            if (FileHelper.isDirectory(fso)) {
                folders++;
            } else {
                files++;
            }
        }

        // Get the string
        final Resources res = getContext().getResources();

        if (files == 0) {
            return res.getQuantityString(R.plurals.selection_folders, folders, folders);
        }

        if (folders == 0) {
            return res.getQuantityString(R.plurals.selection_files, files, files);
        }

        String nFoldersString = res.getQuantityString(R.plurals.n_folders, folders, folders);
        String nFilesString = res.getQuantityString(R.plurals.n_files, files, files);
        return res.getString(R.string.selection_folders_and_files, nFoldersString, nFilesString);
    }

    /**
     * Method that sets the {@link FileSystemObject} selection list
     *
     * @param newSelection The new selection list
     */
    public void setSelection(List<FileSystemObject> newSelection) {
        // Compute the selection
        if (newSelection != null && newSelection.size() > 0) {
            this.mStatus.setText(computeSelection(newSelection));
        }

        // Requires show the animation (expand or collapse)?
        // Is the current state need to be changed?
        if ((newSelection == null || newSelection.size() == 0) &&
                this.getVisibility() == View.GONE) {
            return;
        }
        if ((newSelection != null && newSelection.size() > 0) &&
                this.getVisibility() == View.VISIBLE) {
            return;
        }

        // Need some animation
        final ExpandCollapseAnimation.ANIMATION_TYPE effect =
                (newSelection != null && newSelection.size() > 0) ?
                        ExpandCollapseAnimation.ANIMATION_TYPE.EXPAND :
                        ExpandCollapseAnimation.ANIMATION_TYPE.COLLAPSE;
        ExpandCollapseAnimation animation =
                                    new ExpandCollapseAnimation(
                                            this,
                                            this.mViewHeight,
                                            this.mEffectDuration,
                                            effect);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation anim) {
                LayoutParams params = (LayoutParams)getLayoutParams();
                if (effect.compareTo(ExpandCollapseAnimation.ANIMATION_TYPE.EXPAND) == 0) {
                    params.height = 0;
                } else if (effect.compareTo(ExpandCollapseAnimation.ANIMATION_TYPE.COLLAPSE) == 0) {
                    params.height = SelectionView.this.mViewHeight;
                }
                SelectionView.this.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation anim) {/**NON BLOCK**/}

            @Override
            public void onAnimationEnd(Animation anim) {
                LayoutParams params = (LayoutParams)getLayoutParams();
                if (effect.compareTo(ExpandCollapseAnimation.ANIMATION_TYPE.COLLAPSE) == 0) {
                    params.height = 0;
                    requestLayout();
                    SelectionView.this.setVisibility(View.GONE);
                } else {
                    params.height = SelectionView.this.mViewHeight;
                    requestLayout();
                    SelectionView.this.setVisibility(View.VISIBLE);
                }
            }
        });
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        startAnimation(animation);
    }

    /**
     * An animation effect for expand or collapse the view
     *
     */
    private static class ExpandCollapseAnimation extends Animation {

        /**
         * The enumeration of the types of animation effects
         */
        public enum ANIMATION_TYPE {
            EXPAND,
            COLLAPSE
        }

        private final View mView;
        private final LayoutParams mViewLayoutParams;
        private final int mViewHeight;
        private final ANIMATION_TYPE mEffect;

        /**
         * Constructor of <code>ExpandCollapseAnimation</code>
         *
         * @param view The view to animate
         * @param viewHeight The maximum height of view. Used to compute the animation translation
         * @param duration The duration of the animation
         * @param effect The effect of the animation
         */
        public ExpandCollapseAnimation(
                View view, int viewHeight, int duration, ANIMATION_TYPE effect) {
            super();
            this.mView = view;
            this.mViewHeight = viewHeight;
            this.mEffect = effect;
            this.mViewLayoutParams = (LayoutParams) view.getLayoutParams();
            setDuration(duration);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if (interpolatedTime < 1.0f) {
                int height = (int)(this.mViewHeight * interpolatedTime);
                if (this.mEffect.compareTo(ANIMATION_TYPE.EXPAND) == 0) {
                    this.mViewLayoutParams.height = height;
                } else {
                    this.mViewLayoutParams.height = this.mViewHeight - height;
                }
                this.mView.setLayoutParams(this.mViewLayoutParams);
                this.mView.requestLayout();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

}
