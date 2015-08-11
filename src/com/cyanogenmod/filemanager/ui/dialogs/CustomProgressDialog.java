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
import android.content.DialogInterface;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.DialogHelper;

/**
 * A class that wraps a dialog for showing a progress with text message (non graphical).
 */
public interface CustomProgressDialog {

    /**
     * A class for listen program cancellation events.
     */
    interface OnCancelListener {
        /**
         * Fires when a cancel were requested.
         *
         *  @return boolean If the cancel can be done
         */
        boolean onCancel();
    }

    /**
     * Method that sets the cancel listener.
     *
     * @param onCancelListener The cancel listener
     */
    public void setOnCancelListener(OnCancelListener onCancelListener);

    /**
     * Method that sets the progress of the action.
     *
     * @param progress The progress of progress of the action
     */
    public void setProgress(Spanned progress);

    /**
     * Method that shows the dialog.
     */
    public void show();

    /**
     * Method that dismiss the dialog.
     */
    public void dismiss();
}
