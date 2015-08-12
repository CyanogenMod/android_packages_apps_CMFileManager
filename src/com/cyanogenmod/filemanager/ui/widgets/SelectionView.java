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

package com.cyanogenmod.filemanager.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.widget.TextView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.policy.PrintActionPolicy;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * A view that holds the selection of files
 */
public class SelectionView extends LinearLayout {

    /**
     * @hide
     */
    int mViewHeight;
    private int mEffectDuration;
    private Toolbar mToolbar;
    private FolderUsageExecutable mFolderUsageExecutable;
    private View mTitleLayout;
    int mSize = 0;
    private NavigationView mNavigationView;

    /**
     * Constructor of <code>SelectionView</code>.
     *
     * @param context The current context
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public SelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>SelectionView</code>.
     *
     * @param context  The current context
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *                 will be applied (beyond what is included in the theme). This may
     *                 either be an attribute resource, whose value will be retrieved
     *                 from the current theme, or an explicit style resource.
     */
    public SelectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        //Add the view of the breadcrumb
        View content = inflate(getContext(), R.layout.navigation_view_selectionbar, null);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Obtain the height of the view for use in expand/collapse animation
        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        SelectionView.this.mViewHeight = getHeight();
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        setVisibility(View.GONE);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)
                                SelectionView.this.getLayoutParams();
                        params.height = 0;
                    }
                });

        //Recovery all views
        this.mToolbar = (Toolbar) content.findViewById(R.id.selection_toolbar);

        this.mToolbar.inflateMenu(R.menu.selection_menu);

        mTitleLayout = inflate(getContext(), R.layout.selection_view_customtitle, null);

        mToolbar.addView(mTitleLayout);

        // Obtain the duration of the effect
        this.mEffectDuration =
                getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);

        addView(content);
    }

    /**
     * Method that configure the menu to show according the actual information,
     * the kind of request, the file selection, the mount point, ...
     */
    private void configureMenu(List<FileSystemObject> selection) {
        // Selection
        mToolbar.getMenu().clear();
        this.mToolbar.inflateMenu(R.menu.selection_menu);

        Menu menu = mToolbar.getMenu();
        // run only single file specific items
        FileSystemObject fso = selection.get(0);
        if (selection.size() == 1) {

            // Print (only for text and image categories)
            if (!PrintActionPolicy.isPrintedAllowed(getContext(), fso)) {
                menu.removeItem(R.id.mnu_actions_print);
            }

            if (fso.isSecure() || fso.isRemote()) {
                menu.removeItem(R.id.mnu_actions_add_shortcut);
            }

            //Execute only if mime/type category is EXEC
            MimeTypeHelper.MimeTypeCategory category = MimeTypeHelper.getCategory(getContext(), fso);
            if (category.compareTo(MimeTypeHelper.MimeTypeCategory.EXEC) != 0) {
                menu.removeItem(R.id.mnu_actions_execute);
            }

            if (category.compareTo(MimeTypeHelper.MimeTypeCategory.COMPRESS) == 0) {
                menu.removeItem(R.id.mnu_actions_compress);
            } else {
                menu.removeItem(R.id.mnu_actions_extract);
            }


            //- Open/Open with -> Only when the fso is not a folder
            if (FileHelper.isDirectory(fso)) {
                menu.removeItem(R.id.mnu_actions_open);
                menu.removeItem(R.id.mnu_actions_open_with);
                menu.removeItem(R.id.mnu_actions_send);
            }

        } else {
            // run only global items
            // Don't allow mass rename to avoid horrors.
            menu.removeItem(R.id.mnu_actions_rename);

            // TODO can we print multiple items? what voodoo is this
            // does this feature even work?
            menu.removeItem(R.id.mnu_actions_print);

            // don't allow multiple shortcut adds
            menu.removeItem(R.id.mnu_actions_add_shortcut);

            // We don't compute multiple checksums at once
            menu.removeItem(R.id.mnu_actions_compute_checksum);

            // Don't execute all the things
            menu.removeItem(R.id.mnu_actions_execute);

            // open with sadness
            menu.removeItem(R.id.mnu_actions_open);
            menu.removeItem(R.id.mnu_actions_open_with);
            menu.removeItem(R.id.mnu_actions_send);

        }

        // Not allowed if not in search
        // TODO figure out search and what kind of things it likes to do.
        menu.removeItem(R.id.mnu_actions_open_parent_folder);

        // dark magic to show icons in the overflow menu
        if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                Field field = menu.getClass().getDeclaredField("mOptionalIconsVisible");
                field.setAccessible(true);
                field.setBoolean(menu, true);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method that computes the selection and returns a text message.
     *
     * @param selection The selection
     * @return String The computed text from the selection
     */
    private String computeSelection(List<FileSystemObject> selection) {
        int cc = selection.size();
        final Resources res = getContext().getResources();

        configureMenu(selection);

        if (cc == 1) {
            FileSystemObject fso = selection.get(0);
            return fso.getName();
        } else {
            return res.getQuantityString(R.plurals.selection, cc, cc);
        }
    }

    private String getSubtitle(List<FileSystemObject> selection) {
        int folders = 0;
        int files = 0;
        final Resources res = getContext().getResources();

        for (FileSystemObject fso : selection) {
            if (FileHelper.isDirectory(fso)) {
                folders++;
            } else {
                files++;
            }
        }

        // Get the string
        if (files == 0) {
            return res.getQuantityString(R.plurals.folders, folders, folders);
        }

        if (folders == 0) {
            return getFileSizes(selection);
        }

        String nFoldersString = res.getQuantityString(R.plurals.n_folders, folders, folders);
        String nFilesString = res.getQuantityString(R.plurals.n_files, files, files);
        return res.getString(R.string.selection_folders_and_files, nFilesString, nFoldersString);
    }

    public String getFileSizes(List<FileSystemObject> selection) {
        for (FileSystemObject fso : selection) {
            mSize += fso.getSize();
        }
        return FileHelper.getHumanReadableSize(mSize);
    }

    public void setMenuClickListener(Toolbar.OnMenuItemClickListener menuClickListener) {
        mToolbar.setOnMenuItemClickListener(menuClickListener);
    }

    public void setNavigationView(NavigationView currentNavigationView) {
        mNavigationView = currentNavigationView;
    }


    /**
     * Method that sets the {@link FileSystemObject} selection list
     *
     * @param newSelection The new selection list
     */
    public void setSelection(final List<FileSystemObject> newSelection) {
        // selection changed, wipe away old things
        mSize = 0;

        // Compute the selection
        TextView title = (TextView) mTitleLayout.findViewById(R.id.selector_title);
        TextView subtitle = ((TextView) mTitleLayout.findViewById(R.id.selector_subtitle));

        if (newSelection != null && newSelection.size() > 0 && title != null && subtitle != null) {
            title.setText(computeSelection(newSelection));
            subtitle.setText(getSubtitle(newSelection));
        }

        // Requires show the animation (expand or collapse)?
        // Is the current state need to be changed?
        if ((newSelection == null || newSelection.size() == 0) &&
                this.getVisibility() == View.GONE) {
            return;
        }
        if ((newSelection != null && newSelection.size() > 0) &&
                this.getVisibility() == View.VISIBLE) {
            return;
        }

        int animation = newSelection != null && newSelection.size() > 0 ? R.anim.slide_in : R.anim.slide_out;
        Animation anim = AnimationUtils.loadAnimation(getContext(), animation);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(250L);
        anim.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if (newSelection != null && newSelection.size() > 0) {
                    setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();

                if (newSelection != null && newSelection.size() > 0) {
                    params.height = SelectionView.this.mViewHeight;
                    navView(false);
                } else {
                    params.height = 0;
                    setVisibility(View.GONE);
                    navView(true);
                }

            }

        });

        startAnimation(anim);

    }

    public void navView(boolean isReversed) {
        ValueAnimator va = ValueAnimator.ofInt(0, SelectionView.this.mViewHeight);
        va.setDuration(400L);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mNavigationView.getLayoutParams();
                params.bottomMargin = (Integer) animation.getAnimatedValue();
                mNavigationView.requestLayout();
            }
        });
        if (isReversed) {
            va.reverse();
        } else {
            va.start();
        }
    }

}
