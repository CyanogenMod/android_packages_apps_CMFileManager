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

package com.cyanogenmod.filemanager.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;

public class DetailsActionAdapter extends ArrayAdapter<DetailsActionAdapter.FileDetailAction>
        implements View.OnClickListener {

    public DetailsActionAdapter(Context context, int resource, FileSystemObject fileSystemObject) {
        super(context, resource);
        add(new FileDetailAction(FileDetailAction.ACTION_SHARE));
        add(new FileDetailAction(FileDetailAction.ACTION_SHARE_LINK));
        add(new FileDetailAction(FileDetailAction.ACTION_FAVORITE));
        add(new FileDetailAction(FileDetailAction.ACTION_RENAME));
        add(new FileDetailAction(FileDetailAction.ACTION_DELETE));
        add(new FileDetailAction(FileDetailAction.ACTION_BACKUP));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = (convertView == null) ? LayoutInflater.from(getContext()).inflate(
                R.layout.details_action_item, parent, false) : convertView;
        FileDetailAction action = getItem(position);
        ImageView icon = (ImageView) convertView
                .findViewById(R.id.details_action_item_icon);
        icon.setImageResource(action.getIconResId());
        icon.setImageTintList(ColorStateList.valueOf(Color.argb(144, 00, 00, 00)));

        TextView textView = (TextView) convertView
                .findViewById(R.id.details_action_item_name);
        textView.setText(action.getStringResId());

        convertView.setOnClickListener(this);
        convertView.setTag(position);

        return convertView;
    }

    @Override
    public void onClick(View view) {

    }

    public static class FileDetailAction {
        public static final int ACTION_SHARE = 1;
        public static final int ACTION_SHARE_LINK = 2;
        public static final int ACTION_FAVORITE = 3;
        public static final int ACTION_RENAME = 4;
        public static final int ACTION_DELETE = 5;
        public static final int ACTION_BACKUP = 6;

        private final int mAction;

        private FileDetailAction(int action) {
            mAction = action;
        }

        private int getIconResId() {
           switch (mAction) {
               case ACTION_SHARE:
                   return R.drawable.ic_share;
               case ACTION_SHARE_LINK:
                   return R.drawable.ic_share_link;
               case ACTION_FAVORITE:
                   return R.drawable.ic_favorite_off;
               case ACTION_RENAME:
                   return R.drawable.ic_rename;
               case ACTION_DELETE:
                   return R.drawable.ic_delete;
               case ACTION_BACKUP:
                   return R.drawable.ic_backup;
               default:
                   throw new IllegalArgumentException("Unknown action: " + mAction);
           }
        }

        private int getStringResId() {
            switch (mAction) {
                case ACTION_SHARE:
                    return R.string.file_detail_share;
                case ACTION_SHARE_LINK:
                    return R.string.file_detail_share_link;
                case ACTION_FAVORITE:
                    return R.string.file_detail_favorite;
                case ACTION_RENAME:
                    return R.string.actions_menu_rename;
                case ACTION_DELETE:
                    return R.string.actions_menu_delete;
                case ACTION_BACKUP:
                    return R.string.file_detail_backup;
                default:
                    throw new IllegalArgumentException("Unknown action: " + mAction);
            }
        }


    }
}