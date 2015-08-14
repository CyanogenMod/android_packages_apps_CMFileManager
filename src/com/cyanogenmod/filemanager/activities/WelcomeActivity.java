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

package com.cyanogenmod.filemanager.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.WelcomeAdapter;
import com.cyanogenmod.filemanager.controllers.ViewPagerCustomDuration;
import com.cyanogenmod.filemanager.views.SimplePageIndicator;


/**
 * An activity that welcomes the user and walks them through some basic app functions
 */
public class WelcomeActivity extends Activity {

    private static final String TAG = "WelcomeActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    private static long ANIM_BG_DURATION = 700;
    private static long ANIM_BG_DELAY = 100;
    private static long PULSE_DURATION = 800;

    private int mCurrentScreen;

    private ImageView mNextButton;
    private ViewPagerCustomDuration vp;
    private WelcomeAdapter adapter;
    private ImageView mPrevButton;
    private View mGreenBackground;

    //screen 1
    private FrameLayout mBackground;
    private FrameLayout mBackgroundContainer;
    private ImageView mBgUpperLeft;
    private ImageView mBgUpperRight;
    private ImageView mBgLowerLeft;
    private ImageView mBgLowerRight;

    //screen2
    private RelativeLayout mOobeDevice;
    private float mOobeDeviceCenterX;
    private float mOobeDeviceWidth;
    private ImageView mSelectToolbarButton;
    private ImageView mSelectToolbarHighlight;
    private View mSelectToolbar;
    private float mSelectToolbarInitY;
    private float mSelectToolbarHiddenY;
    private float mSelectToolbarHeight;
    private boolean mSelectToolbarOut = false;
    private AnimatorSet mScaleUpGroup;

    //screen3
    private ImageView mInfoPopupButton;
    private ImageView mInfoPopupHighlight;
    private View mInfoPopup;
    private float mInfoPopupInitY;
    private float mInfoPopupHiddenY;
    private float mInfoPopupHeight;
    private boolean mInfoPopupOut = false;

    //screen 4
    private ImageView mFlyingFolder;
    private View mCloudContainer;
    private boolean mFolderAnimated = false;
    private Button mConnectButton;
    private float mFlyingFolderWidth;
    private float mFlyingFolderHeight;
    private float mFlyingFolderInitX;
    private float mFlyingFolderInitY;
    private AnimatedVectorDrawable mAvd;

    //indicator
    private SimplePageIndicator mSimplePageIndicator;

    private DisplayMetrics mDm;
    private int mScreenWidth;
    private int mScreenHeight;

    private Handler mHandler;

    @Override
    protected void onCreate(android.os.Bundle state) {

        //Save state
        super.onCreate(state);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));

        if (DEBUG) {
            android.util.Log.d(TAG, "WelcomeActivity.onCreate"); //$NON-NLS-1$
        }

        //Set the main layout of the activity
        setContentView(R.layout.welcome);

        mGreenBackground = findViewById(R.id.green_background);

        mCurrentScreen = 0;

        //get screen
        mDm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDm);
        mScreenWidth = mDm.widthPixels;
        mScreenHeight = mDm.heightPixels;

        //initiate background
        mBackground = (FrameLayout)findViewById(R.id.background);
        mBackgroundContainer = (FrameLayout)findViewById(R.id.background_container);
        mBgUpperLeft = (ImageView)findViewById(R.id.oobe_bg_upper_left);
        mBgUpperLeft.setVisibility(View.INVISIBLE);
        mBgUpperRight = (ImageView)findViewById(R.id.oobe_bg_upper_right);
        mBgUpperRight.setVisibility(View.INVISIBLE);
        mBgLowerLeft = (ImageView)findViewById(R.id.oobe_bg_lower_left);
        mBgLowerLeft.setVisibility(View.INVISIBLE);
        mBgLowerRight = (ImageView)findViewById(R.id.oobe_bg_lower_right);
        mBgLowerRight.setVisibility(View.INVISIBLE);

        //setup screen 2
        mOobeDevice = (RelativeLayout)findViewById(R.id.oobe_device);
        mSelectToolbarButton = (ImageView)findViewById(R.id.select_icon);
        mSelectToolbarHighlight = (ImageView)findViewById(R.id.oobe_select_highlight);
        mSelectToolbar = findViewById(R.id.select_toolbar);

        //setup screen 3
        mInfoPopupButton = (ImageView)findViewById(R.id.oobe_info_button);
        mInfoPopup = findViewById(R.id.info_popup);
        mInfoPopupHighlight = (ImageView)findViewById(R.id.oobe_info_highlight);

        //setup screen 4
        mFlyingFolder = (ImageView)findViewById(R.id.flying_folder);
        mCloudContainer = findViewById(R.id.cloud_container);
        mCloudContainer.setVisibility(View.INVISIBLE);
        mCloudContainer.post(new Runnable() {
            @Override
            public void run() {
                setupCloud();
            }
        });
        mConnectButton = (Button)findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                folderFlyAway();
            }
        });

        //setup indicator
        mSimplePageIndicator = (SimplePageIndicator)findViewById(R.id.simple_page_indicator);

        mHandler = new Handler();

        //wait for the illustrations to inflate
        mBgLowerRight.post(animateBackground);
        mBgLowerRight.post(driftBackground);

        mInfoPopup.post(new Runnable() {
            @Override
            public void run() {
                setupOobeDevice();
            }
        });

        adapter = new WelcomeAdapter();
        vp = (ViewPagerCustomDuration) findViewById(R.id.intro_pager);
        mNextButton = (ImageView) findViewById(R.id.nextButton);
        mPrevButton = (ImageView) findViewById(R.id.prevButton);

        vp.setAdapter(adapter);
        vp.setOffscreenPageLimit(3);
        vp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //not using the buttons so on touch action down, alter the scroll speed until idle
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    vp.setScrollDurationFactor(1);
                }

                return false;
            }
        });

        pagePrepare(vp.getCurrentItem());

        vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                pageScrollBehavior(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                pageSelectBehavior(position);
            }

            @Override
            public void onPageScrollStateChanged(int position) {
                if(position == 0){
                    //we are idle and can change the scroll speed back
                    vp.setScrollDurationFactor(3);
                }
            }
        });

    }

    private void setupCloud(){
        mCloudContainer.setAlpha(0f);
        mCloudContainer.setVisibility(View.VISIBLE);

        mFlyingFolderWidth = mFlyingFolder.getWidth();
        mFlyingFolderHeight = mFlyingFolder.getHeight();

        mFlyingFolder.setRotation(-45);

        mFlyingFolderInitX = mFlyingFolder.getX();
        mFlyingFolderInitY = mFlyingFolder.getY();

        mFlyingFolder.setX(mFlyingFolderInitX + mScreenWidth);
        mFlyingFolder.setY(mFlyingFolderInitY + mFlyingFolderHeight / 2);

    }

    private void pageSelectBehavior(int i){
        //prep buttons for selected page
        pagePrepare(i);

        float highlightScale;

        //deal with previous page
        switch (mCurrentScreen){
            case 1:
                mSelectToolbarButton.setClickable(false);
                if(mSelectToolbarOut){
                    animatePopup(mSelectToolbar, mSelectToolbarInitY, mSelectToolbarHiddenY);
                    flipIcon(mSelectToolbarButton, mSelectToolbarOut);
                    mSelectToolbarOut = false;
                    endPulse(mNextButton, 1f, 1f);
                    mNextButton.setAlpha(1.0f);
                }
                mScaleUpGroup.end();
                endPulse(mSelectToolbarHighlight, 0f, 0f);
                break;

            case 2:
                //if the popup is out, animate it to hidden
                mInfoPopupButton.setClickable(false);
                if(mInfoPopupOut){
                    animatePopup(mInfoPopup, mInfoPopupInitY, mInfoPopupHiddenY);
                    mInfoPopupOut = false;
                    endPulse(mNextButton, 1f, 1f);
                    mNextButton.setAlpha(1.0f);
                }
                mScaleUpGroup.end();
                endPulse(mInfoPopupHighlight, 0f, 0f);
                break;
        }

        //setup new page
        switch (i){
            case 1:
                mSelectToolbarButton.setClickable(true);
                startPulse(mSelectToolbarHighlight, 0f, 0.8f, 0f);
                break;

            case 2:
                mInfoPopupButton.setClickable(true);
                startPulse(mInfoPopupHighlight, 0f, 0.8f, 0f);
                break;

        }

        if(i == 3){
            if(!mFolderAnimated){
                animateWings(mFlyingFolder);
                animateFlyingFolder();
            }
        }

        //update the page indicator at the bottom of the screen
        mSimplePageIndicator.updateIndicator(mCurrentScreen, i);

        mCurrentScreen = i;
    }

    private void pageScrollBehavior(int position, float positionOffset, int positionOffsetPixels){

        switch (position){
            case 0:
                mBackground.setX(-positionOffsetPixels/2);
                mBackground.setAlpha(1 - positionOffset);
                mGreenBackground.setAlpha(1 - positionOffset);
                mOobeDevice.setX(rangeMapper(positionOffset, 0, 1, (mScreenWidth * 1.5f), 0));
                break;

            case 1:
                mBackground.setX(mScreenWidth/2);
                mBackground.setAlpha(0);
                mGreenBackground.setAlpha(0);
                mOobeDevice.setX(0);
                mCloudContainer.setAlpha(0);

                if(mFolderAnimated){
                    mFlyingFolder.setX(mScreenWidth * 1.5f);
                }
                break;

            case 2:
                mOobeDevice.setX(rangeMapper(positionOffset, 0, 1, 0, - (mScreenWidth * 1.5f)));
                mCloudContainer.setAlpha(positionOffset);

                if(mFolderAnimated){
                    mFlyingFolder.setX(rangeMapper(positionOffset, 1, 0, mFlyingFolderInitX,
                            mFlyingFolderInitX + (mScreenWidth * 1.5f)));
                }
                break;

            case 3:
                mOobeDevice.setX(- (mScreenWidth * 1.5f));
                mCloudContainer.setAlpha(1);
                if(mFolderAnimated){
                    mFlyingFolder.setX(mFlyingFolderInitX);
                }
                break;
        }
    }

    private void animateFlyingFolder(){
        ObjectAnimator moveX = ObjectAnimator.ofFloat(mFlyingFolder,"x",
                mFlyingFolder.getX(), mFlyingFolderInitX);
        moveX.setInterpolator(new DecelerateInterpolator(1.8f));
        moveX.setDuration(700);

        ObjectAnimator moveY =ObjectAnimator.ofFloat(mFlyingFolder,"y",
                mFlyingFolder.getY(), mFlyingFolderInitY);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setDuration(1000);

        ObjectAnimator rotate = ObjectAnimator.ofFloat(mFlyingFolder,"rotation",-45,0);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setDuration(700);

        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(moveX, moveY, rotate);

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFolderAnimated = true;
            }
        });

        anim.start();

    }

    private void folderFlyAway(){
        ObjectAnimator windUp = ObjectAnimator.ofFloat(mFlyingFolder, "y",
                mFlyingFolderInitY, mFlyingFolderInitY + 100);
        windUp.setDuration(600);
        windUp.setInterpolator(new DecelerateInterpolator(1.8f));

        ObjectAnimator flyOff = ObjectAnimator.ofFloat(mFlyingFolder, "y",
                mFlyingFolderInitY + 100, -mFlyingFolderHeight);
        flyOff.setDuration(250);
        flyOff.setInterpolator(new AccelerateInterpolator(2.2f));

        AnimatorSet anim = new AnimatorSet();
        anim.playSequentially(windUp, flyOff);

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //TODO start up the connect now activity
                finish();
            }
        });

        anim.start();

    }

    private void animateWings(View view){
        if(!(view instanceof ImageView)){
            return;
        }
        final ImageView imageView = (ImageView)view;
        final Drawable drawable = imageView.getDrawable();
        mAvd = (AnimatedVectorDrawable)drawable;

        mHandler.post(repeatWings);

    }

    Runnable repeatWings = new Runnable() {
        @Override
        public void run() {
            mAvd.start();
            mHandler.post(repeatWings);
        }
    };

    private void repeatWings(){
        mAvd.start();
    }

    private void setupOobeDevice(){
        //measure device illustration
        mOobeDeviceCenterX = mOobeDevice.getX();
        mOobeDeviceWidth = mOobeDevice.getWidth();
        mOobeDevice.setX(mScreenWidth * 1.5f);

        //measure select toolbar
        mSelectToolbarInitY = mSelectToolbar.getY();
        mSelectToolbarHeight = mSelectToolbar.getHeight();
        mSelectToolbarHiddenY = mSelectToolbarInitY + mSelectToolbarHeight;
        mSelectToolbar.setY(mSelectToolbarHiddenY);

        //measure info popup
        mInfoPopupInitY = mInfoPopup.getY();
        mInfoPopupHeight = mInfoPopup.getHeight();
        mInfoPopupHiddenY = mInfoPopupInitY + mInfoPopupHeight;
        mInfoPopup.setY(mInfoPopupHiddenY);

        //hide the highlights
        mSelectToolbarHighlight.setScaleX(0f);
        mSelectToolbarHighlight.setScaleY(0f);

        mInfoPopupHighlight.setScaleX(0f);
        mInfoPopupHighlight.setScaleY(0f);

        //set listener for the file select button
        mSelectToolbarButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCurrentScreen == 1 && !mSelectToolbarOut) {
                                animatePopup(mSelectToolbar,
                                        mSelectToolbarHiddenY, mSelectToolbarInitY);
                                flipIcon(mSelectToolbarButton, mSelectToolbarOut);
                                mSelectToolbarOut = true;
                                mScaleUpGroup.end();
                                endPulse(mSelectToolbarHighlight, 0f, 0f);
                                startPulse(mNextButton, 1f, 0.8f, 1f);
                            }
                        }
                    });
                }
                return false;
            }
        });

        //set listener for the info button
        mInfoPopupButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCurrentScreen == 2 && !mInfoPopupOut) {
                                animatePopup(mInfoPopup, mInfoPopupHiddenY, mInfoPopupInitY);
                                mInfoPopupOut = true;
                                mScaleUpGroup.end();
                                endPulse(mInfoPopupHighlight, 0f, 0f);
                                startPulse(mNextButton, 1f, 0.8f, 1f);
                            }
                        }
                    });
                }
                return false;
            }
        });
    }

    private void startPulse(View view, float initScale, float pulseScale, float initAlpha){
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view,"scaleX",initScale,1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view,"scaleY",initScale,1f);
        ObjectAnimator fadeUp = ObjectAnimator.ofFloat(view, "alpha", initAlpha, 1f);
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleUpX, scaleUpY, fadeUp);
        scaleUp.setDuration(200);
        scaleUp.setInterpolator(new DecelerateInterpolator(1.8f));

        ObjectAnimator scalePulseX = ObjectAnimator.ofFloat(view,"scaleX",1f,pulseScale);
        scalePulseX.setRepeatCount(ObjectAnimator.INFINITE);
        scalePulseX.setRepeatMode(ObjectAnimator.REVERSE);
        ObjectAnimator scalePulseY = ObjectAnimator.ofFloat(view,"scaleY",1f,pulseScale);
        scalePulseY.setRepeatCount(ObjectAnimator.INFINITE);
        scalePulseY.setRepeatMode(ObjectAnimator.REVERSE);
        AnimatorSet scalePulse = new AnimatorSet();
        scalePulse.playTogether(scalePulseX, scalePulseY);
        scalePulse.setDuration(PULSE_DURATION);
        scalePulse.setInterpolator(new AccelerateDecelerateInterpolator());

        mScaleUpGroup = new AnimatorSet();
        mScaleUpGroup.playSequentially(scaleUp, scalePulse);

        mScaleUpGroup.start();
    }

    private void endPulse(View view, float postScale, float postAlpha){
        float startScale = view.getScaleX();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view,"scaleX",
                startScale,postScale);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view,"scaleY",
                startScale,postScale);
        ObjectAnimator fadeDown = ObjectAnimator.ofFloat(view, "alpha", 1f, postAlpha);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleDownX, scaleDownY, fadeDown);
        scaleDown.setDuration(200);
        scaleDown.setInterpolator(new AccelerateInterpolator(1.8f));

        scaleDown.start();
    }

    private void animatePopup(View view, float startValue, float endValue){
        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, "y", startValue, endValue);

        moveY.setDuration(300);
        moveY.setInterpolator(new DecelerateInterpolator(2.0f));

        moveY.start();
    }

    Runnable animateBackground = new Runnable() {
        @Override
        public void run() {
            animateBackgroundElement(mBgUpperLeft, ANIM_BG_DURATION,     ANIM_BG_DELAY * 1);
            animateBackgroundElement(mBgUpperRight,ANIM_BG_DURATION,     ANIM_BG_DELAY * 2);
            animateBackgroundElement(mBgLowerLeft,ANIM_BG_DURATION,      ANIM_BG_DELAY * 3);
            animateBackgroundElement(mBgLowerRight,ANIM_BG_DURATION,     ANIM_BG_DELAY * 4);
        }
    };

    Runnable driftBackground = new Runnable() {
        @Override
        public void run() {
            driftAnimation();
        }
    };

    private void flipIcon(View view, final boolean isSelected){
        final ImageView iv = (ImageView)view;

        ObjectAnimator scaleIn = ObjectAnimator.ofFloat(iv, "scaleX", 1.0f, 0.0f);
        scaleIn.setDuration(160);
        scaleIn.setInterpolator(new AccelerateInterpolator(1.6f));

        final ObjectAnimator scaleOut = ObjectAnimator.ofFloat(iv, "scaleX", 0f, 1.0f);
        scaleOut.setDuration(160);
        scaleOut.setInterpolator(new DecelerateInterpolator(1.6f));

        scaleIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Drawable d = iv.getBackground();

                if(!isSelected){
                    d.setTint(getResources().getColor(R.color.icon_selected));
                    iv.setImageResource(R.drawable.ic_check);
                } else {
                    d.setTint(getResources().getColor(R.color.icon_unselected));
                    iv.setImageResource(android.R.color.transparent);
                }

                scaleOut.start();
            }
        });

        scaleIn.start();

    }

    private void driftAnimation(){
        float initX = mBackgroundContainer.getX();
        float targetX = initX - mBackgroundContainer.getWidth()/6;
        ObjectAnimator driftX = ObjectAnimator.ofFloat(mBackgroundContainer,"x",initX,targetX);
        driftX.setDuration(100000);
        driftX.setInterpolator(new DecelerateInterpolator());
        driftX.start();
    }

    private void animateBackgroundElement(View view,long duration, long delay){
        float initX = view.getX();
        float initY = view.getY();
        float offScreenX = 0f;
        float offScreenY = 0f;

        switch (view.getId()){
            case R.id.oobe_bg_upper_left:
                offScreenX = initX - view.getWidth();
                offScreenY = initY - view.getHeight();
                break;
            case R.id.oobe_bg_upper_right:
                offScreenX = initX + view.getWidth();
                offScreenY = initY - view.getHeight();
                break;
            case R.id.oobe_bg_lower_left:
                offScreenX = initX - view.getWidth();
                offScreenY = initY + view.getHeight();
                break;
            case R.id.oobe_bg_lower_right:
                offScreenX = initX + view.getWidth();
                offScreenY = initY + view.getHeight();
                break;
        }

        view.setX(offScreenX);
        view.setY(offScreenY);

        view.setVisibility(View.VISIBLE);

        //animate upper left
        ObjectAnimator animX = ObjectAnimator.ofFloat(view,"x",offScreenX,initX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view,"y",offScreenY,initY);
        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(animX,animY);
        animSetXY.setDuration(duration);
        animSetXY.setStartDelay(delay);
        animSetXY.setInterpolator(new DecelerateInterpolator(2.0f));
        animSetXY.start();

    }

    private void endButton(ImageView b) {
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void pagePrepare(int currentPage) {
        int maxCount = adapter.getCount();
        if (maxCount == currentPage + 1) {
            mNextButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_oobe_finish));
            endButton(mNextButton);
        } else {
            mNextButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_oobe_forward));
            mNextButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int current = vp.getCurrentItem();
                    vp.setCurrentItem(current + 1);
                }
            });
        }
        if (currentPage == 0) {
            mPrevButton.setVisibility(View.INVISIBLE);
        } else {
            mPrevButton.setVisibility(View.VISIBLE);
            mPrevButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int current = vp.getCurrentItem();
                    vp.setCurrentItem(current - 1);
                }
            });
        }
    }

    private float rangeMapper(float source, float minSource, float maxSource,
                              float minTarget, float maxTarget){
        return (source-minSource)/(maxSource-minSource) * (maxTarget-minTarget) + minTarget;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "WelcomeActivity.onDestroy"); //$NON-NLS-1$
        }

        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        if (DEBUG) {
            Log.d(TAG, "SearchActivity.onSaveInstanceState"); //$NON-NLS-1$
        }
        super.onSaveInstanceState(outState);
    }

}

