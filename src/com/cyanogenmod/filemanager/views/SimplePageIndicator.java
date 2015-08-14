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

package com.cyanogenmod.filemanager.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import com.cyanogenmod.filemanager.R;

/**
 * Simple page indicator designed to work with a view pager
 */
public class SimplePageIndicator extends FrameLayout {

    Context mContext;

    float mSelectedAlpha;
    float mUnselectedAlpha;
    int mResID;
    boolean mScaleable;
    int mPageCount;

    float mSelectedDimen;
    float mUnselectedDimen;

    int mIndicatorMargin;

    LinearLayout mIndicatorContainer;
    List<ImageView> mCircles = new ArrayList<>();

    public SimplePageIndicator(Context context, AttributeSet attrs){
        super(context, attrs);

        mContext = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SimplePageIndicator,
                0,0);

        try {
            mSelectedAlpha = a.getFloat(R.styleable.SimplePageIndicator_alphaSelected, 1.0f);
            mUnselectedAlpha = a.getFloat(R.styleable.SimplePageIndicator_alphaUnSelected, 0.4f);
            mResID = a.getResourceId(R.styleable.SimplePageIndicator_indicatorDrawable,
                    R.drawable.page_indicator);
            mScaleable = a.getBoolean(R.styleable.SimplePageIndicator_scaleable, true);
            mPageCount = a.getInt(R.styleable.SimplePageIndicator_pageCount, 2);
            mSelectedDimen = a.getDimension(R.styleable.SimplePageIndicator_selectedSize,
                    getResources().getDimensionPixelSize(R.dimen.selected_indicator_size));
            mUnselectedDimen = a.getDimension(R.styleable.SimplePageIndicator_unselectedSize,
                    getResources().getDimensionPixelSize(R.dimen.unselected_indicator_size));
        } finally {
            a.recycle();
        }

        init();

    }

    private void init(){
        inflate(getContext(), R.layout.simple_page_indicator, this);

        mIndicatorContainer = (LinearLayout)findViewById(R.id.indicator_container);

        mIndicatorMargin = getResources().getDimensionPixelSize(R.dimen.indicator_margin);

        for(int i = 0; i < mPageCount; i++){
            if(i == 0){
                mCircles.add(createIndicator(true));
            } else {
                mCircles.add(createIndicator(false));
            }
        }
    }

    private ImageView createIndicator(boolean isFirst){
        ImageView view = new ImageView(mContext);
        view.setImageResource(mResID);

        LinearLayout.LayoutParams lp;

        if(isFirst){
            lp = new LinearLayout.LayoutParams(
                    (int)mSelectedDimen,
                    (int)mSelectedDimen
            );
            view.setAlpha(mSelectedAlpha);
        } else {
            lp = new LinearLayout.LayoutParams(
                    (int)mUnselectedDimen,
                    (int)mUnselectedDimen
            );
            view.setAlpha(mUnselectedAlpha);
        }

        lp.setMargins(mIndicatorMargin, mIndicatorMargin,
                mIndicatorMargin, mIndicatorMargin);

        mIndicatorContainer.addView(view, lp);

        return view;
    }

    public void updateIndicator(int prePosition, int newPosition) {
        animateIndicator(mCircles.get(prePosition), mSelectedAlpha, mUnselectedAlpha,
                mSelectedDimen, mUnselectedDimen);
        animateIndicator(mCircles.get(newPosition), mUnselectedAlpha, mSelectedAlpha,
                mUnselectedDimen, mSelectedDimen);
    }

    private void animateIndicator(final View view, float startAlpha, float endAlpha,
                                  float fromDimen, float toDimen){
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", startAlpha, endAlpha);
        final ValueAnimator size = ValueAnimator.ofFloat(fromDimen, toDimen);
        size.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float)animation.getAnimatedValue();
                view.getLayoutParams().width = (int) value;
                view.getLayoutParams().height = (int) value;
                view.requestLayout();
            }
        });
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(alpha, size);
        anim.setDuration(300);
        anim.setInterpolator(new PathInterpolator(0.6f,0f,0.4f,1f));
        anim.start();
    }

}
