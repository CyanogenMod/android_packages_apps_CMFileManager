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

public class WelcomeAdapter extends PagerAdapter {

    private final static int COUNT_OF_INTRO_PAGES = 4;

    public Object instantiateItem(ViewGroup collection, int position) {

        int resId = 0;
        switch (position) {
            case 0:
                resId = R.id.itemOne;
                break;
            case 1:
                resId = R.id.itemTwo;
                break;
            case 2:
                resId = R.id.itemThree;
                break;
            case 3:
                resId = R.id.itemFour;
        }
        return collection.findViewById(resId);
    }

    @Override
    public int getCount() {
        return COUNT_OF_INTRO_PAGES;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == ((View) o);
    }
}
