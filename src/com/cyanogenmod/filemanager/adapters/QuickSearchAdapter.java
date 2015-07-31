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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.SearchActivity;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.*;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.COMPRESS;

public class QuickSearchAdapter extends ArrayAdapter<MimeTypeHelper.MimeTypeCategory>
        implements View.OnClickListener {

    static String MIME_TYPE_LOCALIZED_NAMES[];

    static Map<MimeTypeHelper.MimeTypeCategory, Integer> QUICK_SEARCH_ICONS
            = new HashMap<MimeTypeHelper.MimeTypeCategory, Integer>();
    static {
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.IMAGE, R.drawable.ic_category_images);
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.AUDIO, R.drawable.ic_category_audio);
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.VIDEO, R.drawable.ic_category_video);
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.DOCUMENT, R.drawable.ic_category_docs);
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.APP, R.drawable.ic_category_apps);
        QUICK_SEARCH_ICONS.put(MimeTypeHelper.MimeTypeCategory.COMPRESS, R.drawable.ic_category_archives);
    }

    public static final List<MimeTypeHelper.MimeTypeCategory> QUICK_SEARCH_LIST
            = new ArrayList<MimeTypeHelper.MimeTypeCategory>() {
        {
            add(IMAGE);
            add(AUDIO);
            add(VIDEO);
            add(DOCUMENT);
            add(APP);
            add(COMPRESS);
        }
    };

    IconHolder mIconHolder;

    public QuickSearchAdapter(Context context, int resource) {
        super(context, resource);

        MIME_TYPE_LOCALIZED_NAMES = MimeTypeHelper.MimeTypeCategory.getDefinedLocalizedNames(context);
        mIconHolder = new IconHolder(context, false);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = (convertView == null) ? LayoutInflater.from(getContext()).inflate(
                R.layout.quick_search_item, parent, false) : convertView;

        MimeTypeHelper.MimeTypeCategory item = getItem(position);
        String typeTitle = MIME_TYPE_LOCALIZED_NAMES[item.ordinal()];
        TextView typeTitleTV = (TextView) convertView
                .findViewById(R.id.navigation_view_item_name);
        ImageView typeIconIV = (ImageView) convertView
                .findViewById(R.id.navigation_view_item_icon);

        typeTitleTV.setText(typeTitle);
        convertView.setOnClickListener(this);
        convertView.setTag(position);

        setFileIcon(typeIconIV, QUICK_SEARCH_ICONS.get(item), null);

        return convertView;
    }

    private void setFileIcon(ImageView view, final int iconId, FileSystemObject fso) {
        // Use iconholder to check for thumbnail
        final IconHolder.ICallback callback = new IconHolder.ICallback() {
            @Override
            public void onLoaded(ImageView imageView, Drawable icon) {
                if (icon == null) {
                    // Icon holder didn't have anything at the moment, set default.
                    int colorId = MimeTypeHelper.getIconColorFromIconId(getContext(), iconId);
                    setIcon(imageView, getContext().getResources().getDrawable(iconId),
                            R.color.navigation_view_icon_unselected, R.drawable.ic_icon_background,
                            getContext().getResources().getColor(colorId));

                }
            }
        };
        mIconHolder.loadDrawable(view, fso, iconId, callback);
    }

    // Set drawable as icon
    private void setIcon(ImageView view, Drawable iconDrawable, int iconColorId, int backgroundId,
                         int backgroundColor) {

        StateListDrawable stateListDrawable = new StateListDrawable();
        addUnselected(getContext().getResources(), stateListDrawable, iconDrawable, iconColorId);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{getContext().getResources().getColor(R.color.navigation_view_icon_selected),
                        backgroundColor});

        view.setBackgroundResource(backgroundId);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    // default
    private void addUnselected(Resources res, StateListDrawable drawable, Drawable iconDrawable,
                               int colorId) {
        iconDrawable.setTint(res.getColor(colorId));
        drawable.addState(new int[0], iconDrawable);
    }

    @Override
    public void onClick(View view) {
        Integer position = (Integer) view.getTag();

        Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY, FileHelper.ROOT_DIRECTORY);
        intent.putExtra(SearchManager.QUERY, "*"); // Use wild-card '*'

        ArrayList<MimeTypeHelper.MimeTypeCategory> searchCategories = new ArrayList<MimeTypeHelper.MimeTypeCategory>();
        MimeTypeHelper.MimeTypeCategory selectedCategory = QUICK_SEARCH_LIST.get(position);
        searchCategories.add(selectedCategory);
        // a one off case where we implicitly want to also search for TEXT mimetypes when the
        // DOCUMENTS category is selected
        if (selectedCategory == MimeTypeHelper.MimeTypeCategory.DOCUMENT) {
            searchCategories.add(
                    MimeTypeHelper.MimeTypeCategory.TEXT);
        }
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MIMETYPE, searchCategories);

        getContext().startActivity(intent);
    }
}