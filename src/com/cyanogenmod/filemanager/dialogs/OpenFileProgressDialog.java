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
package com.cyanogenmod.filemanager.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.DialogHelper;

public class OpenFileProgressDialog {
    public OpenFileProgressDialog(Context context, View layout, String name, int iconId,
            int colorId) {
        int color = context.getResources().getColor(colorId);

        // set colors and icons specific to this dialog
        final ProgressBar progress =
                (ProgressBar)layout.findViewById(R.id.message_progress_dialog_waiting);
        progress.setIndeterminateTintList(ColorStateList.valueOf(color));

        final ImageView icon = (ImageView)layout.findViewById(R.id.message_progress_dialog_icon);
        icon.setImageResource(iconId);
        icon.setColorFilter(color, Mode.SRC_IN);

        final TextView message =
                (TextView)layout.findViewById(R.id.open_file_progress_dialog_filename);
        message.setText(name);
    }

    public static AlertDialog createOpenFileProgressDialog(Context context, String name, int iconId,
            int colorId) {
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = li.inflate(R.layout.open_file_progress_dialog, null);
        final OpenFileProgressDialog progressDialog =
                new OpenFileProgressDialog(context, layout, name, iconId, colorId);
        return DialogHelper.createDialog(context, layout);
    }
}
