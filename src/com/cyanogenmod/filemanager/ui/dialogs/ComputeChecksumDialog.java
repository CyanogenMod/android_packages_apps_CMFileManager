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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultExecutable;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
/**
 * A class that wraps a dialog for computing the checksums of a {@link FileSystemObject}
 */
public class ComputeChecksumDialog implements
    DialogInterface.OnClickListener, View.OnClickListener, AsyncResultListener {

    /**
     * @hide
     */
    final Context mContext;
    /**
     * @hide
     */
    final FileSystemObject mFso;
    private final Handler mHandler;
    /**
     * @hide
     */
    final AlertDialog mDialog;

    // For cancel the operation
    /**
     * @hide
     */
    AsyncResultExecutable mCmd;
    /**
     * @hide
     */
    boolean mFinished;

    /**
     * @hide
     */
    EditText[] mChecksums = new EditText[2];

    /**
     * @hide
     */
    int mComputeStatus;

    private final ClipboardManager mClipboardMgr;

    /**
     * Constructor of <code>ComputeChecksumDialog</code>.
     *
     * @param context The current context
     * @param fso The file system object to execute
     */
    public ComputeChecksumDialog(final Context context, final FileSystemObject fso) {
        super();

        // Save properties
        this.mContext = context;
        this.mFso = fso;
        this.mHandler = new Handler();
        this.mComputeStatus = 0;

        this.mClipboardMgr =
                (ClipboardManager)this.mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        //Create the layout
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup layout = (ViewGroup)li.inflate(R.layout.compute_checksum_dialog, null);
        TextView tvFileName = (TextView)layout.findViewById(R.id.checksum_filename);
        tvFileName.setText(fso.getFullPath());
        this.mChecksums[0] = (EditText)layout.findViewById(R.id.checksum_md5);
        this.mChecksums[1] = (EditText)layout.findViewById(R.id.checksum_sha1);
        View btMD5 = layout.findViewById(R.id.bt_md5_clipboard);
        btMD5.setOnClickListener(this);
        View btSHA1 = layout.findViewById(R.id.bt_sha1_clipboard);
        btSHA1.setOnClickListener(this);

        // Apply the theme
        applyTheme(context, layout);

        //Create the dialog
        String title = context.getString(R.string.compute_checksum_title);
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        title,
                                        layout);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(android.R.string.ok), this);

        // Start checksum compute
        try {
            this.mCmd = CommandHelper.checksum(context, fso.getFullPath(), this, null);
        } catch (Exception e) {
            ExceptionUtil.translateException(context, e);
        }
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * Method that dismiss the dialog.
     */
    public void dismiss() {
        this.mDialog.dismiss();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                // Cancel the program?
                try {
                    if (this.mCmd != null && !this.mFinished) {
                        if (this.mCmd.isCancellable() && !this.mCmd.isCancelled()) {
                            this.mCmd.cancel();
                        }
                    }
                } catch (Exception e) {/**NON BLOCK**/}
                this.mDialog.dismiss();
                break;

            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        String digest = ""; //$NON-NLS-1$
        String label = ""; //$NON-NLS-1$
        switch (v.getId()) {
            case R.id.bt_md5_clipboard:
                digest = this.mChecksums[0].getText().toString();
                label = String.format("MD5 Checksum - %s", this.mFso.getFullPath()); //$NON-NLS-1$
                break;
            case R.id.bt_sha1_clipboard:
                digest = this.mChecksums[1].getText().toString();
                label = String.format("SHA-1 Checksum - %s", this.mFso.getFullPath()); //$NON-NLS-1$
                break;

            default:
                break;
        }

        // Copy text to clipboard
        if (this.mClipboardMgr != null) {
            ClipData clip =ClipData.newPlainText(label, digest);
            this.mClipboardMgr.setPrimaryClip(clip);
            DialogHelper.showToast(this.mContext, R.string.copy_text_msg, Toast.LENGTH_SHORT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncStart() {
        /** NON BLOCK **/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncEnd(boolean cancelled) {
        /** NON BLOCK **/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncExitCode(int exitCode) {
        if (exitCode != 0) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int cc = ComputeChecksumDialog.this.mChecksums.length;
                    for (int i = ComputeChecksumDialog.this.mComputeStatus; i < cc; i++) {
                        ComputeChecksumDialog.this.mChecksums[i].setText(R.string.error_message);
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPartialResult(final Object result) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                setChecksum(String.valueOf(result));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(Exception cause) {
        ExceptionUtil.translateException(this.mContext, cause, false, false);
    }

    /**
     * Method that attach the checksum result to the view
     *
     * @param digest The digest value
     * @hide
     */
    synchronized void setChecksum(String digest) {
        this.mChecksums[this.mComputeStatus].setText(digest);
        this.mComputeStatus++;
    }

    /**
     * Method that applies the current theme to the dialog
     *
     * @param ctx The current context
     * @param root The root view
     */
    private void applyTheme(Context ctx, ViewGroup root) {
        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(ctx);
        theme.setBackgroundDrawable(ctx, root, "background_drawable"); //$NON-NLS-1$
        View v = root.findViewById(R.id.checksum_filename_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.checksum_filename);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.checksum_md5_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        theme.setBackgroundColor(ctx, this.mChecksums[0], "console_bg_color"); //$NON-NLS-1$
        theme.setTextColor(ctx, this.mChecksums[0], "console_fg_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.checksum_sha1_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        theme.setBackgroundColor(ctx, this.mChecksums[1], "console_bg_color"); //$NON-NLS-1$
        theme.setTextColor(ctx, this.mChecksums[1], "console_fg_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.bt_md5_clipboard);
        theme.setImageDrawable(ctx, (ImageView)v, "ic_copy_drawable"); //$NON-NLS-1$
        v = root.findViewById(R.id.bt_sha1_clipboard);
        theme.setImageDrawable(ctx, (ImageView)v, "ic_copy_drawable"); //$NON-NLS-1$
    }
}
