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
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.List;

/**
 * A view that allow view all installed themes by scrolling horizontally.
 */
public class ThemeRoulette extends HorizontalScrollView {

    /**
     * An interface for raise theme selection events
     */
    public interface OnThemeScrollSelectionListener {
        /**
         * Method invoked when the scroll selection of a theme start
         */
        void onScrollSelectionStart();
        /**
         * Method invoked when a new scroll selection of a theme was made
         *
         * @param theme The new theme
         */
        void onScrollSelectionChanged(Theme theme);
    }

    /**
     * @hide
     */
    ViewGroup mRouletteLayout;
    private View mSpacer1;
    private View mSpacer2;

    /**
     * @hide
     */
    int mSpacerWidth;
    /**
     * @hide
     */
    int mThemeWidth;

    /**
     * @hide
     */
    int mCurrentThemePosition;

    /**
     * @hide
     */
    OnThemeScrollSelectionListener mOnThemeScrollSelectionListener;

    /**
     * Constructor of <code>ThemeRoulette</code>.
     *
     * @param context The current context
     */
    public ThemeRoulette(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>ThemeRoulette</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ThemeRoulette(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>ThemeRoulette</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public ThemeRoulette(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view
     */
    private void init() {
        this.mRouletteLayout = (ViewGroup)inflate(getContext(), R.layout.theme_roulette, null);
        this.mRouletteLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        this.mSpacer1 = this.mRouletteLayout.findViewById(R.id.spacer1);
        this.mSpacer2 = this.mRouletteLayout.findViewById(R.id.spacer2);
        addView(this.mRouletteLayout);

        this.mCurrentThemePosition = -1;

        setSmoothScrollingEnabled(false);
    }

    /**
     * Method that sets the listener where return theme scroll selection changes
     *
     * @param onThemeScrollSelectionListener The listener
     */
    public void setOnThemeScrollSelectionListener(
            OnThemeScrollSelectionListener onThemeScrollSelectionListener) {
        this.mOnThemeScrollSelectionListener = onThemeScrollSelectionListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.setMeasuredDimension(parentWidth, parentHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int mw = (int)getResources().getDimension(R.dimen.theme_max_width);
        int w =  (int)(getWidth() / 1.5);
        this.mSpacerWidth = (getWidth() - Math.min(w, mw)) / 2;
        this.mSpacer1.getLayoutParams().width = this.mSpacerWidth;
        this.mSpacer2.getLayoutParams().width = this.mSpacerWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        // Detect the motion
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            break;

        case MotionEvent.ACTION_MOVE:
            if (this.mOnThemeScrollSelectionListener != null) {
                this.mOnThemeScrollSelectionListener.onScrollSelectionStart();
            }
            break;

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            post(new Runnable() {
                @Override
                public void run() {
                    int themeViewPosition = getVisibleThemeViewPosition();
                    if (themeViewPosition >= 1) {
                        ThemeRoulette.this.mCurrentThemePosition = themeViewPosition;
                        scrollToThemePosition(themeViewPosition, true);
                    }
                }
            });
            break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Method that sets the themes of this roulette
     *
     * @param themes The themes of this roulette
     * @param currentTheme The current theme
     * @param scrollTo The theme to scroll to
     */
    public void setThemes(List<Theme> themes, Theme currentTheme, final int scrollTo) {
        int cc = themes.size();
        int pos = -1;
        for (int i = 0; i < cc; i++) {
            Theme theme = themes.get(i);
            addTheme(this.mRouletteLayout, theme);
            if (theme.compareTo(currentTheme) == 0) {
                pos = i + 1;
            }
        }

        // If the theme is found
        if (pos != -1) {
            final int childPos = pos;
            post(new Runnable() {
                @Override
                public void run() {
                    ThemeRoulette.this.mCurrentThemePosition =
                            (scrollTo > 0) ? scrollTo : childPos;
                    scrollToThemePosition(ThemeRoulette.this.mCurrentThemePosition, false);
                    markThemeAsDefault(childPos);
                }
            });
        }
    }

    /**
     * Method that adds a theme to the roulette
     *
     * @param root The root view
     * @param theme The theme to add
     */
    private void addTheme(ViewGroup root, Theme theme) {
        // Create the layout and assign its theme
        ThemeView v = (ThemeView)inflate(getContext(), R.layout.theme_view, null);
        int mw = (int)getResources().getDimension(R.dimen.theme_max_width);
        int w =  (int)(getWidth() / 1.5);
        this.mThemeWidth = Math.min(w, mw);
        v.setLayoutParams(
                new LinearLayout.LayoutParams(
                        this.mThemeWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        v.setTag(theme);

        // Set the view info
        TextView name = (TextView)v.findViewById(R.id.theme_name);
        name.setText(theme.getName());
        TextView author = (TextView)v.findViewById(R.id.theme_author);
        author.setText(theme.getAuthor());
        TextView desc = (TextView)v.findViewById(R.id.theme_desc);
        desc.setText(theme.getDescription());
        ImageView preview = (ImageView)v.findViewById(R.id.theme_preview);
        preview.setContentDescription(theme.getName());
        Drawable dw = theme.getPreviewImage(getContext());
        if (dw != null) {
            preview.setImageDrawable(dw);
        } else {
            dw = theme.getNoPreviewImage(getContext());
            preview.setImageDrawable(dw);
        }

        // Add to the end of the roulette
        root.addView(v, root.getChildCount()-1);
    }

    /**
     * Method that returns the visible theme view position, based on the position
     * on the screen (the most centered is the current view)
     *
     * @return int The visible theme view position
     * @hide
     */
    int getVisibleThemeViewPosition() {
        int[] rouletteSize = new int[2];
        this.getLocationOnScreen(rouletteSize);
        int x = rouletteSize[0] + (this.getWidth() / 2);
        int width = 0;

        int[] location = new int[2];
        int cc = this.mRouletteLayout.getChildCount()-1;
        for (int i = 1; i < cc; i++) {
            View v = this.mRouletteLayout.getChildAt(i);
            if (v instanceof ThemeView) {
                v.getLocationOnScreen(location);
                width = v.getWidth();
                if (location[0] <= x && x <= (location[0] + width)) {
                   return  i;
                }
            }
        }
        return -1;
    }

    /**
     * Method that scroll to the theme position
     *
     * @param position The position where to scroll
     * @param smooth Use a smooth scroll to position
     * @hide
     */
    void scrollToThemePosition(int position, boolean smooth) {
        if (position <= 0) return;
        int x = 0;
        if (position > 1) {
            x += ThemeRoulette.this.mThemeWidth * (position - 1);
        }
        if (smooth) {
            smoothScrollTo(x, 0);
        } else {
            scrollTo(x, 0);
        }

        // Notify the change
        if (ThemeRoulette.this.mOnThemeScrollSelectionListener != null) {
            ThemeView v =
                    (ThemeView)ThemeRoulette.this.mRouletteLayout.
                            getChildAt(position);
            ThemeRoulette.this.
                mOnThemeScrollSelectionListener.
                    onScrollSelectionChanged((Theme)v.getTag());
        }
    }

    /**
     * Method that mark the current visible theme as the default
     */
    void markCurrentVisibleThemeAsDefault() {
       int position = getVisibleThemeViewPosition();
       if (position >= 1) {
           markThemeAsDefault(position);
       }
    }

    /**
     * Method that mark the theme as the default
     *
     * @param position The position of the theme
     */
    void markThemeAsDefault(int position) {
        int cc = this.mRouletteLayout.getChildCount()-1;
        for (int i = 1; i < cc; i++) {
            ThemeView v =
                    (ThemeView)ThemeRoulette.this.mRouletteLayout.
                            getChildAt(i);
            if (i == position) {
                v.getName().setPaintFlags(
                        v.getName().getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            } else {
                v.getName().setPaintFlags(
                        v.getName().getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
            }
        }
    }
}
