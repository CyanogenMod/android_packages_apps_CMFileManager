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

package com.cyanogenmod.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.widgets.DirectoryInlineAutocompleteTextView;
import com.cyanogenmod.filemanager.util.DialogHelper;

import java.io.File;

/**
 * A class that wraps a dialog for showing list of consoles for choosing one.
 * This class lets the user to set the default console.
 */
public class InitialDirectoryDialog implements DialogInterface.OnClickListener {

    /**
     * An interface to communicate events for value changing.
     */
    public interface OnValueChangedListener {
        /**
         * Method invoked when the value of the initial directory was changed.
         *
         * @param newInitialDir The new initial directory
         */
        void onValueChanged(String newInitialDir);
    }

    private static final String TAG = "InitialDirectoryDialog"; //$NON-NLS-1$

    private final Context mContext;
    /**
     * @hide
     */
    final AlertDialog mDialog;
    private final DirectoryInlineAutocompleteTextView mAutocomplete;
    private OnValueChangedListener mOnValueChangedListener;

    /**
     * Constructor of <code>InitialDirectoryDialog</code>.
     *
     * @param context The current context
     */
    public InitialDirectoryDialog(Context context) {
        super();

        //Save the context
        this.mContext = context;

        //Extract current value
        String value = Preferences.getSharedPreferences().getString(
                FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                (String)FileManagerSettings.SETTINGS_INITIAL_DIR.getDefaultValue());

        //Create the layout
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout)li.inflate(R.layout.initial_directory, null);
        final View msgView = layout.findViewById(R.id.initial_directory_info_msg);
        final TextView labelView = (TextView)layout.findViewById(R.id.initial_directory_label);
        this.mAutocomplete =
                (DirectoryInlineAutocompleteTextView)layout.findViewById(
                        R.id.initial_directory_edittext);
        this.mAutocomplete.setOnValidationListener(
                new DirectoryInlineAutocompleteTextView.OnValidationListener() {
            @Override
            public void onVoidValue() {
                msgView.setVisibility(View.GONE);
                //The first invocation is valid. Can be ignore safely
                if (InitialDirectoryDialog.this.mDialog != null) {
                    InitialDirectoryDialog.this.mDialog.getButton(
                            DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            }
            @Override
            public void onValidValue() {
                msgView.setVisibility(View.GONE);
                //The first invocation is valid. Can be ignore safely
                if (InitialDirectoryDialog.this.mDialog != null) {
                    InitialDirectoryDialog.this.mDialog.getButton(
                            DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                }
            }
            @Override
            public void onInvalidValue() {
                msgView.setVisibility(View.VISIBLE);
                //The first invocation is valid. Can be ignore safely
                if (InitialDirectoryDialog.this.mDialog != null) {
                    InitialDirectoryDialog.this.mDialog.getButton(
                            DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
        this.mAutocomplete.setText(value);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, layout, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(context, labelView, "text_color"); //$NON-NLS-1$
        theme.setTextColor(context, (TextView)msgView, "text_color"); //$NON-NLS-1$
        ((TextView)msgView).setCompoundDrawablesWithIntrinsicBounds(
                theme.getDrawable(this.mContext,
                        "filesystem_dialog_warning_drawable"), //$NON-NLS-1$
                null, null, null);
        this.mAutocomplete.applyTheme();

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        R.string.initial_directory_dialog_title,
                                        layout);
        this.mDialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);
    }

    /**
     * Method that set the listener for retrieve value changed events.
     *
     * @param onValueChangedListener The listener for retrieve value changed events
     */
    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.mOnValueChangedListener = onValueChangedListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                done();
                break;
            default:
                break;
        }
    }

    /**
     * Method invoked when the user press ok, or Enter key
     */
    private void done() {
        //Check that the directory is a valid directory
        String newInitialDir = this.mAutocomplete.getText().toString();
        try {
            if (!newInitialDir.endsWith(File.separator)) {
                newInitialDir += File.separator;
            }
            Preferences.savePreference(
                    FileManagerSettings.SETTINGS_INITIAL_DIR, newInitialDir, true);
            if (this.mOnValueChangedListener != null) {
                this.mOnValueChangedListener.onValueChanged(newInitialDir);
            }
        } catch (Throwable ex) {
            Log.e(TAG, "The save initial directory setting operation fails", ex); //$NON-NLS-1$
            DialogHelper.showToast(
                    this.mContext,
                    R.string.initial_directory_error_msg,
                    Toast.LENGTH_LONG);
        }
    }
}
