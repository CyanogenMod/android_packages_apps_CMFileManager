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

import afzkl.development.mColorPicker.views.ColorDialogView;
import afzkl.development.mColorPicker.views.ColorPanelView;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import com.cyanogenmod.filemanager.R;

/**
 * A {@link Preference} that allow to select/pick a color in a new window dialog.
 */
public class ColorPickerPreference extends DialogPreference {

    private ColorPanelView mColorPicker;
    private int mColor;

    private ColorDialogView mColorDlg;

    /**
     * Constructor of <code>ColorPickerPreference</code>
     *
     * @param context The current context
     */
    public ColorPickerPreference(Context context) {
        this(context, null);
    }

    /**
     * Constructor of <code>ColorPickerPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the preference.
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.color_picker_pref_item);
    }

    /**
     * Returns the color of the picker.
     *
     * @return The color of the picker.
     */
    public int getColor() {
        return this.mColor;
    }

    /**
     * Sets the color of the picker and saves it to the {@link SharedPreferences}.
     *
     * @param color The new color.
     */
    public void setColor(int color) {
        // Always persist/notify the first time; don't assume the field's default of false.
        final boolean changed = this.mColor != color;
        if (changed) {
            this.mColor = color;
            // when called from onSetInitialValue the view is still not set
            if (this.mColorPicker != null) {
                this.mColorPicker.setColor(color);
            }
            persistInt(color);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Integer.valueOf(a.getColor(index, 0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setColor(restoreValue ? getPersistedInt(0) : ((Integer)defaultValue).intValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        // Configure the dialog
        this.mColorDlg = new ColorDialogView(getContext());
        this.mColorDlg.setColor(this.mColor);
        this.mColorDlg.showAlphaSlider(true);
        this.mColorDlg.setAlphaSliderText(
                getContext().getString(R.string.color_picker_alpha_slider_text));
        this.mColorDlg.setCurrentColorText(
                getContext().getString(R.string.color_picker_current_text));
        this.mColorDlg.setNewColorText(
                getContext().getString(R.string.color_picker_new_text));
        this.mColorDlg.setColorLabelText(
                getContext().getString(R.string.color_picker_color));
        builder.setView(this.mColorDlg);

        // The color is selected by the user and confirmed by clicking ok
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void onClick(DialogInterface dialog, int which) {
                int color = ColorPickerPreference.this.mColorDlg.getColor();
                if (callChangeListener(Integer.valueOf(color))) {
                    setColor(color);
                }
                dialog.dismiss();
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View v = view.findViewById(R.id.color_picker);
        if (v != null && v instanceof ColorPanelView) {
            this.mColorPicker = (ColorPanelView)v;
            this.mColorPicker.setColor(this.mColor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.color = getColor();
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
        setColor(myState.color);
    }

    /**
     * A class for managing the instance state of a {@link ColorPickerPreference}.
     */
    static class SavedState extends BaseSavedState {
        int color;

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param source The source
         */
        public SavedState(Parcel source) {
            super(source);
            this.color = source.readInt();
        }

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param superState The parcelable state
         */
        public SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.color);
        }

        /**
         * A class that generates instances of the <code>SavedState</code> class from a Parcel.
         */
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
