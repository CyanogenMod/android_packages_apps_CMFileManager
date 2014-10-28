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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class that display graphically the usage of a mount point.
 */
public class DiskUsageGraph extends View {

    /**
     * @hide
     */
    int mDiskWarningAngle = (360 * 95) / 100;

    private AnimationDrawingThread mThread;
    /**
     * @hide
     */
    final List<DrawingObject> mDrawingObjects =
            Collections.synchronizedList(new ArrayList<DiskUsageGraph.DrawingObject>(2));

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context The current context
     */
    public DiskUsageGraph(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public DiskUsageGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor of <code>DiskUsageGraph</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public DiskUsageGraph(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
     * Method that sets the free disk space percentage after the widget change his color
     * to advise the user
     *
     * @param percentage The free disk space percentage
     */
    public void setFreeDiskSpaceWarningLevel(int percentage) {
        this.mDiskWarningAngle = (360 * percentage) / 100;
    }

    /**
     * Method that draw the disk usage.
     *
     * @param diskUsage The disk usage
     */
    public void drawDiskUsage(DiskUsage diskUsage) {
        // Clear if a current drawing exit
        if (this.mThread != null) {
            this.mThread.exit();
        }
        this.mDrawingObjects.clear();
        invalidate();

        // Start drawing thread
        this.mThread = new AnimationDrawingThread(diskUsage);
        this.mThread.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        //Draw super surface
        super.onDraw(canvas);

        //Draw all the drawing objects
        int cc = this.mDrawingObjects.size();
        for (int i = 0; i < cc; i++) {
            DrawingObject dwo = this.mDrawingObjects.get(i);
            canvas.drawArc(dwo.mRectF, dwo.mStartAngle, dwo.mSweepAngle, false, dwo.mPaint);
        }
    }

    /**
     * A thread for drawing the animation of the graph.
     */
    private class AnimationDrawingThread extends Thread {

        private final DiskUsage mDiskUsage;
        private boolean mRunning;
        private final Object mSync = new Object();
        private int mIndex = 0;

        /**
         * Constructor of <code>AnimationDrawingThread</code>.
         *
         * @param diskUsage The disk usage
         */
        public AnimationDrawingThread(DiskUsage diskUsage) {
            super();
            this.mDiskUsage = diskUsage;
            this.mRunning = false;
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

            float used = 0.0f;
            if (this.mDiskUsage == null) {
                used = 100.0f;
            } else if (this.mDiskUsage.getTotal() != 0) {
                used = (this.mDiskUsage.getUsed() * 100) / this.mDiskUsage.getTotal();
            }
            //Translate to angle
            used = (360 * used) / 100;

            synchronized (this.mSync) {
                this.mRunning = true;
            }
            try {
                boolean disk_warning = false;
                while (this.mRunning) {
                    //Get the current arc
                    DrawingObject dwo = null;
                    if (DiskUsageGraph.this.mDrawingObjects != null
                            && DiskUsageGraph.this.mDrawingObjects.size() > this.mIndex) {
                        dwo = DiskUsageGraph.this.mDrawingObjects.get(this.mIndex);
                    }

                    //Draw the total arc circle and then the used arc circle
                    if (this.mIndex == 0 && dwo == null) {
                        //Initialize the total arc circle
                        DiskUsageGraph.this.mDrawingObjects.add(
                                createDrawingObject(
                                        rect, "disk_usage_total_color", stroke)); //$NON-NLS-1$
                        continue;
                    }
                    if (this.mIndex == 1 && dwo == null) {
                      //Initialize the used arc circle
                        DiskUsageGraph.this.mDrawingObjects.add(
                                createDrawingObject(
                                        rect, "disk_usage_used_color", stroke)); //$NON-NLS-1$
                        continue;
                    }

                    if (this.mIndex == 1 && !disk_warning &&
                            dwo.mSweepAngle >= DiskUsageGraph.this.mDiskWarningAngle) {
                        Theme theme = ThemeManager.getCurrentTheme(getContext());
                        dwo.mPaint.setColor(
                                theme.getColor(
                                        getContext(),
                                        "disk_usage_used_warning_color")); //$NON-NLS-1$
                        disk_warning = true;
                    }

                    //Redraw the canvas
                    post(new Runnable() {
                        @Override
                        public void run() {
                            invalidate();
                        }
                    });

                    //Next draw call
                    dwo.mSweepAngle++;
                    if (this.mIndex >= 1) {
                        //Only fill until used
                        if ((dwo.mSweepAngle >= used) || (this.mIndex > 1)) {
                            synchronized (this.mSync) {
                                break;  //End of the animation
                            }
                        }
                    }
                    if (dwo.mSweepAngle == 360) {
                        this.mIndex++;
                    }

                    try {
                        Thread.sleep(1L);
                    } catch (Throwable ex) {
                        /**NON BLOCK**/
                    }
                }
            } finally {
                try {
                    synchronized (this.mSync) {
                        this.mRunning = false;
                        this.mSync.notify();
                    }
                } catch (Throwable ex) {
                    /**NON BLOCK**/
                }
            }
        }

        /**
         * Method that force the thread to exit.
         */
        public void exit() {
            try {
                synchronized (this.mSync) {
                    if (this.mRunning) {
                        this.mRunning = false;
                        this.mSync.wait();
                    }
                }
            } catch (Throwable ex) {
                /**NON BLOCK**/
            }
        }

        /**
         * Method that creates the drawing object.
         *
         * @param rect The area of drawing
         * @param colorResourceThemeId The theme resource identifier of the color
         * @param stroke The stroke width
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
