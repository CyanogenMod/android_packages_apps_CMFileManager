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

package com.cyanogenmod.filemanager.ui.policy;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.dialogs.ComputeChecksumDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FsoPropertiesDialog;
import com.cyanogenmod.filemanager.util.DialogHelper;

/**
 * A class with the convenience methods for resolve the display of info actions
 */
public final class InfoActionPolicy extends ActionsPolicy {

    /**
     * Method that show a {@link Toast} with the content description of a {@link FileSystemObject}.
     *
     * @param ctx The current context
     * @param fso The file system object
     */
    public static void showContentDescription(final Context ctx, final FileSystemObject fso) {
        String contentDescription = fso.getFullPath();
        DialogHelper.showToast(ctx, contentDescription, Toast.LENGTH_SHORT);
    }

    /**
     * Method that show a new dialog for show {@link FileSystemObject} properties.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param onRequestRefreshListener The listener for request a refresh after properties
     * of the {@link FileSystemObject} were changed (optional)
     */
    public static void showPropertiesDialog(
            final Context ctx, final FileSystemObject fso,
            final OnRequestRefreshListener onRequestRefreshListener) {
        //Show a the filesystem info dialog
        final FsoPropertiesDialog dialog = new FsoPropertiesDialog(ctx, fso);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dlg) {
                // Any change?
                if (dialog.isHasChanged()) {
                    if (onRequestRefreshListener != null) {
                        onRequestRefreshListener.onRequestRefresh(dialog.getFso(), false);
                    }
                }
            }
        });
        dialog.show();
    }

    /**
     * Method that show a new dialog for compute checksum of a {@link FileSystemObject}.
     *
     * @param ctx The current context
     * @param fso The file system object
     * of the {@link FileSystemObject} were changed (optional)
     */
    public static void showComputeChecksumDialog(
            final Context ctx, final FileSystemObject fso) {
        //Show a the filesystem info dialog
        final ComputeChecksumDialog dialog = new ComputeChecksumDialog(ctx, fso);
        dialog.show();
    }
}