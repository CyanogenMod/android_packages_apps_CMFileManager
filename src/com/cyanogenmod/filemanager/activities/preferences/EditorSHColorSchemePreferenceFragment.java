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

package com.cyanogenmod.filemanager.activities.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ash.HighlightColors;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.preferences.ColorPickerPreference;
import com.cyanogenmod.filemanager.util.ExceptionUtil;

/**
 * A class that manages the color scheme of the syntax highlight processor.
 */
public class EditorSHColorSchemePreferenceFragment extends TitlePreferenceFragment {

    private static final String TAG = "EditorSHColorSchemePreferenceFragment"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private static final String KEY_RESET_COLOR_SCHEME = "ash_reset_color_scheme"; //$NON-NLS-1$

    private Preference mResetColorScheme;
    private ColorPickerPreference[] mColorScheme;

    /**
     * @hide
     */
    boolean mLoaded = false;

    private final OnPreferenceChangeListener mOnChangeListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            boolean ret = true;

            String key = preference.getKey();
            if (DEBUG) {
                Log.d(TAG,
                    String.format("New value for %s: %s",  //$NON-NLS-1$
                            key,
                            String.valueOf(newValue)));
            }

            if (isColorSchemePreference(preference)) {
                // Unify the color schemes property. Save the property here
                int color = ((Integer)newValue).intValue();
                try {
                    String colorScheme = toColorSchemeSet(preference, color);
                    Preferences.savePreference(
                            FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME,
                            colorScheme,
                            true);
                } catch (Exception e) {
                    ExceptionUtil.translateException(getActivity(), e);
                }
                ((ColorPickerPreference)preference).setColor(color);

                // Change the key to get notifications of color scheme
                key = FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME.getId();
            }

            // Notify the change (only if fragment is loaded. Default values are loaded
            // while not in loaded mode)
            if (EditorSHColorSchemePreferenceFragment.this.mLoaded && ret) {
                Intent intent = new Intent(FileManagerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(
                        FileManagerSettings.EXTRA_SETTING_CHANGED_KEY, key);
                getActivity().sendBroadcast(intent);
            }

            return ret;
        }
    };

    private final OnPreferenceClickListener mOnClickListener =
            new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            if (KEY_RESET_COLOR_SCHEME.compareTo(key) == 0) {
                loadDefaultColorScheme(true);
            }
            return false;
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(Preferences.SETTINGS_FILENAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_editor_color_scheme);

        // Color scheme (need to resolver color scheme prior to use theme default)
        loadDefaultColorScheme(false);

        // Reset to default theme color scheme
        this.mResetColorScheme = findPreference(KEY_RESET_COLOR_SCHEME);

        // Now the listeners
        this.mResetColorScheme.setOnPreferenceClickListener(this.mOnClickListener);

        // Loaded
        this.mLoaded = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return getString(R.string.pref_syntax_highlight_color_scheme);
    }

    /**
     * Method that loads the default color scheme
     *
     * @param reset Whether the color scheme should be reseted
     * @hide
     */
    void loadDefaultColorScheme(boolean reset) {
        try {
            String defaultValue =
                    (String)FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME.getDefaultValue();
            if (!reset) {
                defaultValue =
                        Preferences.getSharedPreferences().getString(
                            FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME.getId(),
                            defaultValue);
            } else {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME,
                        defaultValue,
                        true);
            }
            int[] colorScheme = toColorShemeArray(defaultValue);
            HighlightColors[] colors = HighlightColors.values();
            int cc = colors.length;
            this.mColorScheme = new ColorPickerPreference[cc];
            for (int i = 0; i < cc; i++) {
                this.mColorScheme[i] = (ColorPickerPreference)findPreference(colors[i].getId());
                setColorScheme(colors[i], colorScheme, i);
                this.mColorScheme[i].setOnPreferenceChangeListener(this.mOnChangeListener);
            }
        } catch (Exception e) {
            ExceptionUtil.translateException(getActivity(), e);
        }
    }

    /**
     * Method that set a color scheme (use setting or theme default)
     *
     * @param color The color reference
     * @param colorScheme The array of colors
     * @param pos The position of the color
     * @hide
     */
    void setColorScheme(HighlightColors color, int[] colorScheme, int pos) {
        try {
            this.mColorScheme[pos].setColor(colorScheme[pos]);
        } catch (Exception e) {
            this.mColorScheme[pos].setColor(
                    ThemeManager.getCurrentTheme(
                            getActivity()).getColor(getActivity(), color.getResId()));
            Log.w(TAG,
                    String.format(
                            "Color scheme value not found for \"%s\"", //$NON-NLS-1$
                            color.getId()));
        }
    }

    /**
     * Method that returns if the preference is part of the color scheme preferences
     *
     * @return boolean Whether preference is part of the color scheme preferences
     * @hide
     */
    static boolean isColorSchemePreference(final Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            return false;
        }
        HighlightColors[] colors = HighlightColors.values();
        int cc = colors.length;
        for (int i = 0; i < cc; i++) {
            if (colors[i].getId().compareTo(key) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that converts the string set of color schemes to an array of colors
     *
     * @param value The string set of color schemes to parse
     * @return int[] Array of colors
     */
    public static int[] toColorShemeArray(String value) {
        if (value == null || value.length() == 0) {
            return new int[]{};
        }
        String[] values = value.split("\\|"); //$NON-NLS-1$
        int[] colors = new int[values.length];
        int cc = colors.length;
        for (int i = 0; i < cc; i++) {
            try {
                colors[i] = Integer.parseInt(values[i]);
            } catch (Exception e) {
                Log.w(TAG,
                        String.format(
                                "Problem parsing color value \"%s\" on position %d", //$NON-NLS-1$
                                values[i], Integer.valueOf(i)));
                colors[i] = 0;
            }
        }
        return colors;
    }

    /**
     * Method that converts all the color scheme preference to one unified preference set
     *
     * @param preference The color scheme preference that was changed
     * @param newValue The new value of the color scheme
     * @return colorScheme The actual color schemes
     * @hide
     */
    String toColorSchemeSet(final Preference preference, final int newValue) {
        int cc = this.mColorScheme.length;
        String[] colorSchemes = new String[cc];
        for (int i = 0; i < cc; i++) {
            String prop = String.valueOf(this.mColorScheme[i].getColor());
            if (this.mColorScheme[i].getKey().compareTo(preference.getKey()) == 0) {
                prop = String.valueOf(newValue);
            }
            colorSchemes[i] = prop;
        }
        return TextUtils.join("|", colorSchemes); //$NON-NLS-1$
    }
}
