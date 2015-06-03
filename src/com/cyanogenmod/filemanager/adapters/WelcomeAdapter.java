package com.cyanogenmod.filemanager.adapters;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import com.cyanogenmod.filemanager.R;
import android.view.ViewGroup;

/**
 * Created by bird on 6/2/15.
 */
public class WelcomeAdapter extends PagerAdapter {

    private final static int COUNT_OF_INTRO_PAGES = 3;

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
