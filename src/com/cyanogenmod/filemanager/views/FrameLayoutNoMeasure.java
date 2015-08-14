package com.cyanogenmod.filemanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Created by Schoen on 7/19/15.
 */
public class FrameLayoutNoMeasure extends FrameLayout {

    public FrameLayoutNoMeasure(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(0, 0);
    }

}
