package com.cyanogenmod.filemanager.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
//import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.DetailsActionAdapter;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.widgets.FileDetailImageView;
import com.cyanogenmod.filemanager.ui.widgets.MultiShrinkScroller;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.SchedulingUtils;
import com.cyanogenmod.filemanager.util.StorageHelper;
import com.cyanogenmod.filemanager.util.ViewUtil;


public class FileDetailActivity extends Activity {

    private static final String EXTRA_FILE_OBJECT = "file_object";
    public static final int MODE_FULLY_EXPANDED = 4;
    private static final String TAG = "FileDetailActivity";

    private static final String KEY_THEME_COLOR = "theme_color";

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int SCRIM_COLOR = Color.argb(0xC8, 0, 0, 0);

    private FileSystemObject mFileSystemObject;

    private int mExtraMode;
    private int mStatusBarColor;
    private boolean mHasAlreadyBeenOpened;

    private FileDetailImageView mPhotoView;
    private MultiShrinkScroller mScroller;

    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;
    private boolean mIsExitAnimationInProgress;
    private boolean mHasComputedThemeColor;

    private PorterDuffColorFilter mColorFilter;

    final MultiShrinkScroller.MultiShrinkScrollerListener mMultiShrinkScrollerListener
            = new MultiShrinkScroller.MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            mIsExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (mIsEntranceAnimationFinished) {
                mWindowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };


    public static void show(Context context, FileSystemObject fileSystemObject) {
        Intent intent = new Intent(context ,FileDetailActivity.class);
        intent.putExtra(EXTRA_FILE_OBJECT, fileSystemObject);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileSystemObject = (FileSystemObject)getIntent().getSerializableExtra(EXTRA_FILE_OBJECT);
        if (mFileSystemObject == null) {
            throw new IllegalArgumentException("FileSystemObject needed");
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        setContentView(R.layout.file_detail_activity);
        mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);

        mPhotoView = (FileDetailImageView) findViewById(R.id.photo);
        final View transparentView = findViewById(R.id.transparent_view);
        if (mScroller != null) {
            transparentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mScroller.scrollOffBottom();
                }
            });
        }

        final boolean isStorageProvider =
                (!TextUtils.isEmpty(mFileSystemObject.getProviderPrefix()));
        String locationName = null;
        StorageProviderInfo storageProviderInfo = null;
        if (isStorageProvider) {
            StorageApiConsole storageApiConsole =
                    StorageApiConsole.getConsoleForHashCode(Integer
                            .valueOf(StorageHelper
                                    .getStorageVolumeFromPath(mFileSystemObject.getFullPath())));
            if (storageApiConsole != null) {
                storageProviderInfo = storageApiConsole.getStorageProviderInfo();
                locationName = storageProviderInfo.getTitle();
            }
        } else {
            locationName = StorageHelper
                    .getStorageVolumeDescription(this, StorageHelper.getStorageVolumeForPath(
                                    mFileSystemObject.getFullPath()));
        }

        GridView gridview = (GridView) findViewById(R.id.file_detail_actions);
        DetailsActionAdapter detailsActionAdapter =
                new DetailsActionAdapter(this, R.layout.details_action_item, mFileSystemObject);
        gridview.setAdapter(detailsActionAdapter);

        TextView typeValue = (TextView) findViewById(R.id.type_value);
        if (FileHelper.isDirectory(mFileSystemObject)) {
            typeValue.setText(getString(R.string.mime_folder));
        } else {
            typeValue.setText(MimeTypeHelper.getMimeType(this, mFileSystemObject));
        }

        TextView locationValue = (TextView) findViewById(R.id.location_value);
        locationValue.setText(locationName);

        TextView pathValue = (TextView) findViewById(R.id.path_value);
        pathValue.setText(mFileSystemObject.getFullPath());

        TextView sizeValue = (TextView) findViewById(R.id.size_value);
        sizeValue.setText(FileHelper.getHumanReadableSize(mFileSystemObject));

        TextView modifiedValue = (TextView) findViewById(R.id.modified_value);
        modifiedValue.setText(mFileSystemObject.getLastModifiedTime().toString());

        // Allow a shadow to be shown under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle(null);
        // Put a TextView with a known resource id into the ActionBar. This allows us to easily
        // find the correct TextView location & size later.
        toolbar.addView(getLayoutInflater().inflate(R.layout.file_detail_title_placeholder, null));

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);

        mScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED);
        // mScroller needs to perform asynchronous measurements after initalize(), therefore
        // we can't mark this as GONE.
        mScroller.setVisibility(View.INVISIBLE);

        setHeaderNameText(mFileSystemObject.getName());

        SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ true,
                new Runnable() {
                    @Override
                    public void run() {
                        if (!mHasAlreadyBeenOpened) {
                            // The initial scrim opacity must match the scrim opacity that would be
                            // achieved by scrolling to the starting position.
                            final float alphaRatio = mExtraMode == MODE_FULLY_EXPANDED ?
                                    1 : mScroller.getStartingTransparentHeightRatio();
                            final int duration = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                            final int desiredAlpha = (int) (0xFF * alphaRatio);
                            ObjectAnimator o = ObjectAnimator.ofInt(mWindowScrim, "alpha", 0,
                                    desiredAlpha).setDuration(duration);

                            o.start();
                        }
                    }
                });

        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt(KEY_THEME_COLOR, 0);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Need to wait for the pre draw before setting the initial scroll
                            // value. Prior to pre draw all scroll values are invalid.
                            if (mHasAlreadyBeenOpened) {
                                mScroller.setVisibility(View.VISIBLE);
                                mScroller.setScroll(mScroller.getScrollNeededToBeFullScreen());
                            }
                            // Need to wait for pre draw for setting the theme color. Setting the
                            // header tint before the MultiShrinkScroller has been measured will
                            // cause incorrect tinting calculations.
                            if (color != 0) {
                                setThemeColor();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showActivity();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mHasAlreadyBeenOpened = true;
        mIsEntranceAnimationFinished = true;
        mHasComputedThemeColor = false;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mColorFilter != null) {
            savedInstanceState.putInt(KEY_THEME_COLOR, mColorFilter.getColor());
        }
    }

    private void runEntranceAnimation() {
        if (mHasAlreadyBeenOpened) {
            return;
        }
        mHasAlreadyBeenOpened = true;
        mScroller.scrollUpForEntranceAnimation(mExtraMode != MODE_FULLY_EXPANDED);
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(int resId) {
        if (mScroller != null) {
            mScroller.setTitle(getText(resId) == null ? null : getText(resId).toString());
        }
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(String value) {
        if (!TextUtils.isEmpty(value)) {
            if (mScroller != null) {
                mScroller.setTitle(value);
            }
        }
    }

    private void showActivity() {
        if (mScroller != null) {
            mScroller.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private void setThemeColor() {
        // If the color is invalid, use the predefined default
        final int primaryColor = getResources().getColor(R.color.default_primary);
        mScroller.setHeaderTintColor(primaryColor);
        mStatusBarColor = getResources().getColor(R.color.primary_dark_material_dark);
        updateStatusBarColor();

        mColorFilter =
                new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
    }

    private void updateStatusBarColor() {
        if (mScroller == null) {
            return;
        }
        final int desiredStatusBarColor;
        // Only use a custom status bar color if QuickContacts touches the top of the viewport.
        if (mScroller.getScrollNeededToBeFullScreen() <= 0) {
            desiredStatusBarColor = mStatusBarColor;
        } else {
            desiredStatusBarColor = Color.TRANSPARENT;
        }
        // Animate to the new color.
        final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                getWindow().getStatusBarColor(), desiredStatusBarColor);
        animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
        animation.setEvaluator(new ArgbEvaluator());
        animation.start();
    }

    private int colorFromBitmap(Bitmap bitmap) {
        // Author of Palette recommends using 24 colors when analyzing profile photos.
        final int NUMBER_OF_PALETTE_COLORS = 24;
//        final Palette palette = Palette.generate(bitmap, NUMBER_OF_PALETTE_COLORS);
//        if (palette != null && palette.getVibrantSwatch() != null) {
//            return palette.getVibrantSwatch().getRgb();
//        }
        return 0;
    }

    @Override
    public void onBackPressed() {
        if (mScroller != null) {
            if (!mIsExitAnimationInProgress) {
                mScroller.scrollOffBottom();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();

        // override transitions to skip the standard window animations
        overridePendingTransition(0, 0);
    }


}
