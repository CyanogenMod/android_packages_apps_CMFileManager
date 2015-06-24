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

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.WelcomeAdapter;
import com.cyanogenmod.filemanager.views.CirclePageIndicator;
import com.cyanogenmod.filemanager.views.PageIndicator;


/**
 * An activity for search files and folders.
 */
public class WelcomeActivity extends Activity {

    private static final String TAG = "WelcomeActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    Button nextButton;
    ViewPager vp;
    WelcomeAdapter adapter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(android.os.Bundle state) {

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (DEBUG) {
            android.util.Log.d(TAG, "WelcomeActivity.onCreate"); //$NON-NLS-1$
        }
        //Set the main layout of the activity
        setContentView(R.layout.welcome);

        adapter = new WelcomeAdapter();
        vp = (ViewPager) findViewById(R.id.intro_pager);
        nextButton = (Button) findViewById(R.id.nextButton);
        PageIndicator indicator = (CirclePageIndicator)findViewById(R.id.pagination);
        Button skipButton = (Button) findViewById(R.id.skipButton);

        endButton(skipButton);
        vp.setAdapter(adapter);
        vp.setOffscreenPageLimit(3);

        indicator.setViewPager(vp);
        pagePrepare(vp.getCurrentItem());

        indicator.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                pagePrepare(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        //Save state
        super.onCreate(state);
    }

    private void endButton(Button b) {
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
            nextButton.setText("Done");
            endButton(nextButton);
        } else {
            nextButton.setText("Next");
            nextButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int current = vp.getCurrentItem();
                    vp.setCurrentItem(current + 1);
                }
            });
        }
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

