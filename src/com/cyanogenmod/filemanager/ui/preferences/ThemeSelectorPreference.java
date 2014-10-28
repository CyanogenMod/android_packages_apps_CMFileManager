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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.preferences.ThemeRoulette.OnThemeScrollSelectionListener;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A preference implementation for the selection of a theme.
 */
public class ThemeSelectorPreference extends Preference implements OnClickListener {

    private static final String TAG = "ThemeSelectorPreference"; //$NON-NLS-1$

    /**
     * @hide
     */
    final AsyncTask<Void, Integer, List<Theme>> mThemeTask =
                            new AsyncTask<Void, Integer, List<Theme>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        protected List<Theme> doInBackground(Void... params) {
            List<Theme> themes = new ArrayList<Theme>();
            themes.addAll(ThemeManager.getAvailableThemes(getContext()));
            return themes;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(List<Theme> result) {
            ThemeSelectorPreference.this.mRoulette.setThemes(
                    result,
                    ThemeManager.getCurrentTheme(getContext()),
                    ThemeSelectorPreference.this.mScrollTo);
            ThemeSelectorPreference.this.mWaiting.setVisibility(View.GONE);
        }
    };


    private View mRootView;
    /**
     * @hide
     */
    Button mButton;
    /**
     * @hide
     */
    ThemeRoulette mRoulette;
    /**
     * @hide
     */
    ProgressBar mWaiting;

    // The visible theme, selected by scrolling. This may not be the current theme.
    /**
     * @hide
     */
    Theme mSelectedTheme;

    /**
     * @hide
     */
    int mScrollTo = -1;

    /**
     * Constructor of <code>ThemeSelectorPreference</code>.
     *
     * @param context The current context
     */
    public ThemeSelectorPreference(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>ThemeSelectorPreference</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ThemeSelectorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>ThemeSelectorPreference</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public ThemeSelectorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the preference
     */
    private void init() {
        setLayoutResource(R.layout.theme_selector_preference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View onCreateView(ViewGroup parent) {
        this.mRootView = super.onCreateView(parent);
        this.mButton = (Button)this.mRootView.findViewById(R.id.theme_button);
        this.mButton.setOnClickListener(this);
        this.mRoulette = (ThemeRoulette)this.mRootView.findViewById(R.id.theme_roulette);
        this.mRoulette.setOnThemeScrollSelectionListener(new OnThemeScrollSelectionListener() {
            @Override
            public void onScrollSelectionStart() {
                ThemeSelectorPreference.this.mButton.setEnabled(false);
            }

            @Override
            public void onScrollSelectionChanged(Theme theme) {
                boolean enabled = ThemeManager.getCurrentTheme(getContext()).compareTo(theme) != 0;
                ThemeSelectorPreference.this.mButton.setEnabled(enabled);
                if (enabled) {
                    ThemeSelectorPreference.this.mSelectedTheme = theme;
                }
            }
        });
        this.mWaiting = (ProgressBar)this.mRootView.findViewById(R.id.theme_waiting);
        return this.mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        view.post(new Runnable() {
            @Override
            public void run() {
                Resources res = getContext().getResources();
                Display display = ((Activity)getContext()).getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                // Set the preference height
                int mh = (int)res.getDimension(R.dimen.theme_max_height);
                int rowHeight = 0;
                if (!AndroidHelper.isTablet(getContext())) {
                    rowHeight = (int)res.getDimension(R.dimen.extra_margin);
                }
                int[] window = new int[2];
                view.getLocationInWindow(window);
                view.getLayoutParams().height =
                        Math.min(mh, size.y - window[1] - rowHeight);

                // The button width
                int minWidth = (int)res.getDimension(R.dimen.themes_min_width_button);
                int w = ThemeSelectorPreference.this.mButton.getWidth();
                ThemeSelectorPreference.this.mButton.setWidth(Math.max(minWidth, w));

                // Now display the progress and load the themes in background
                ThemeSelectorPreference.this.mWaiting.setVisibility(View.VISIBLE);
                ThemeSelectorPreference.this.mThemeTask.execute();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        String newValue = this.mSelectedTheme.getComposedId();
        if (ThemeManager.setCurrentTheme(getContext(), newValue)) {
            if (!persistString(newValue)) {
                Log.w(TAG, "can't save theme preference"); //$NON-NLS-1$
            }
            this.mRoulette.markCurrentVisibleThemeAsDefault();
            callChangeListener(newValue);

            // Now we are in the current theme
            ThemeSelectorPreference.this.mSelectedTheme =
                    ThemeManager.getCurrentTheme(getContext());
            ThemeSelectorPreference.this.mButton.setEnabled(false);

            // Display a confirmation
            DialogHelper.showToast(
                    getContext(), R.string.pref_themes_confirmation, Toast.LENGTH_SHORT);
        } else {
            // The theme was not found
            DialogHelper.showToast(
                    getContext(), R.string.pref_themes_not_found, Toast.LENGTH_SHORT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.mPosition = this.mRoulette.mCurrentThemePosition;
        return myState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        this.mScrollTo = myState.mPosition;
    }

    /**
     * A saved state persistence class
     */
    static class SavedState extends BaseSavedState {
        int mPosition;

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param source The original parcel
         */
        public SavedState(Parcel source) {
            super(source);
            this.mPosition = source.readInt();
        }

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param superState The super state
         */
        public SavedState(Parcelable superState) {
            super(superState);
            this.mPosition = -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mPosition);
        }

        /**
         * A class for create the saved state
         */
        public static final Parcelable.Creator<SavedState> CREATOR =
                                        new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
