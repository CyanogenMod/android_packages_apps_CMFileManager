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
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.DiskUsageCategory;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A class that display graphically the usage of a mount point.
 */
public class DiskUsageGraph extends View {

    /**
     * This is a list for accessing the loaded colors
     */
    public static final List<Integer> COLOR_LIST = new ArrayList<Integer>();
    /**
     * This is an internal color id reference
     */
    private static final List<Integer> INTERNAL_COLOR_LIST = new ArrayList<Integer>() {
        {

            // Material Blue
            add(R.color.material_palette_blue_1);
            add(R.color.material_palette_blue_2);
            add(R.color.material_palette_blue_3);
            add(R.color.material_palette_blue_4);

            // Material Lime
            add(R.color.material_palette_green_1);
            add(R.color.material_palette_green_2);
            add(R.color.material_palette_green_3);
            add(R.color.material_palette_green_4);

            // Material Orange
            add(R.color.material_palette_orange_1);
            add(R.color.material_palette_orange_2);
            add(R.color.material_palette_orange_3);
            add(R.color.material_palette_orange_4);

        }
    };

    /**
     * Initialize the color assets into memory for direct access
     */
    private void initializeColors() {
        // Only load the colors if needed
        if (COLOR_LIST.size() == 0) {
            for (int colorId : INTERNAL_COLOR_LIST) {
                COLOR_LIST.add(getContext().getResources().getColor(colorId));
            }
        }
    }

    /**
     * @hide
     */
    int mDiskWarningAngle = (360 * 95) / 100;

    /**
     * @hide
     */
    final List<DrawingObject> mDrawingObjects =
            Collections.synchronizedList(new ArrayList<DiskUsageGraph.DrawingObject>(2));

    /**
     * @hide
     * drawing objects lock
     */
    static final int[] LOCK = new int[0];

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context The current context
     */
    public DiskUsageGraph(Context context) {
        this(context, null);
    }

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context The current context
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public DiskUsageGraph(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context  The current context
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style will be applied
     *        (beyond what is included in the theme). This may either be an attribute
     *        resource, whose value will be retrieved from the current theme, or an
     *        explicit style resource.
     */
    public DiskUsageGraph(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeColors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(parentWidth, parentHeight);
        this.setMeasuredDimension(size, size);
    }

    /**
     * Method that sets the free disk space percentage after the widget change his color to advise
     * the user
     *
     * @param percentage The free disk space percentage
     */
    public void setFreeDiskSpaceWarningLevel(int percentage) {
        this.mDiskWarningAngle = (360 * percentage) / 100;
    }

    // Handle thread for drawing calculations
    private Future mAnimationFuture = null;
    private static ExecutorService sThreadPool = Executors.newFixedThreadPool(1);

    /**
     * Method that draw the disk usage.
     *
     * @param diskUsage {@link com.cyanogenmod.filemanager.model.DiskUsage} The disk usage params
     */
    public void drawDiskUsage(DiskUsage diskUsage) {

        // Clear if a current drawing exit
        if (mAnimationFuture != null && !mAnimationFuture.isCancelled()) {
            mAnimationFuture.cancel(true);
        }

        // Clear canvas
        synchronized (LOCK) {
            this.mDrawingObjects.clear();
        }
        invalidate();

        // Start drawing thread
        AnimationDrawingRunnable animationDrawingRunnable = new AnimationDrawingRunnable(diskUsage);
        mAnimationFuture = sThreadPool.submit(animationDrawingRunnable);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        //Draw super surface
        super.onDraw(canvas);

        //Draw all the drawing objects
        synchronized (LOCK) {
            for (DrawingObject dwo : this.mDrawingObjects) {
                canvas.drawArc(dwo.mRectF, dwo.mStartAngle, dwo.mSweepAngle, false, dwo.mPaint);
            }
        }
    }

    /**
     * A thread for drawing the animation of the graph.
     */
    private class AnimationDrawingRunnable implements Runnable {

        private final DiskUsage mDiskUsage;

        // Delay in between UI updates and slow down calculations
        private static final long ANIMATION_DELAY = 1l;

        // Slop space adjustment for space between segments
        private static final int SLOP = 2;

        // flags
        private static final boolean USE_COLORS = true;

        /**
         * Constructor of <code>AnimationDrawingThread</code>.
         *
         * @param diskUsage The disk usage
         */
        public AnimationDrawingRunnable(DiskUsage diskUsage) {
            this.mDiskUsage = diskUsage;
        }

        private void sleepyTime() {
            try {
                Thread.sleep(ANIMATION_DELAY);
            } catch (InterruptedException ignored) {
            }
        }

        private void redrawCanvas() {
            //Redraw the canvas
            post(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                }
            });
        }

        private void drawTotal(Rect rect, int stroke) {
            // Draw total
            DrawingObject drawingObject = createDrawingObject(rect, "disk_usage_total_color",
                    stroke);
            synchronized (LOCK) {
                mDrawingObjects.add(drawingObject);
            }
            while (drawingObject.mSweepAngle < 360) {
                sleepyTime();
                drawingObject.mSweepAngle++;
                redrawCanvas();
            }
        }

        private void drawUsed(Rect rect, int stroke, float used) {
            // Draw used
            DrawingObject drawingObject = createDrawingObject(rect, "disk_usage_used_color", stroke);
            synchronized (LOCK) {
                mDrawingObjects.add(drawingObject);
            }
            while (drawingObject.mSweepAngle < used) {
                sleepyTime();
                drawingObject.mSweepAngle++;
                redrawCanvas();
            }
        }

        private void drawUsedWithColors(Rect rect, int stroke) {
            // Draw used segments
            if (mDiskUsage != null) {
                int lastSweepAngle = 0;
                float catUsed = 100.0f;
                int color;
                int index = 0;
                for (DiskUsageCategory category : mDiskUsage.getUsageCategoryList()) {
                    catUsed = (category.getSizeBytes() * 100) / mDiskUsage.getTotal(); // calc percent
                    catUsed = (catUsed < 1) ? 1 : catUsed; // Normalize
                    catUsed = (360 * catUsed) / 100; // calc angle

                    // Figure out a color
                    if (index > -1 && index < COLOR_LIST.size()) {
                        color = COLOR_LIST.get(index);
                        index++;
                    } else {
                        index = 0;
                        color = COLOR_LIST.get(index);
                    }

                    DrawingObject drawingObject = createDrawingObjectNoTheme(rect, color, stroke);
                     drawingObject.mStartAngle += lastSweepAngle;
                    synchronized (LOCK) {
                        mDrawingObjects.add(drawingObject);
                    }
                    while (drawingObject.mSweepAngle < catUsed + SLOP) {
                        sleepyTime();
                        drawingObject.mSweepAngle++;
                        redrawCanvas();
                    }
                    lastSweepAngle += drawingObject.mSweepAngle - SLOP;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            //Get information about the drawing zone, and adjust the size
            Rect rect = new Rect();
            getDrawingRect(rect);
            int stroke = (rect.width() / 2) / 2;
            rect.left += stroke / 2;
            rect.right -= stroke / 2;
            rect.top += stroke / 2;
            rect.bottom -= stroke / 2;

            float used = 100.0f;
            if (this.mDiskUsage != null && this.mDiskUsage.getTotal() != 0) {
                used = (this.mDiskUsage.getUsed() * 100) / this.mDiskUsage.getTotal();
            }
            //Translate to angle
            used = (360 * used) / 100;

            // Draws out the graph background color
            drawTotal(rect, stroke);

            // Draw the usage
            if (USE_COLORS) {
                drawUsedWithColors(rect, stroke);
            } else {
                drawUsed(rect, stroke, used);
            }

        }

        /**
         * Method that creates the drawing object.
         *
         * @param rect                 The area of drawing
         * @param colorResourceThemeId The theme resource identifier of the color
         * @param stroke               The stroke width
         *
         * @return DrawingObject The drawing object
         */
        private DrawingObject createDrawingObject(
                Rect rect, String colorResourceThemeId, int stroke) {
            DrawingObject out = new DrawingObject();
            out.mSweepAngle = 0;
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            out.mPaint.setColor(theme.getColor(getContext(), colorResourceThemeId));
            out.mPaint.setStrokeWidth(stroke);
            out.mPaint.setAntiAlias(true);
            out.mPaint.setStrokeCap(Paint.Cap.BUTT);
            out.mPaint.setStyle(Paint.Style.STROKE);
            out.mRectF = new RectF(rect);
            return out;
        }

        /**
         * Method that creates the drawing object.
         *
         * @param rect                 The area of drawing
         * @param color                Integer id of the color
         * @param stroke               The stroke width
         *
         * @return DrawingObject The drawing object
         *
         * [TODO][MSB]: Implement colors for sections into theme
         */
        @Deprecated
        private DrawingObject createDrawingObjectNoTheme(
                Rect rect, int color, int stroke) {
            DrawingObject out = new DrawingObject();
            out.mSweepAngle = 0;
            out.mPaint.setColor(color);
            out.mPaint.setStrokeWidth(stroke);
            out.mPaint.setAntiAlias(true);
            out.mPaint.setStrokeCap(Paint.Cap.BUTT);
            out.mPaint.setStyle(Paint.Style.STROKE);
            out.mRectF = new RectF(rect);
            return out;
        }
    }

    /**
     * A class with information about a drawing object.
     */
    private class DrawingObject {
        DrawingObject() {/**NON BLOCK**/}

        int mStartAngle = -180;
        int mSweepAngle = 0;
        Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF mRectF = new RectF();
    }

}
