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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.util.List;

/**
 * A class that wraps a dialog for input a name for file, folder, ...
 */
public class InputNameDialog
    implements TextWatcher, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    private static final int FILENAME_CHAR_LIMIT = 255;

    private final Context mContext;
    /**
     * @hide
     */
    final AlertDialog mDialog;
    private final TextView mMsg;
    /**
     * @hide
     */
    final EditText mEditText;
    /**
     * @hide
     */
    final List<FileSystemObject> mFiles;
    /**
     * @hide
     */
    final FileSystemObject mFso;
    private final boolean mAllowFsoName;

    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;
    /**
     * @hide
     */
    boolean mCancelled;

    /**
     * Constructor of <code>InputNameDialog</code>.
     *
     * @param context The current context
     * @param files The files of the current directory (used to validate the name)
     * @param dialogTitle The dialog title
     */
    public InputNameDialog(
            final Context context, List<FileSystemObject> files, String dialogTitle) {
        this(context, files, null, false, dialogTitle);
    }

    /**
     * Constructor of <code>InputNameDialog</code>.
     *
     * @param context The current context
     * @param files The files of the current directory (used to validate the name)
     * @param fso The original file system object. null if not needed.
     * @param allowFsoName If allow that the name of the fso will be returned
     * @param dialogTitle The dialog title
     */
    public InputNameDialog(
            final Context context, final List<FileSystemObject> files,
            final FileSystemObject fso, boolean allowFsoName, final String dialogTitle) {
        super();

        //Save the context
        this.mContext = context;

        //Save the files
        this.mFiles = files;
        this.mFso = fso;
        this.mAllowFsoName = allowFsoName;
        this.mCancelled = true;

        //Create the
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.input_name_dialog, null);
        TextView title = (TextView)v.findViewById(R.id.input_name_dialog_label);
        title.setText(R.string.input_name_dialog_label);
        this.mEditText = (EditText)v.findViewById(R.id.input_name_dialog_edit);
        if (this.mFso != null) {
            this.mEditText.setText(this.mFso.getName());
        } else {
            this.mEditText.setText(dialogTitle);
        }
        this.mEditText.selectAll();
        this.mEditText.addTextChangedListener(this);
        this.mMsg = (TextView)v.findViewById(R.id.input_name_dialog_message);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, v, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(context, title, "text_color"); //$NON-NLS-1$
        theme.setTextColor(context, this.mMsg, "text_color"); //$NON-NLS-1$
        this.mMsg.setCompoundDrawablesWithIntrinsicBounds(
                theme.getDrawable(this.mContext,
                        "filesystem_dialog_warning_drawable"), //$NON-NLS-1$
                null, null, null);

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        dialogTitle,
                                        v);
        this.mDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputNameDialog.this.mCancelled = false;
                    }
                });
        this.mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager mgr =
                        (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(InputNameDialog.this.mEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener)null);
        this.mDialog.setOnCancelListener(this);
        this.mDialog.setOnDismissListener(this);

        // Disable accept button, because name is the same as fso
        if (this.mFso != null && !this.mAllowFsoName) {
            this.mEditText.post(new Runnable() {
                @Override
                public void run() {
                    InputNameDialog.this.mDialog.getButton(
                            DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            });
        } else {
            this.mEditText.post(new Runnable() {
                @Override
                public void run() {
                    checkName(InputNameDialog.this.mEditText.getText().toString());
                }
            });
        }
    }

    /**
     * Set a listener to be invoked when the dialog is cancelled.
     * <p>
     * This will only be invoked when the dialog is cancelled, if the creator
     * needs to know when it is dismissed in general, use
     * {@link #setOnDismissListener}.
     *
     * @param onCancelListener The {@link "OnCancelListener"} to use.
     */
    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
    }

    /**
     * Set a listener to be invoked when the dialog is dismissed.
     *
     * @param onDismissListener The {@link "OnDismissListener"} to use.
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.mOnDismissListener = onDismissListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * Method that returns the name that the user inputted.
     *
     * @return String The name that the user inputted
     */
    public String getName() {
        return this.mEditText.getText().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        /**NON BLOCK**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        /**NON BLOCK**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        String name = s.toString().trim();
        checkName(name);
    }

    /**
     * Method that checks the input name
     * @param name
     * @hide
     */
    void checkName(String name) {

        //The name is empty
        if (TextUtils.isEmpty(name)) {
            setMsg(
                InputNameDialog.this.mContext.getString(
                      R.string.input_name_dialog_message_empty_name), false);
            return;
        }

        // Too long
        if (name.length() >= FILENAME_CHAR_LIMIT) {
            setMsg(InputNameDialog.this.mContext.getString(
                    R.string.input_name_dialog_message_invalid_name_length), true);
            return;
        }

        // The path is invalid
        if (name.contains(File.separator)) {
            setMsg(
                InputNameDialog.this.mContext.getString(
                      R.string.input_name_dialog_message_invalid_path_name,
                      File.separator), false);
            return;
        }

        // No allow . or ..
        if (name.compareTo(FileHelper.CURRENT_DIRECTORY) == 0 ||
                name.compareTo(FileHelper.PARENT_DIRECTORY) == 0) {
            setMsg(
                InputNameDialog.this.mContext.getString(
                        R.string.input_name_dialog_message_invalid_name), false);
            return;
        }

        // The same name
        if (this.mFso != null && !this.mAllowFsoName && name.compareTo(this.mFso.getName()) == 0) {
            setMsg(null, false);
            return;
        }

        // Name exists
        if (FileHelper.isNameExists(this.mFiles, name)) {
            setMsg(
                InputNameDialog.this.mContext.getString(
                        R.string.input_name_dialog_message_name_exists), false);
            return;
        }

        //Valid name
        setMsg(null, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!InputNameDialog.this.mCancelled) {
            if (this.mOnDismissListener != null) {
                this.mOnDismissListener.onDismiss(dialog);
            }
            return;
        }
        if (this.mOnCancelListener != null) {
            this.mOnCancelListener.onCancel(dialog);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        if (this.mOnCancelListener != null) {
            this.mOnCancelListener.onCancel(dialog);
        }
    }

    /**
     * Method that shows the alert message with the validation warning.
     *
     * @param msg The message to show
     * @param activate If the positive button must be activate
     */
    private void setMsg(String msg, boolean activate) {
        if (msg == null || msg.length() == 0) {
            this.mMsg.setVisibility(View.GONE);
            this.mMsg.setText(""); //$NON-NLS-1$
        } else {
            this.mMsg.setText(msg);
            this.mMsg.setVisibility(View.VISIBLE);
        }
        this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(activate);
    }

}
