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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cyanogenmod.filemanager.util.AndroidHelper;

/**
 * A {@link ListView} implementation for remove items using flinging gesture.
 */
public class FlingerListView extends ListView {

    /**
     * An interface for dispatch flinging gestures
     */
    public interface OnItemFlingerListener {
        /**
         * Method invoke when a row item is going to be flinging.
         *
         * @param parent The AbsListView where the flinging happened
         * @param view The view within the AbsListView that was flingered
         * @param position The position of the view in the list
         * @param id The row id of the item that was flingered
         * @return boolean If the flinging operation must continue
         */
        boolean onItemFlingerStart(AdapterView<?> parent, View view, int position, long id);

        /**
         * Method invoke when a row item was flingered.
         *
         * @param responder The responder to the flinging action. You MUST be invoke one
         * the option methods of this interface (accept or cancel).
         * @param parent The AbsListView where the flinging happened
         * @param view The view within the AbsListView that was flingered
         * @param position The position of the view in the list
         * @param id The row id of the item that was flingered
         */
        void onItemFlingerEnd(
                OnItemFlingerResponder responder,
                AdapterView<?> parent, View view, int position, long id);
    }

    /**
     * An interface for response to {@link OnItemFlingerListener#onItemFlingerEnd(
     * OnItemFlingerResponder, AdapterView, View, int, long)} event.
     */
    public interface OnItemFlingerResponder {
        /**
         * Method that indicates that the item was removed. This method MUST be called
         * after the remove of item (that it's responsibility of the invoker) to ensure
         * that all references are cleaned.
         */
        void accept();

        /**
         * Method that indicates that the action must be cancelled, and the item
         * MUST NOT be removed.
         */
        void cancel();
    }

    /**
     * An implementation of {@link OnItemFlingerResponder}
     */
    private class ItemFlingerResponder implements OnItemFlingerResponder {
        /**
         * @hide
         */
        View mItemView;

        /**
         * Constructor of <code></code>. For synthetic-access only.
         */
        public ItemFlingerResponder() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept() {
            // Remove the flinger effect
            this.mItemView.setTranslationX(0);
            clearVars();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void cancel() {
            // Remove the flinger effect
            this.mItemView.setTranslationX(0);
            clearVars();
        }
    }

    /**
     * The time after that the pressed is sending to the view.
     */
    private static final long PRESSED_DELAY_TIME = 250L;

    /**
     * The default percentage for flinging remove event.
     */
    private static final float DEFAULT_FLING_REMOVE_PERCENTAJE = 0.40f;

    /**
     * The minimum flinger threshold to start the flinger motion in x axis (in dp)
     */
    private static final int MIN_FLINGER_THRESHOLD_X = 24;

    /**
     * The minimum flinger threshold to start the flinger motion in y axis (in dp)
     */
    private static final int MIN_FLINGER_THRESHOLD_Y = 8;

    // Flinging data
    private int mTranslationX = 0;
    private int mStartX = 0;
    private int mStartY = 0;
    private int mCurrentX = 0;
    private int mCurrentY = 0;
    private int mFlingingViewPos;
    private View mFlingingView;
    private boolean mFlingingViewPressed;
    private int mFlingingViewWidth;
    private boolean mScrolling;
    private boolean mScrollInAnimation;
    private boolean mFlinging;
    private boolean mFlingingStarted;
    private boolean mMoveStarted;
    private boolean mLongPress;
    private Runnable mLongPressDetection;

    private float mFlingRemovePercentaje;
    private float mFlingThresholdX;
    private float mFlingThresholdY;
    private OnItemFlingerListener mOnItemFlingerListener;

    /**
     * Constructor of <code>FlingerListView</code>.
     *
     * @param context The current context
     */
    public FlingerListView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>FlingerListView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public FlingerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>FlingerListView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public FlingerListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view
     */
    private void init() {
        //Initialize variables
        this.mFlingRemovePercentaje = DEFAULT_FLING_REMOVE_PERCENTAJE;
        this.mFlingThresholdX = AndroidHelper.convertDpToPixel(
                getContext(), MIN_FLINGER_THRESHOLD_X);
        this.mFlingThresholdY = AndroidHelper.convertDpToPixel(
                getContext(), MIN_FLINGER_THRESHOLD_Y);
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mScrollInAnimation = (scrollState == SCROLL_STATE_FLING);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
            }
        });
        mScrollInAnimation = false;
    }

    /**
     * Method that returns the percentage (from 0 to 1) of the item view width on which
     * an OnItemFlinger event occurs
     *
     * @return float The percentage (from 0 to 1) of the item view width
     */
    public float getFlingRemovePercentaje() {
        return this.mFlingRemovePercentaje;
    }

    /**
     *  Method that sets the percentage (from 0 to 1) of the item view width on which
     * an OnItemFlinger event occurs
     *
     * @param flingRemovePercentaje The percentage (from 0 to 1) of the item view width
     */
    public void setFlingRemovePercentaje(float flingRemovePercentaje) {
        if (flingRemovePercentaje < 0) {
            this.mFlingRemovePercentaje = 0;
        } else if (flingRemovePercentaje > 1) {
            this.mFlingRemovePercentaje = 1;
        } else {
            this.mFlingRemovePercentaje = flingRemovePercentaje;
        }
    }

    /**
     * Method that sets the listener for listen flinging events
     *
     * @param mOnItemFlingerListener The flinging listener
     */
    public void setOnItemFlingerListener(OnItemFlingerListener mOnItemFlingerListener) {
        this.mOnItemFlingerListener = mOnItemFlingerListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // If no flinger support is request, don't change the default behaviour
        if (this.mOnItemFlingerListener == null) {
            return super.onTouchEvent(ev);
        }

        // This events are trap inside this method
        setLongClickable(false);
        setClickable(false);

        // Get information about the x and y
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        // Detect the motion
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            // Clean variables
            this.mScrolling = false;
            this.mFlinging = false;
            this.mLongPress = false;
            this.mFlingingStarted = false;
            this.mMoveStarted = false;
            this.mFlingingViewPressed = false;
            if (this.mFlingingView != null) {
                this.mFlingingView.setTranslationX(0);
            }

            // Get the view to fling
            this.mFlingingViewPos = pointToPosition(x, y);
            if (this.mFlingingViewPos != INVALID_POSITION) {
                this.mStartX = (int) ev.getX();
                this.mCurrentX = (int) ev.getX();
                this.mStartY = (int) ev.getY();
                this.mCurrentY = (int) ev.getY();
                this.mTranslationX = 0;
                this.mFlingingView =
                        getChildAt(this.mFlingingViewPos - getFirstVisiblePosition());
                this.mFlingingViewPressed = true;

                // Detect long press event
                if (getOnItemLongClickListener() != null) {
                    this.mLongPressDetection = new Runnable() {
                        @Override
                        @SuppressWarnings({"synthetic-access" })
                        public void run() {
                            if (!FlingerListView.this.mFlingingStarted &&
                                !FlingerListView.this.mMoveStarted) {
                                // Notify the long-click
                                FlingerListView.this.mLongPress = true;
                                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                FlingerListView.this.mFlingingView.setPressed(false);
                                FlingerListView.this.mFlingingViewPressed = false;
                                getOnItemLongClickListener().onItemLongClick(
                                        FlingerListView.this,
                                        FlingerListView.this.mFlingingView,
                                        FlingerListView.this.mFlingingViewPos,
                                        FlingerListView.this.mFlingingView.getId());
                            }
                        }
                    };
                    this.mFlingingView.postDelayed(
                            this.mLongPressDetection,
                            ViewConfiguration.getLongPressTimeout());
                }

                // Calculate the item size
                Rect r = new Rect();
                this.mFlingingView.getDrawingRect(r);
                this.mFlingingViewWidth = r.width();

                // Set the pressed state
                this.mFlingingView.postDelayed(new Runnable() {
                    @Override
                    @SuppressWarnings("synthetic-access")
                    public void run() {
                        FlingerListView.this.mFlingingView.setPressed(
                                FlingerListView.this.mFlingingViewPressed);
                    }
                }, PRESSED_DELAY_TIME);

                // If not the view is not scrolling the capture event
                if (!mScrollInAnimation) {
                    return true;
                }
            }
            break;

        case MotionEvent.ACTION_MOVE:
            // Detect scrolling
            this.mCurrentY = (int)ev.getY();
            this.mScrolling =
                    Math.abs(this.mCurrentY - this.mStartY) > this.mFlingThresholdY;
            if (this.mFlingingStarted) {
                // Don't allow scrolling
                this.mScrolling = false;
            }

            if ((this.mFlingingStarted || this.mScrolling) && this.mFlingingView != null) {
                this.mFlingingView.removeCallbacks(this.mLongPressDetection);
                this.mFlingingView.setPressed(false);
                this.mFlingingViewPressed = false;
            }

            // With flinging support
            if (this.mFlingingView != null) {
                // Only if event has changed (and only to the right and if not scrolling)
                if (!this.mScrolling) {
                    if (ev.getX() >= this.mStartX && (ev.getX() - this.mCurrentX != 0)) {
                        this.mCurrentX = (int)ev.getX();
                        this.mTranslationX = this.mCurrentX - this.mStartX;
                        this.mFlingingView.setTranslationX(this.mTranslationX);
                        this.mFlingingView.setPressed(false);
                        this.mFlingingViewPressed = false;

                        // Started
                        if (!this.mFlingingStarted) {
                            // Flinging starting
                            if (!mMoveStarted) {
                                if (!this.mOnItemFlingerListener.onItemFlingerStart(
                                        this,
                                        this.mFlingingView,
                                        this.mFlingingViewPos,
                                        this.mFlingingView.getId())) {
                                    this.mCurrentX = 0;
                                    this.mTranslationX = 0;
                                    this.mFlingingView.setTranslationX(this.mTranslationX);
                                    this.mFlingingView.setPressed(false);
                                    this.mFlingingViewPressed = false;
                                    break;
                                }
                            }
                            mMoveStarted = true;
                            if (this.mTranslationX > this.mFlingThresholdX) {
                                this.mFlingingStarted = true;
                            }
                        }

                        // Detect if flinging occurs
                        float flingLimit =
                                (this.mFlingingViewWidth * this.mFlingRemovePercentaje);
                        if (!this.mFlinging && this.mTranslationX > flingLimit) {
                            // Flinging occurs. Mark and raise an event
                            this.mFlinging = true;
                            final ItemFlingerResponder responder =
                                    new ItemFlingerResponder();
                            responder.mItemView = this.mFlingingView;

                            // Request a response (we need to do this in background for
                            // get new events)
                            this.mFlingingView.post(new Runnable() {
                                @Override
                                @SuppressWarnings("synthetic-access")
                                public void run() {
                                    FlingerListView.
                                        this.mOnItemFlingerListener.onItemFlingerEnd(
                                            responder,
                                            FlingerListView.this,
                                            FlingerListView.this.mFlingingView,
                                            FlingerListView.this.mFlingingViewPos,
                                            FlingerListView.this.mFlingingView.getId());
                                }
                            });
                        }
                    }
                } else {
                    this.mCurrentX = 0;
                    this.mTranslationX = 0;
                    this.mFlingingView.setTranslationX(this.mTranslationX);
                    this.mFlingingView.setPressed(false);
                    this.mFlingingViewPressed = false;
                }
            }
            if (this.mFlingingStarted) {
                return true;
            }
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            // Clear flinging (only if not waiting confirmation)
            // On scrolling, flinging has no effect
            if (!this.mFlinging || this.mScrolling) {
                this.mStartX = 0;
                this.mCurrentX = 0;
                this.mTranslationX = 0;
                if (this.mFlingingView != null) {
                    this.mFlingingView.setTranslationX(0);
                }
            } else {
                // Force to display at the limit
                if (this.mFlingingView != null) {
                    float flingLimit =
                            (this.mFlingingViewWidth * this.mFlingRemovePercentaje);
                    this.mFlingingView.setTranslationX(flingLimit);
                }
            }

            // What is the motion
            if (!this.mScrolling && this.mFlingingView != null) {
                if (!this.mMoveStarted && !this.mLongPress) {
                    this.mFlingingView.removeCallbacks(this.mLongPressDetection);
                    this.mFlingingView.setPressed(true);
                    this.mFlingingViewPressed = true;
                    performItemClick(
                            this.mFlingingView,
                            this.mFlingingViewPos,
                            this.mFlingingView.getId());

                    // Handled
                    this.mFlingingView.postDelayed(new Runnable() {
                        @Override
                        @SuppressWarnings("synthetic-access")
                        public void run() {
                            FlingerListView.this.mFlingingView.setPressed(false);
                            FlingerListView.this.mFlingingViewPressed = false;
                        }
                    }, PRESSED_DELAY_TIME);
                }
                return true;
            }

            // Scrolling -> Remove any status (don't handle event)
            if (this.mFlingingView != null) {
                this.mFlingingView.setPressed(false);
                this.mFlingingViewPressed = false;
            }
            break;

        default:
            break;
        }

        return super.onTouchEvent(ev);
    }

    /**
     * Method that clean the internal variables
     * @hide
     */
    void clearVars() {
        this.mScrolling = false;
        this.mFlinging = false;
        this.mLongPress = false;
        this.mFlingingStarted = false;
        this.mMoveStarted = false;
        this.mFlingingView = null;
    }
}
