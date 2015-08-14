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

import android.support.v4.view.PagerAdapter;
import android.view.View;
import com.cyanogenmod.filemanager.R;
import android.view.ViewGroup;

public class PickerAdapter extends PagerAdapter {

    public static enum ListType {
        ROOTS_LISTVIEW,
        NAVIGATION_VIEW,
    }

    public Object instantiateItem(ViewGroup collection, int position) {

        int resId = 0;
        ListType type = ListType.values()[position];
        switch (type) {
            case ROOTS_LISTVIEW:
                resId = R.id.roots_listview;
                break;
            case NAVIGATION_VIEW:
                resId = R.id.navigation_view;
                break;
        }
        return collection.findViewById(resId);
    }

    @Override
    public int getCount() {
        return ListType.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == ((View) o);
    }
}
