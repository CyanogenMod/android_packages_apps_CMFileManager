/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.console.secure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.DialogHelper;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.KeyPromptingCancelledException;
import de.schlichtherle.truezip.key.KeyPromptingInterruptedException;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.UnknownKeyException;

/**
 * A class that remembers all the secure storage
 */
public class SecureStorageKeyPromptDialog
    implements de.schlichtherle.truezip.key.PromptingKeyProvider.View<AesCipherParameters> {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final int MSG_REQUEST_UNLOCK_DIALOG = 1;

    private static boolean sResetInProgress;
    private static boolean sDeleteInProgress;
    private static transient AesCipherParameters sOldUnlockKey = null;
    private static transient AesCipherParameters sUnlockKey = null;
    private static transient AesCipherParameters sOldUnlockKeyTemp = null;
    private static transient AesCipherParameters sUnlockKeyTemp = null;
    private static final Object WAIT_SYNC = new Object();

    /**
     * An activity that simulates a dialog over the activity that requested the key prompt.
     */
    public static class SecureStorageKeyPromptActivity extends Activity
            implements OnClickListener, TextWatcher {

        private AlertDialog mDialog;

        private TextView mMessage;
        private EditText mOldKey;
        private EditText mKey;
        private EditText mRepeatKey;
        private TextView mValidationMsg;
        private Button mUnlock;

        private boolean mNewStorage;
        private boolean mResetPassword;
        private boolean mDeleteStorage;

        AesCipherParameters mOldKeyParams;
        AesCipherParameters mKeyParams;

        @Override
        @SuppressWarnings("deprecation")
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Check with java.io.File instead of TFile because TFile#exists() will
            // check for password key, which is currently locked
            mNewStorage = !SecureConsole.getSecureStorageRoot().getFile().exists();
            mResetPassword = sResetInProgress;
            mDeleteStorage = sDeleteInProgress;

            // Set the theme before setContentView
            Theme theme = ThemeManager.getCurrentTheme(this);
            theme.setBaseTheme(this, true);

            // Load the dialog's custom layout
            ViewGroup v = (ViewGroup) LayoutInflater.from(this).inflate(
                    R.layout.unlock_dialog_message, null);
            mMessage = (TextView) v.findViewById(R.id.unlock_dialog_message);
            mOldKey = (EditText) v.findViewById(R.id.unlock_old_password);
            mOldKey.addTextChangedListener(this);
            mKey = (EditText) v.findViewById(R.id.unlock_password);
            mKey.addTextChangedListener(this);
            mRepeatKey = (EditText) v.findViewById(R.id.unlock_repeat);
            mRepeatKey.addTextChangedListener(this);
            View oldPasswordLayout = v.findViewById(R.id.unlock_old_password_layout);
            View repeatLayout = v.findViewById(R.id.unlock_repeat_layout);
            mValidationMsg = (TextView) v.findViewById(R.id.unlock_validation_msg);

            // Load resources
            int messageResourceId = R.string.secure_storage_unlock_key_prompt_msg;
            int positiveButtonLabelResourceId = R.string.secure_storage_unlock_button;
            String title = getString(R.string.secure_storage_unlock_title);
            if (mNewStorage) {
                positiveButtonLabelResourceId = R.string.secure_storage_create_button;
                title = getString(R.string.secure_storage_create_title);
                messageResourceId = R.string.secure_storage_unlock_key_new_msg;
            } else if (mResetPassword) {
                positiveButtonLabelResourceId = R.string.secure_storage_reset_button;
                title = getString(R.string.secure_storage_reset_title);
                messageResourceId = R.string.secure_storage_unlock_key_reset_msg;
                TextView passwordLabel = (TextView) v.findViewById(R.id.unlock_password_title);
                passwordLabel.setText(R.string.secure_storage_unlock_new_key_title);
            } else if (mDeleteStorage) {
                positiveButtonLabelResourceId = R.string.secure_storage_delete_button;
                title = getString(R.string.secure_storage_delete_title);
                messageResourceId = R.string.secure_storage_unlock_key_delete_msg;
            }

            // Set the message according to the storage creation status
            mMessage.setText(messageResourceId);
            repeatLayout.setVisibility(mNewStorage || mResetPassword ? View.VISIBLE : View.GONE);
            oldPasswordLayout.setVisibility(mResetPassword ? View.VISIBLE : View.GONE);

            // Set validation msg
            mValidationMsg.setText(getString(R.string.secure_storage_unlock_validation_length,
                    MIN_PASSWORD_LENGTH));
            mValidationMsg.setVisibility(View.VISIBLE);

            // Create the dialog
            mDialog = DialogHelper.createTwoButtonsDialog(this,
                    positiveButtonLabelResourceId, R.string.cancel,
                    theme.getResourceId(this,"ic_secure_drawable"), title, v, this);
            mDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mDialog.dismiss();
                    finish();

                    // Unlock the wait
                    synchronized (WAIT_SYNC) {
                        WAIT_SYNC.notify();
                    }
                }
            });
            mDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    sUnlockKeyTemp = null;
                    mDialog.cancel();
                    finish();

                    // Unlock the wait
                    synchronized (WAIT_SYNC) {
                        WAIT_SYNC.notify();
                    }
                }
            });
            mDialog.setCanceledOnTouchOutside(false);

            // Apply the theme to the custom view of the dialog
            applyTheme(this, v);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();

            DialogHelper.delegateDialogShow(this, mDialog);
            mUnlock = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mUnlock.setEnabled(false);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Create the AES parameter and set to the prompting view
                    if (mResetPassword) {
                        AesCipherParameters params = new AesCipherParameters();
                        params.setPassword(mOldKey.getText().toString().toCharArray());
                        params.setKeyStrength(KeyStrength.BITS_128);
                        sOldUnlockKeyTemp = params;
                    }
                    AesCipherParameters params = new AesCipherParameters();
                    params.setPassword(mKey.getText().toString().toCharArray());
                    params.setKeyStrength(KeyStrength.BITS_128);
                    sUnlockKeyTemp = params;

                    // We ended with this dialog
                    dialog.dismiss();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // User had cancelled the dialog
                    dialog.cancel();

                    break;

                default:
                    break;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Ignore
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Ignore
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Validations:
            //  * Key must be MIN_PASSWORD_LENGTH characters or more
            //  * Repeat == Key
            String oldkey = mOldKey.getText().toString();
            String key = mKey.getText().toString();
            String repeatKey = mRepeatKey.getText().toString();
            boolean validLength = key.length() >= MIN_PASSWORD_LENGTH &&
                    (!mResetPassword || (mResetPassword && oldkey.length() >= MIN_PASSWORD_LENGTH));
            boolean validEquals = key.equals(repeatKey);
            boolean valid = validLength && ((mNewStorage && validEquals) || !mNewStorage);
            mUnlock.setEnabled(valid);

            if (!validLength) {
                mValidationMsg.setText(getString(R.string.secure_storage_unlock_validation_length,
                        MIN_PASSWORD_LENGTH));
                mValidationMsg.setVisibility(View.VISIBLE);
            } else if (mNewStorage && !validEquals) {
                mValidationMsg.setText(R.string.secure_storage_unlock_validation_equals);
                mValidationMsg.setVisibility(View.VISIBLE);
            } else {
                mValidationMsg.setVisibility(View.INVISIBLE);
            }
        }

        private void applyTheme(Context ctx, ViewGroup root) {
            // Apply the current theme
            Theme theme = ThemeManager.getCurrentTheme(ctx);
            theme.setBackgroundDrawable(ctx, root, "background_drawable");
            theme.setTextColor(ctx, mMessage, "text_color");
            theme.setTextColor(ctx, mOldKey, "text_color");
            theme.setTextColor(ctx, (TextView) root.findViewById(R.id.unlock_old_password_title),
                    "text_color");
            theme.setTextColor(ctx, mKey, "text_color");
            theme.setTextColor(ctx, (TextView) root.findViewById(R.id.unlock_password_title),
                    "text_color");
            theme.setTextColor(ctx, mRepeatKey, "text_color");
            theme.setTextColor(ctx, (TextView) root.findViewById(R.id.unlock_repeat_title),
                    "text_color");
            theme.setTextColor(ctx, mValidationMsg, "text_color");
            mValidationMsg.setCompoundDrawablesWithIntrinsicBounds(
                    theme.getDrawable(ctx, "filesystem_dialog_warning_drawable"), //$NON-NLS-1$
                    null, null, null);
        }
    }

    SecureStorageKeyPromptDialog() {
        super();
        sResetInProgress = false;
        sDeleteInProgress = false;
        sOldUnlockKey = null;
        sUnlockKey = null;
    }

    @Override
    public void promptWriteKey(Controller<AesCipherParameters> controller)
            throws UnknownKeyException {
        controller.setKey(getOrPromptForKey(false));
        if (sResetInProgress) {
            // Not needed any more. Reads are now done with new key
            sOldUnlockKey = null;
            sResetInProgress = false;
        }
    }

    @Override
    public void promptReadKey(Controller<AesCipherParameters> controller, boolean invalid)
            throws UnknownKeyException {
        if (!sResetInProgress && invalid) {
            sUnlockKey = null;
        }
        controller.setKey(getOrPromptForKey(true));
    }

    /**
     * {@hide}
     */
    void umount() {
        // Discard current keys
        sResetInProgress = false;
        sDeleteInProgress = false;
        sOldUnlockKey = null;
        sUnlockKey = null;
    }

    /**
     * {@hide}
     */
    void reset() {
        // Discard current keys
        sResetInProgress = true;
        sDeleteInProgress = false;
        sOldUnlockKey = null;
        sUnlockKey = null;
    }

    /**
     * {@hide}
     */
    void delete() {
        sDeleteInProgress = true;
        sResetInProgress = false;
        sOldUnlockKey = null;
    }

    /**
     * Method that return or prompt the user for the secure storage key
     *
     * @param read If should return the read or write key
     * @return AesCipherParameters The AES cipher parameters
     */
    private static synchronized AesCipherParameters getOrPromptForKey(boolean read)
            throws UnknownKeyException {
        // Check if we have a cached key
        if (read && sResetInProgress && sOldUnlockKey != null) {
            return sOldUnlockKey;
        }
        if (sUnlockKey != null) {
            return sUnlockKey;
        }

        // Need to prompt the user for the secure storage key, so we open a overlay activity
        // to show the prompt dialog
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                Context ctx = FileManagerApplication.getInstance();
                Intent intent = new Intent(ctx, SecureStorageKeyPromptActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        };
        handler.sendEmptyMessage(MSG_REQUEST_UNLOCK_DIALOG);

        // Wait for the response
        synchronized (WAIT_SYNC) {
            try {
                WAIT_SYNC.wait();
            } catch (InterruptedException ex) {
                throw new KeyPromptingInterruptedException(ex);
            }
        }

        // Request for authentication is done. We need to exit from delete status
        sDeleteInProgress = false;

        // Check if the user cancelled the dialog
        if (sUnlockKeyTemp == null) {
            throw new KeyPromptingCancelledException();
        }

        // Move temporary params to real params
        sUnlockKey = sUnlockKeyTemp;
        sOldUnlockKey = sOldUnlockKeyTemp;

        AesCipherParameters key = sUnlockKey;
        if (sResetInProgress && read) {
            key = sOldUnlockKey;
        }
        return key;
    }
}
