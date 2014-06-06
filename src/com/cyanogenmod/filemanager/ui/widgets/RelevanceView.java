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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import os.ransj.filemanager.R;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A widget for display a relevance widget.
 */
public class RelevanceView extends View {

    private final Paint mBorderPaint = new Paint();
    private final Paint mRelevancePaint = new Paint();
    private float mRelevance;

    private Map<Integer, Integer> mColors;

    /**
     * Constructor of <code>RelevanceView</code>.
     *
     * @param context The current context
     */
    public RelevanceView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>RelevanceView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public RelevanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>RelevanceView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public RelevanceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        //Configure the paints
        float density = getResources().getDisplayMetrics().density;
        this.mBorderPaint.setColor(getResources().getColor(R.color.relevance_border));
        this.mBorderPaint.setStrokeWidth((1 * density) + 0.5f);
        this.mRelevancePaint.setColor(Color.TRANSPARENT);

        //Create the color map
        this.mColors = new TreeMap<Integer, Integer>();
        this.mColors.put(Integer.valueOf(25),
                Integer.valueOf(getResources().getColor(R.color.relevance_percentil_25)));
        this.mColors.put(Integer.valueOf(50),
                Integer.valueOf(getResources().getColor(R.color.relevance_percentil_50)));
        this.mColors.put(Integer.valueOf(75),
                Integer.valueOf(getResources().getColor(R.color.relevance_percentil_75)));
        this.mColors.put(Integer.valueOf(100),
                Integer.valueOf(getResources().getColor(R.color.relevance_percentil_100)));

        //Relevance is 0 until is sets
        this.mRelevance = 0.0f;
    }

    /**
     * Set the relevance of the widget.
     *
     * @param relevance the mRelevance to set
     */
    public void setRelevance(float relevance) {
        this.mRelevance = relevance;
        if (relevance < 0) {
            this.mRelevance = 0.0f;
        }
        if (relevance > 100) {
            this.mRelevance = 100.0f;
        }

        //Change the color of the relevance depending on his percentage
        Iterator<Integer> it = this.mColors.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            if (this.mRelevance <= key.intValue()) {
                this.mRelevancePaint.setColor(this.mColors.get(key).intValue());
                break;
            }
        }

        //Invalidate the widget for drawing again
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        //Draw the relevance
        canvas.drawRect(0, 0, (this.mRelevance * w / 100), h, this.mRelevancePaint);

        //Draw the border
        canvas.drawLine(0, 0, w, 0, this.mBorderPaint);
        canvas.drawLine(w, 0, w, h, this.mBorderPaint);
        canvas.drawLine(w, h, 0, h, this.mBorderPaint);
        canvas.drawLine(0, h, 0, 0, this.mBorderPaint);
    }



}
