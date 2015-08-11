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
package com.cyanogenmod.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.DialogHelper;

public class OpenFileProgressDialog implements CustomProgressDialog {
    /**
     * @hide
     */
    final Context mContext;
    /**
     * @hide
     */
    final AlertDialog mDialog;
    /**
     * @hide
     */
    OnCancelListener mOnCancelListener;

    /**
     * Constructor of <code>OpenFileProgressDialog</code>.
     *
     *
     * @param context The current context
     * @param iconId The icon dialog resource identifier
     * @param message The dialog message to display
     * @param color The primary color
     * @param cancellable If the dialog is cancellable
     */
    public OpenFileProgressDialog(Context context, int iconId, String message, int color,
            boolean cancellable) {
        //Save the context
        this.mContext = context;

        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = li.inflate(R.layout.open_file_progress_dialog, null);

        // set colors and icons specific to this dialog
        final ProgressBar progress =
                (ProgressBar)layout.findViewById(R.id.message_progress_dialog_waiting);
        progress.setIndeterminateTintList(ColorStateList.valueOf(color));

        final ImageView icon = (ImageView)layout.findViewById(R.id.message_progress_dialog_icon);
        icon.setImageResource(iconId);
        icon.setColorFilter(color, Mode.SRC_IN);

        final TextView messageView =
                (TextView)layout.findViewById(R.id.open_file_progress_dialog_filename);
        if (!TextUtils.isEmpty(message)) {
            messageView.setText(message);
        }

        this.mDialog = DialogHelper.createDialog(context, layout);
        this.mDialog.setCancelable(cancellable);
        this.mDialog.setCanceledOnTouchOutside(cancellable);
    }

    /**
     * Method that sets the cancel listener.
     *
     * @param onCancelListener The cancel listener
     */
    @Override
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
    }

    /**
     * Method that sets the progress of the action.
     *
     * @param progress The progress of progress of the action
     */
    @Override
    public void setProgress(Spanned progress) {
        // Not implemented
    }

    /**
     * Method that shows the dialog.
     */
    @Override
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * Method that dismiss the dialog.
     */
    @Override
    public void dismiss() {
        this.mDialog.dismiss();
    }
}
