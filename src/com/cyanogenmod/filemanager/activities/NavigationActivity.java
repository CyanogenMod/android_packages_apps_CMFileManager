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

package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.ui.widgets.HomeFragment;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, NavigationActivity is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class NavigationActivity extends ActionBarActivity {

    private static final String TAG = "NavigationActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    // Bookmark list XML tags
    private static final String TAG_BOOKMARKS = "Bookmarks"; //$NON-NLS-1$
    private static final String TAG_BOOKMARK = "bookmark"; //$NON-NLS-1$

    private static final String STR_USB = "usb"; // $NON-NLS-1$

    /**
     * Constant for extra information about selected search entry.
     */
    public static final String EXTRA_SEARCH_ENTRY_SELECTION =
            "extra_search_entry_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about last search data.
     */
    public static final String EXTRA_SEARCH_LAST_SEARCH_DATA =
            "extra_search_last_search_data"; //$NON-NLS-1$

    /**
     * Constant for extra information for request a navigation to the passed path.
     */
    public static final String EXTRA_NAVIGATE_TO =
            "extra_navigate_to"; //$NON-NLS-1$

    /**
     * Constant for extra information for request to add navigation to the history
     */
    public static final String EXTRA_ADD_TO_HISTORY =
            "extra_add_to_history"; //$NON-NLS-1$


    static String MIME_TYPE_LOCALIZED_NAMES[];

    public HomeFragment mHomeFragment;
    public Toolbar mToolbar;
    NavFrag nFrag;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {


        //Save state
        super.onCreate(state);
        //Set the main layout of the activity
        setContentView(R.layout.navigation);

        //mToolbar = (Toolbar) findViewById(R.id.homepage_toolbar);
        //setSupportActionBar(mToolbar);

        // As we're using a Toolbar, we should retrieve it and set it
        // to be our ActionBar
        // to be our ActionBar
        mToolbar = (android.support.v7.widget.Toolbar) findViewById(
                com.cyanogenmod.filemanager.R.id.material_toolbar);

        setSupportActionBar(mToolbar);

        FragmentManager fragmentManager = getSupportFragmentManager();

        nFrag = new NavFrag();

        fragmentManager.beginTransaction()
                .replace(R.id.navigation_fragment_container, nFrag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

    }

    public void addBookmark(Bookmark bookmark) {
        // stub
    }

    public void updateActiveDialog(Dialog dialog) {
        // stub
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        //Initialize navigation
        //initNavigation(this.mCurrentNavigationView, true, intent);

        //Check the intent action
        //checkIntent(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /*if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            if (mDrawerLayout.isDrawerOpen(mDrawer)) {
                mDrawerLayout.closeDrawer(android.view.Gravity.START);
            } else {
                mDrawerLayout.openDrawer(android.view.Gravity.START);
            }
            return true;
        }*/
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
       /* if (mDrawerLayout.isDrawerOpen(android.view.Gravity.START)) {
            mDrawerLayout.closeDrawer(android.view.Gravity.START);
            return;
        }
        if (checkBackAction()) {
            performHideEasyMode();
            return;
        } else {
            if (mNeedsEasyMode && !isEasyModeVisible()) {
                performShowEasyMode();
                return;
            }
        }

        // An exit event has occurred, force the destroy the consoles
        exit(); */
    }


    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(android.view.View view) {

        if (nFrag != null && nFrag.isAdded()) {

            switch (view.getId()) {
                //######################
                //Navigation Custom Title
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_configuration:
                    //Show navigation view configuration toolbar
                    nFrag.getCurrentNavigationView().getCustomTitle().showConfigurationView();
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_close:
                    //Hide navigation view configuration toolbar
                    nFrag.getCurrentNavigationView().getCustomTitle().hideConfigurationView();
                    break;

                //######################
                //Breadcrumb Actions
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_filesystem_info:
                    //Show information of the filesystem
                    com.cyanogenmod.filemanager.model.MountPoint mp =
                            nFrag.getCurrentNavigationView().getBreadcrumb().getMountPointInfo();
                    com.cyanogenmod.filemanager.model.DiskUsage du =
                            nFrag.getCurrentNavigationView().getBreadcrumb().getDiskUsageInfo();
                    nFrag.showMountPointInfo(mp, du);
                    break;

                //######################
                //Navigation view options
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_sort_mode:
                    nFrag.showSettingsPopUp(view,
                            java.util.Arrays.asList(
                                    new FileManagerSettings[]{
                                            FileManagerSettings.SETTINGS_SORT_MODE}));
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_layout_mode:
                    nFrag.showSettingsPopUp(view,
                            java.util.Arrays.asList(
                                    new FileManagerSettings[]{
                                            FileManagerSettings.SETTINGS_LAYOUT_MODE}));
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_view_options:
                    // If we are in ChRooted mode, then don't show non-secure items
                    if (nFrag.mChRooted) {
                        nFrag.showSettingsPopUp(view,
                                java.util.Arrays
                                        .asList(new FileManagerSettings[]{
                                                FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST}));
                    } else {
                        nFrag.showSettingsPopUp(view,
                                java.util.Arrays
                                        .asList(new FileManagerSettings[]{
                                                FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST,
                                                FileManagerSettings.SETTINGS_SHOW_HIDDEN,
                                                FileManagerSettings.SETTINGS_SHOW_SYSTEM,
                                                FileManagerSettings.SETTINGS_SHOW_SYMLINKS}));
                    }

                    break;

                //######################
                //Selection Actions
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_selection_done:
                    //Show information of the filesystem
                    nFrag.getCurrentNavigationView().onDeselectAll();
                    break;

                //######################
                //Action Bar buttons
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_actions:
                    nFrag.openActionsDialog(nFrag.getCurrentNavigationView().getCurrentDir(),
                            true);
                    break;

                case com.cyanogenmod.filemanager.R.id.ab_search:

                    nFrag.openSearch();
                    break;

                default:
                    break;
            }
        }
    }

}
