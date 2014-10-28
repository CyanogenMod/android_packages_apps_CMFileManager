/*
 * Copyright (C) 2013 The Android Open Source Project
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
 *
 *
 * (modified from android.support.v4.app)
 */

package com.cyanogenmod.filemanager.ui.widgets;

import java.lang.reflect.Method;

import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * This class provides a handy way to tie together the functionality of
 * {@link DrawerLayout} and the framework <code>ActionBar</code> to implement
 * the recommended design for navigation drawers.
 *
 * <p>
 * To use <code>ActionBarDrawerToggle</code>, create one in your Activity and
 * call through to the following methods corresponding to your Activity
 * callbacks:
 * </p>
 *
 * <ul>
 * <li>
 * {@link Activity#onConfigurationChanged(android.content.res.Configuration)
 * onConfigurationChanged}</li>
 * <li>{@link Activity#onOptionsItemSelected(android.view.MenuItem)
 * onOptionsItemSelected}</li>
 * </ul>
 *
 * <p>
 * Call {@link #syncState()} from your <code>Activity</code>'s
 * {@link Activity#onPostCreate(android.os.Bundle) onPostCreate} to synchronize
 * the indicator with the state of the linked DrawerLayout after
 * <code>onRestoreInstanceState</code> has occurred.
 * </p>
 *
 * <p>
 * <code>ActionBarDrawerToggle</code> can be used directly as a
 * {@link DrawerLayout.DrawerListener}, or if you are already providing your own
 * listener, call through to each of the listener methods from your own.
 * </p>
 */
public class ActionBarDrawerToggle implements DrawerLayout.DrawerListener {

    /**
     * Allows an implementing Activity to return an
     * {@link ActionBarDrawerToggle.Delegate} to use with ActionBarDrawerToggle.
     */
    public interface DelegateProvider {

        /**
         * @return Delegate to use for ActionBarDrawableToggles, or null if the
         *         Activity does not wish to override the default behavior.
         */
        Delegate getDrawerToggleDelegate();
    }

    public interface Delegate {
        /**
         * @return Up indicator drawable as defined in the Activity's theme, or
         *         null if one is not defined.
         */
        Drawable getThemeUpIndicator();

        /**
         * Set the Action Bar's up indicator drawable and content description.
         *
         * @param upDrawable
         *            - Drawable to set as up indicator
         * @param contentDescRes
         *            - Content description to set
         */
        void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes);

        /**
         * Set the Action Bar's up indicator content description.
         *
         * @param contentDescRes
         *            - Content description to set
         */
        void setActionBarDescription(int contentDescRes);
    }

    private static final int[] THEME_ATTRS = new int[] { R.attr.homeAsUpIndicator };

    private static class ActionBarDrawerToggleImpl {
        public static Drawable getThemeUpIndicator(Activity activity) {
            final TypedArray a = activity.obtainStyledAttributes(THEME_ATTRS);
            final Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        public static Object setActionBarUpIndicator(Object info,
                Activity activity, Drawable drawable, int contentDescRes) {
            if (info == null) {
                info = new SetIndicatorInfo(activity);
            }

            final ActionBar actionBar = activity.getActionBar();
            actionBar.setHomeAsUpIndicator(drawable);
            actionBar.setHomeActionContentDescription(contentDescRes);

            return info;
        }

        public static Object setActionBarDescription(Object info,
                Activity activity, int contentDescRes) {
            if (info == null) {
                info = new SetIndicatorInfo(activity);
            }

            final ActionBar actionBar = activity.getActionBar();
            actionBar.setHomeActionContentDescription(contentDescRes);

            return info;
        }
    }

    @SuppressWarnings("unused")
    private static class SetIndicatorInfo {
        SetIndicatorInfo(Activity activity) {
            try {
                Method setHomeAsUpIndicator = ActionBar.class.getDeclaredMethod(
                        "setHomeAsUpIndicator", Drawable.class);
                Method setHomeActionContentDescription = ActionBar.class
                        .getDeclaredMethod("setHomeActionContentDescription",
                                Integer.TYPE);

                // If we got the method we won't need the stuff below.
                return;
            } catch (NoSuchMethodException e) {
                // Oh well. We'll use the other mechanism below instead.
            }

            final View home = activity.findViewById(android.R.id.home);
            if (home == null) {
                // Action bar doesn't have a known configuration, an OEM messed
                // with things.
                return;
            }

            final ViewGroup parent = (ViewGroup) home.getParent();
            final int childCount = parent.getChildCount();
            if (childCount != 2) {
                // No idea which one will be the right one, an OEM messed with
                // things.
                return;
            }

            final View first = parent.getChildAt(0);
            final View second = parent.getChildAt(1);
            final View up = first.getId() == android.R.id.home ? second : first;
        }
    }

    /** Fraction of its total width by which to offset the toggle drawable. */
    private static final float TOGGLE_DRAWABLE_OFFSET = 1 / 3f;

    // android.R.id.home as defined by public API in v11
    private static final int ID_HOME = 0x0102002c;

    private final Activity mActivity;
    private final Delegate mActivityImpl;
    private final DrawerLayout mDrawerLayout;
    private boolean mDrawerIndicatorEnabled = true;

    private Drawable mThemeImage;
    private Drawable mDrawerImage;
    private SlideDrawable mSlider;
    private int mDrawerImageResource;
    private final int mOpenDrawerContentDescRes;
    private final int mCloseDrawerContentDescRes;

    private Object mSetIndicatorInfo;

    /**
     * Construct a new ActionBarDrawerToggle.
     *
     * <p>
     * The given {@link Activity} will be linked to the specified
     * {@link DrawerLayout}. The provided drawer indicator drawable will animate
     * slightly off-screen as the drawer is opened, indicating that in the open
     * state the drawer will move off-screen when pressed and in the closed
     * state the drawer will move on-screen when pressed.
     * </p>
     *
     * <p>
     * String resources must be provided to describe the open/close drawer
     * actions for accessibility services.
     * </p>
     *
     * @param activity
     *            The Activity hosting the drawer
     * @param drawerLayout
     *            The DrawerLayout to link to the given Activity's ActionBar
     * @param drawerImageRes
     *            A Drawable resource to use as the drawer indicator
     * @param openDrawerContentDescRes
     *            A String resource to describe the "open drawer" action for
     *            accessibility
     * @param closeDrawerContentDescRes
     *            A String resource to describe the "close drawer" action for
     *            accessibility
     */
    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
            int drawerImageRes, int openDrawerContentDescRes,
            int closeDrawerContentDescRes) {
        mActivity = activity;

        // Allow the Activity to provide an impl
        if (activity instanceof DelegateProvider) {
            mActivityImpl = ((DelegateProvider) activity)
                    .getDrawerToggleDelegate();
        } else {
            mActivityImpl = null;
        }

        mDrawerLayout = drawerLayout;
        mDrawerImageResource = drawerImageRes;
        mOpenDrawerContentDescRes = openDrawerContentDescRes;
        mCloseDrawerContentDescRes = closeDrawerContentDescRes;

        mThemeImage = getThemeUpIndicator();
        mDrawerImage = activity.getResources().getDrawable(drawerImageRes);
        mSlider = new SlideDrawable(mDrawerImage);
        mSlider.setOffset(TOGGLE_DRAWABLE_OFFSET);
    }

    /**
     * Synchronize the state of the drawer indicator/affordance with the linked
     * DrawerLayout.
     *
     * <p>
     * This should be called from your <code>Activity</code>'s
     * {@link Activity#onPostCreate(android.os.Bundle) onPostCreate} method to
     * synchronize after the DrawerLayout's instance state has been restored,
     * and any other time when the state may have diverged in such a way that
     * the ActionBarDrawerToggle was not notified. (For example, if you stop
     * forwarding appropriate drawer events for a period of time.)
     * </p>
     */
    public void syncState() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mSlider.setPosition(1);
        } else {
            mSlider.setPosition(0);
        }

        if (mDrawerIndicatorEnabled) {
            setActionBarUpIndicator(
                    mSlider,
                    mDrawerLayout.isDrawerOpen(Gravity.START) ? mCloseDrawerContentDescRes
                            : mOpenDrawerContentDescRes);
        }
    }

    /**
     * Enable or disable the drawer indicator. The indicator defaults to
     * enabled.
     *
     * <p>
     * When the indicator is disabled, the <code>ActionBar</code> will revert to
     * displaying the home-as-up indicator provided by the <code>Activity</code>
     * 's theme in the <code>android.R.attr.homeAsUpIndicator</code> attribute
     * instead of the animated drawer glyph.
     * </p>
     *
     * @param enable
     *            true to enable, false to disable
     */
    public void setDrawerIndicatorEnabled(boolean enable) {
        if (enable != mDrawerIndicatorEnabled) {
            if (enable) {
                setActionBarUpIndicator(
                        mSlider,
                        mDrawerLayout.isDrawerOpen(Gravity.START) ? mCloseDrawerContentDescRes
                                : mOpenDrawerContentDescRes);
            } else {
                setActionBarUpIndicator(mThemeImage, 0);
            }
            mDrawerIndicatorEnabled = enable;
        }
    }

    /**
     * @return true if the enhanced drawer indicator is enabled, false otherwise
     * @see #setDrawerIndicatorEnabled(boolean)
     */
    public boolean isDrawerIndicatorEnabled() {
        return mDrawerIndicatorEnabled;
    }

    /**
     * This method replaces the drawer image resource with a new one.
     *
     * @param newDrawerImageRes
     *            The new resource id
     */
    public void setDrawerImageResource(int newDrawerImageRes) {
        mDrawerImageResource = newDrawerImageRes;
        mDrawerImage = mActivity.getResources().getDrawable(
                mDrawerImageResource);
        mSlider = new SlideDrawable(mDrawerImage);
        mSlider.setOffset(TOGGLE_DRAWABLE_OFFSET);
        syncState();
    }

    /**
     * This method should always be called by your <code>Activity</code>'s
     * {@link Activity#onConfigurationChanged(android.content.res.Configuration)
     * onConfigurationChanged} method.
     *
     * @param newConfig
     *            The new configuration
     */
    public void onConfigurationChanged(Configuration newConfig) {
        // Reload drawables that can change with configuration
        mThemeImage = getThemeUpIndicator();
        mDrawerImage = mActivity.getResources().getDrawable(
                mDrawerImageResource);
        syncState();
    }

    /**
     * This method should be called by your <code>Activity</code>'s
     * {@link Activity#onOptionsItemSelected(android.view.MenuItem)
     * onOptionsItemSelected} method. If it returns true, your
     * <code>onOptionsItemSelected</code> method should return true and skip
     * further processing.
     *
     * @param item
     *            the MenuItem instance representing the selected menu item
     * @return true if the event was handled and further processing should not
     *         occur
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == ID_HOME
                && mDrawerIndicatorEnabled) {
            if (mDrawerLayout.isDrawerVisible(Gravity.START)) {
                mDrawerLayout.closeDrawer(Gravity.START);
            } else {
                mDrawerLayout.openDrawer(Gravity.START);
            }
            return true;
        }
        return false;
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use
     * your ActionBarDrawerToggle instance directly as your DrawerLayout's
     * listener, you should call through to this method from your own listener
     * object.
     *
     * @param drawerView
     *            The child view that was moved
     * @param slideOffset
     *            The new offset of this drawer within its range, from 0-1
     */
    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        float glyphOffset = mSlider.getPosition();
        if (slideOffset > 0.5f) {
            glyphOffset = Math.max(glyphOffset,
                    Math.max(0.f, slideOffset - 0.5f) * 2);
        } else {
            glyphOffset = Math.min(glyphOffset, slideOffset * 2);
        }
        mSlider.setPosition(glyphOffset);
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use
     * your ActionBarDrawerToggle instance directly as your DrawerLayout's
     * listener, you should call through to this method from your own listener
     * object.
     *
     * @param drawerView
     *            Drawer view that is now open
     */
    @Override
    public void onDrawerOpened(View drawerView) {
        mSlider.setPosition(1);
        if (mDrawerIndicatorEnabled) {
            setActionBarDescription(mCloseDrawerContentDescRes);
        }
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use
     * your ActionBarDrawerToggle instance directly as your DrawerLayout's
     * listener, you should call through to this method from your own listener
     * object.
     *
     * @param drawerView
     *            Drawer view that is now closed
     */
    @Override
    public void onDrawerClosed(View drawerView) {
        mSlider.setPosition(0);
        if (mDrawerIndicatorEnabled) {
            setActionBarDescription(mOpenDrawerContentDescRes);
        }
    }

    /**
     * {@link DrawerLayout.DrawerListener} callback method. If you do not use
     * your ActionBarDrawerToggle instance directly as your DrawerLayout's
     * listener, you should call through to this method from your own listener
     * object.
     *
     * @param newState
     *            The new drawer motion state
     */
    @Override
    public void onDrawerStateChanged(int newState) {
    }

    Drawable getThemeUpIndicator() {
        if (mActivityImpl != null) {
            return mActivityImpl.getThemeUpIndicator();
        }
        return ActionBarDrawerToggleImpl.getThemeUpIndicator(mActivity);
    }

    void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
        if (mActivityImpl != null) {
            mActivityImpl.setActionBarUpIndicator(upDrawable, contentDescRes);
            return;
        }
        mSetIndicatorInfo = ActionBarDrawerToggleImpl.setActionBarUpIndicator(mSetIndicatorInfo,
                mActivity, upDrawable, contentDescRes);
    }

    void setActionBarDescription(int contentDescRes) {
        if (mActivityImpl != null) {
            mActivityImpl.setActionBarDescription(contentDescRes);
            return;
        }
        mSetIndicatorInfo = ActionBarDrawerToggleImpl.setActionBarDescription(mSetIndicatorInfo,
                mActivity, contentDescRes);
    }

    private class SlideDrawable extends LevelListDrawable implements
            Drawable.Callback {
        private final boolean mHasMirroring = Build.VERSION.SDK_INT > 18;
        private final Rect mTmpRect = new Rect();

        private float mPosition;
        private float mOffset;

        private SlideDrawable(Drawable wrapped) {
            super();

            if (wrapped.isAutoMirrored()) {
                this.setAutoMirrored(true);
            }

            addLevel(0, 0, wrapped);
        }

        /**
         * Sets the current position along the offset.
         *
         * @param position
         *            a value between 0 and 1
         */
        public void setPosition(float position) {
            mPosition = position;
            invalidateSelf();
        }

        public float getPosition() {
            return mPosition;
        }

        /**
         * Specifies the maximum offset when the position is at 1.
         *
         * @param offset
         *            maximum offset as a fraction of the drawable width,
         *            positive to shift left or negative to shift right.
         * @see #setPosition(float)
         */
        public void setOffset(float offset) {
            mOffset = offset;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            copyBounds(mTmpRect);
            canvas.save();

            // Layout direction must be obtained from the activity.
            final boolean isLayoutRTL = mActivity.getWindow().getDecorView()
                    .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            final int flipRtl = isLayoutRTL ? -1 : 1;
            final int width = mTmpRect.width();
            canvas.translate(-mOffset * width * mPosition * flipRtl, 0);

            // Force auto-mirroring if it's not supported by the platform.
            if (isLayoutRTL && !mHasMirroring) {
                canvas.translate(width, 0);
                canvas.scale(-1, 1);
            }

            super.draw(canvas);
            canvas.restore();
        }
    }
}
