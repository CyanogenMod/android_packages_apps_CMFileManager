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

package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import com.cyanogenmod.filemanager.FileManagerApplication;

import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences;
import com.cyanogenmod.filemanager.console.ConsoleHolder;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.fragments.HomeFragment;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.io.File;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, MainActivity is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class MainActivity extends ActionBarActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ResultCallback<StorageProviderInfo.ProviderInfoListResult> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static boolean DEBUG = false;

    // Bookmark list XML tags
    private static final String TAG_BOOKMARKS = "Bookmarks"; //$NON-NLS-1$
    private static final String TAG_BOOKMARK = "bookmark"; //$NON-NLS-1$

    private static final String STR_USB = "usb"; // $NON-NLS-1$

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SETTINGS = 20001;

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

    /**
     * Fragment types
     */
    private enum FragmentType {
        // Home fragment
        HOME,

        // Navigation fragment
        NAVIGATION,
    }

    static String MIME_TYPE_LOCALIZED_NAMES[];

    int[][] color_states = new int[][] {
            new int[] {android.R.attr.state_checked}, // checked
            new int[0] // default
    };

    // TODO: Replace with legitimate colors per item.
    int[] colors = new int[] {
            R.color.favorites_primary,
            Color.BLACK
    };

    private Toolbar mToolbar;
    private Fragment currentFragment;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationDrawer;
    private Map<Integer, StorageProviderInfo> mProvidersMap;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        //Save state
        super.onCreate(state);
        //Set the main layout of the activity
        setContentView(R.layout.navigation);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawer = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationDrawer.setNavigationItemSelectedListener(this);
        ColorStateList colorStateList = new ColorStateList(color_states, colors);
        // TODO: Figure out why the following doesn't work correctly...
        mNavigationDrawer.setItemTextColor(colorStateList);
        mNavigationDrawer.setItemIconTintList(colorStateList);


        MIME_TYPE_LOCALIZED_NAMES = MimeTypeCategory.getFriendlyLocalizedNames(this);

        showWelcomeMsg();

        /*
         * TEST CODE
         * TODO: MOVE SOMEWHERE MORE LEGITIMATE
         */
        mProvidersMap = new HashMap<Integer, StorageProviderInfo>();
        StorageApi storageApi = StorageApi.newInstance(MainActivity.this);
        PendingResult<StorageProviderInfo.ProviderInfoListResult> pendingResult =
                storageApi.fetchProviders();
        pendingResult.setResultCallback(this);

        setCurrentFragment(FragmentType.HOME);

        //Initialize nfc adapter
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public android.net.Uri[] createBeamUris(android.nfc.NfcEvent event) {
                    if (currentFragment instanceof NavigationFragment) {
                        List<FileSystemObject> selectedFiles =
                                ((NavigationFragment)currentFragment).getCurrentNavigationView()
                                        .getSelectedFiles();
                        if (selectedFiles.size() > 0) {
                            List<android.net.Uri> fileUri = new ArrayList<Uri>();
                            for (FileSystemObject f : selectedFiles) {
                                //Beam ignores folders and system files
                                if (!FileHelper.isDirectory(f) && !FileHelper.isSystemFile(f)) {
                                    fileUri.add(Uri.fromFile(new File(f.getFullPath())));
                                }
                            }
                            if (fileUri.size() > 0) {
                                return fileUri.toArray(new android.net.Uri[fileUri.size()]);
                            }
                        }
                    }
                    return null;
                }
            }, this);
        }
    }

    private void setCurrentFragment(FragmentType fragmentType) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (fragmentType) {
            case NAVIGATION:
                currentFragment = new NavigationFragment();
                break;
            case HOME:
            default:
                // Default to HOME
                currentFragment = HomeFragment.newInstance();
                break;
        }

        fragmentManager.beginTransaction()
                .replace(R.id.navigation_fragment_container, currentFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
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
        //stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.navigation_item_home:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_home");
                setCurrentFragment(FragmentType.HOME);
                break;
            case R.id.navigation_item_favorites:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_favorites");
                break;
            case R.id.navigation_item_internal:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_favorites");
                setCurrentFragment(FragmentType.NAVIGATION);
                break;
            case R.id.navigation_item_root_d:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_root_d");
                break;
            case R.id.navigation_item_sd_card:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_sd_card");
                break;
            case R.id.navigation_item_usb:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_usb");
                break;
            case R.id.navigation_item_protected:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_protected");
                break;
            case R.id.navigation_item_manage:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_manage");
                break;
            case R.id.navigation_item_settings:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_settings");
                openSettings();
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, String.format("onNavigationItemSelected::default (%d)", id));

                    // Check for item id in remote roots
                    StorageProviderInfo providerInfo = mProvidersMap.get(id);
                    Log.v(TAG, "providerInfo " + providerInfo.hashCode());
                    Log.v(TAG, "providerInfo.package " + providerInfo.getPackage());
                    Log.v(TAG, "providerInfo.authority " + providerInfo.getAuthority());
                    Log.v(TAG, "providerInfo.needsAuth " + providerInfo.needAuthentication());
                    Log.v(TAG, "providerInfo.title " + providerInfo.getTitle());
                    Log.v(TAG, "providerInfo.summary " + providerInfo.getSummary());
                    Log.v(TAG, "providerInfo.root " + providerInfo.getRootPath());
                }
                break;
        }
        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onResult(StorageProviderInfo.ProviderInfoListResult providerInfoListResult) {
        List<StorageProviderInfo> providerInfoList =
                providerInfoListResult.getProviderInfoList();
        if (providerInfoList == null) {
            Log.e(TAG, "no results returned");
            return;
        }
        Log.v(TAG, "got result(s)! " + providerInfoList.size());
        for (StorageProviderInfo providerInfo : providerInfoList) {
            StorageApi sapi = StorageApi.newInstance(MainActivity.this);
            sapi.getMetadata(providerInfo, providerInfo.getRootPath(), true);
            if (DEBUG) {
                Log.v(TAG, "providerInfo " + providerInfo.hashCode());
                Log.v(TAG, "providerInfo.package " + providerInfo.getPackage());
                Log.v(TAG, "providerInfo.authority " + providerInfo.getAuthority());
                Log.v(TAG, "providerInfo.needsAuth " + providerInfo.needAuthentication());
                Log.v(TAG, "providerInfo.title " + providerInfo.getTitle());
                Log.v(TAG, "providerInfo.summary " + providerInfo.getSummary());
                Log.v(TAG, "providerInfo.root " + providerInfo.getRootPath());
            }
            final String rootTitle = String.format("%s %s", providerInfo.getTitle(),
                    providerInfo.getSummary());

            // Add provider to map
            mProvidersMap.put(rootTitle.hashCode(), providerInfo);

            // Verify console exists, or create one
            StorageApiConsole.registerStorageApiConsole(this, sapi, providerInfo);

            // Add to navigation drawer
            mNavigationDrawer.getMenu()
                    .add(R.id.navigation_group_roots, rootTitle.hashCode(), 0, rootTitle)
                    .setIcon(R.drawable.ic_fso_folder);
        }
    }

    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(android.view.View view) {

        if (currentFragment instanceof NavigationFragment) {

            switch (view.getId()) {
                //######################
                //Navigation Custom Title
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_configuration:
                    //Show navigation view configuration toolbar
                    ((NavigationFragment)currentFragment)
                            .getCurrentNavigationView().getCustomTitle().showConfigurationView();
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_close:
                    //Hide navigation view configuration toolbar
                    ((NavigationFragment)currentFragment)
                            .getCurrentNavigationView().getCustomTitle().hideConfigurationView();
                    break;

                //######################
                //Breadcrumb Actions
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_filesystem_info:
                    //Show information of the filesystem
                    com.cyanogenmod.filemanager.model.MountPoint mp =
                            ((NavigationFragment)currentFragment)
                                    .getCurrentNavigationView().getBreadcrumb().getMountPointInfo();
                    com.cyanogenmod.filemanager.model.DiskUsage du =
                            ((NavigationFragment)currentFragment)
                                    .getCurrentNavigationView().getBreadcrumb().getDiskUsageInfo();
                    ((NavigationFragment)currentFragment).showMountPointInfo(mp, du);
                    break;

                //######################
                //Navigation view options
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_sort_mode:
                    ((NavigationFragment)currentFragment).showSettingsPopUp(view,
                            java.util.Arrays.asList(
                                    new FileManagerSettings[]{
                                            FileManagerSettings.SETTINGS_SORT_MODE}));
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_layout_mode:
                    ((NavigationFragment)currentFragment).showSettingsPopUp(view,
                            java.util.Arrays.asList(
                                    new FileManagerSettings[]{
                                            FileManagerSettings.SETTINGS_LAYOUT_MODE}));
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_view_options:
                    // If we are in ChRooted mode, then don't show non-secure items
                    if (((NavigationFragment)currentFragment).mChRooted) {
                        ((NavigationFragment)currentFragment).showSettingsPopUp(view,
                                java.util.Arrays
                                        .asList(new FileManagerSettings[]{
                                                FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST}));
                    } else {
                        ((NavigationFragment)currentFragment).showSettingsPopUp(view,
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
                    ((NavigationFragment)currentFragment)
                            .getCurrentNavigationView().onDeselectAll();
                    break;

                //######################
                //Action Bar buttons
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_actions:
                    ((NavigationFragment)currentFragment).openActionsDialog(
                            ((NavigationFragment)currentFragment)
                                    .getCurrentNavigationView().getCurrentDir(),
                            true);
                    break;

                case com.cyanogenmod.filemanager.R.id.ab_search:
                    ((NavigationFragment)currentFragment).openSearch();
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Method that opens the settings activity.
     *
     * @hide
     */
    void openSettings() {
        Intent settingsIntent = new Intent(MainActivity.this,
                SettingsPreferences.class);
        startActivityForResult(settingsIntent, INTENT_REQUEST_SETTINGS);
    }

    /**
     * Method that displays a welcome message the first time the user
     * access the application
     */
    private void showWelcomeMsg() {
        boolean firstUse = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_FIRST_USE.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_FIRST_USE.getDefaultValue()).booleanValue());

        //Display the welcome message?
        if (firstUse && FileManagerApplication.hasShellCommands()) {

            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);

            try {
                Preferences.savePreference(FileManagerSettings.SETTINGS_FIRST_USE, Boolean.FALSE,
                        true);
            } catch (InvalidClassException e) {
                e.printStackTrace();
            }
        }
    }
}
