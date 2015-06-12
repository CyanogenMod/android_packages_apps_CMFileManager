package com.cyanogenmod.filemanager.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

/**
 * Use this class when you want a gridview that doesn't scroll and automatically
 * wraps to the height of its contents
 * Concept from:
 * stackoverflow.com/questions/20561663/gridview-show-according-to-actual-height-within-scroll-view
 * Created By user: LongwayTo http://stackoverflow.com/users/3106513/longwayto
 */
public class WrappedGridView extends GridView {
    public WrappedGridView(Context context) {
        super(context);
    }

    public WrappedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrappedGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate entire height by providing a very large height hint.
        // View.MEASURED_SIZE_MASK represents the largest height possible.
        int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = getMeasuredHeight();
    }
}
