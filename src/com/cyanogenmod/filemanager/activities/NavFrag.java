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
import android.os.Bundle;

import android.text.TextUtils;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy;

import java.util.List;

import static com.cyanogenmod.filemanager.activities.PickerActivity.EXTRA_FOLDER_PATH;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.AUDIO;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.IMAGE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.NONE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.VIDEO;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, NavFrag is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class NavFrag extends android.support.v4.app.Fragment
    implements com.cyanogenmod.filemanager.listeners.OnHistoryListener, com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener,
    com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationRequestMenuListener, com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationSelectionChangedListener {

    private static final String TAG = "NavFrag"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    // Bookmark list XML tags
    private static final String TAG_BOOKMARKS = "Bookmarks"; //$NON-NLS-1$
    private static final String TAG_BOOKMARK = "bookmark"; //$NON-NLS-1$

    private static final String STR_USB = "usb"; // $NON-NLS-1$

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SEARCH = 10001;

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SETTINGS = 20001;

    /**
     * Intent code for request a copy.
     */
    public static final int INTENT_REQUEST_COPY = 30001;

    /**
     * Intent code for request a move.
     */
    public static final int INTENT_REQUEST_MOVE = 30002;

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

    // The timeout needed to reset the exit status for back button
    // After this time user need to tap 2 times the back button to
    // exit, and the toast is shown again after the first tap.
    private static final int RELEASE_EXIT_CHECK_TIMEOUT = 3500;


    private android.support.v7.widget.Toolbar mToolBar;
    private android.widget.SearchView mSearchView;
    private com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView mCustomTitleView;
    private android.view.inputmethod.InputMethodManager mImm;

    private final android.content.BroadcastReceiver mNotificationReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(
                        com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_SETTING_CHANGED) == 0) {
                    // The settings has changed
                    String key = intent.getStringExtra(
                            com.cyanogenmod.filemanager.preferences.FileManagerSettings.EXTRA_SETTING_CHANGED_KEY);
                    if (key != null) {
                        // Disk usage warning level
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_DISK_USAGE_WARNING_LEVEL.getId()) == 0) {

                            // Set the free disk space warning level of the breadcrumb widget
                            com.cyanogenmod.filemanager.ui.widgets.Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                            String fds = com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getString(
                                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                                    (String) com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                        SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
                            breadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));
                            breadcrumb.updateMountPointInfo();
                            return;
                        }

                        // Case sensitive sort
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_CASE_SENSITIVE_SORT.getId()) == 0) {
                            getCurrentNavigationView().refresh();
                            return;
                        }

                        // Display thumbs
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_DISPLAY_THUMBS.getId()) == 0) {
                            // Clean the icon cache applying the current theme
                            applyTheme();
                            return;
                        }

                        // Use flinger
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_USE_FLINGER.getId()) == 0) {
                            boolean useFlinger =
                                    com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getBoolean(
                                            com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_USE_FLINGER.getId(),
                                                ((Boolean) com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                                        SETTINGS_USE_FLINGER.
                                                            getDefaultValue()).booleanValue());
                            getCurrentNavigationView().setUseFlinger(useFlinger);
                            return;
                        }

                        // Access mode
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_ACCESS_MODE.getId()) == 0) {
                            // Is it necessary to create or exit of the ChRooted?
                            boolean chRooted =
                                    com.cyanogenmod.filemanager.FileManagerApplication.
                                        getAccessMode().compareTo(
                                            com.cyanogenmod.filemanager.preferences.AccessMode.SAFE) == 0;
                            if (chRooted != com.cyanogenmod.filemanager.activities.NavFrag.this.mChRooted) {
                                if (chRooted) {
                                    createChRooted();
                                } else {
                                    exitChRooted();
                                }
                            }
                        }

                        // Restricted access
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS.getId()) == 0) {
                            if (com.cyanogenmod.filemanager.util.AndroidHelper.isSecondaryUser(context)) {
                                try {
                                    com.cyanogenmod.filemanager.preferences.Preferences.savePreference(
                                            com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_ACCESS_MODE,
                                            com.cyanogenmod.filemanager.preferences.AccessMode.SAFE, true);
                                } catch (Throwable ex) {
                                    android.util.Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
                                }
                                com.cyanogenmod.filemanager.console.ConsoleBuilder.changeToNonPrivilegedConsole(context);
                                createChRooted();
                            }
                        }

                        // Filetime format mode
                        if (key.compareTo(
                                com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                                SETTINGS_FILETIME_FORMAT_MODE.getId()) == 0) {
                            // Refresh the data
                            synchronized (com.cyanogenmod.filemanager.util.FileHelper.DATETIME_SYNC) {
                                com.cyanogenmod.filemanager.util.FileHelper.sReloadDateTimeFormats = true;
                                com.cyanogenmod.filemanager.activities.NavFrag.this.getCurrentNavigationView().refresh();
                            }
                        }
                    }

                } else if (intent.getAction().compareTo(
                        com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_FILE_CHANGED) == 0) {
                    // Retrieve the file that was changed
                    String file =
                            intent.getStringExtra(
                                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.EXTRA_FILE_CHANGED_KEY);
                    try {
                        com.cyanogenmod.filemanager.model.FileSystemObject fso = com.cyanogenmod.filemanager.util.CommandHelper.getFileInfo(context, file, null);
                        if (fso != null) {
                            getCurrentNavigationView().refresh(fso);
                        }
                    } catch (Exception e) {
                        com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(context, e, true, false);
                    }

                } else if (intent.getAction().compareTo(
                        com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();

                } else if (intent.getAction().compareTo(android.content.Intent.ACTION_TIME_CHANGED) == 0 ||
                           intent.getAction().compareTo(android.content.Intent.ACTION_DATE_CHANGED) == 0 ||
                           intent.getAction().compareTo(android.content.Intent.ACTION_TIMEZONE_CHANGED) == 0) {
                    // Refresh the data
                    synchronized (com.cyanogenmod.filemanager.util.FileHelper.DATETIME_SYNC) {
                        com.cyanogenmod.filemanager.util.FileHelper.sReloadDateTimeFormats = true;
                        com.cyanogenmod.filemanager.activities.NavFrag.this.getCurrentNavigationView().refresh();
                    }
                } else if (intent.getAction().compareTo(
                        com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED) == 0 ||
                            intent.getAction().equals(android.content.Intent.ACTION_MEDIA_MOUNTED) ||
                            intent.getAction().equals(android.content.Intent.ACTION_MEDIA_UNMOUNTED)) {
                    onRequestBookmarksRefresh();
                    removeUnmountedHistory();
                    removeUnmountedSelection();
                }
            }
        }
    };

    private android.view.View.OnClickListener mOnClickDrawerTabListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            switch (v.getId()) {
                case com.cyanogenmod.filemanager.R.id.drawer_bookmarks_tab:
                    if (!mBookmarksTab.isSelected()) {
                        mBookmarksTab.setSelected(true);
                        mHistoryTab.setSelected(false);
                        mBookmarksTab.setTextAppearance(
                                getActivity(), com.cyanogenmod.filemanager.R.style
                                        .primary_text_appearance);
                        mHistoryTab.setTextAppearance(
                                getActivity(), com.cyanogenmod.filemanager.R.style.secondary_text_appearance);
                        mHistoryLayout.setVisibility(android.view.View.GONE);
                        mBookmarksLayout.setVisibility(android.view.View.VISIBLE);
                        applyTabTheme();

                        try {
                            com.cyanogenmod.filemanager.preferences.Preferences.savePreference(
                                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.USER_PREF_LAST_DRAWER_TAB,
                                    Integer.valueOf(0), true);
                        } catch (Exception ex) {
                            android.util.Log.e(TAG, "Can't save last drawer tab", ex); //$NON-NLS-1$
                        }

                        mClearHistory.setVisibility(android.view.View.GONE);
                    }
                    break;
                case com.cyanogenmod.filemanager.R.id.drawer_history_tab:
                    if (!mHistoryTab.isSelected()) {
                        mHistoryTab.setSelected(true);
                        mBookmarksTab.setSelected(false);
                        mHistoryTab.setTextAppearance(
                                getActivity(), com.cyanogenmod.filemanager.R.style.primary_text_appearance);
                        mBookmarksTab.setTextAppearance(
                                getActivity(), com.cyanogenmod.filemanager.R.style.secondary_text_appearance);
                        mBookmarksLayout.setVisibility(android.view.View.GONE);
                        mHistoryLayout.setVisibility(android.view.View.VISIBLE);
                        applyTabTheme();

                        try {
                            com.cyanogenmod.filemanager.preferences.Preferences.savePreference(
                                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.USER_PREF_LAST_DRAWER_TAB,
                                    Integer.valueOf(1), true);
                        } catch (Exception ex) {
                            android.util.Log.e(TAG, "Can't save last drawer tab", ex); //$NON-NLS-1$
                        }

                        mClearHistory.setVisibility(mHistory.size() > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private android.view.View.OnClickListener mOnClickDrawerActionBarListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            switch (v.getId()) {
                case com.cyanogenmod.filemanager.R.id.ab_settings:
                    //mDrawerLayout.closeDrawer(android.view.Gravity.START);
                    openSettings();
                    break;
                case com.cyanogenmod.filemanager.R.id.ab_clear_history:
                    clearHistory();
                    mClearHistory.setVisibility(android.view.View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    static String MIME_TYPE_LOCALIZED_NAMES[];
    /**
     * @hide
     */
    static java.util.Map<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory, android.graphics.drawable.Drawable> EASY_MODE_ICONS = new
            java.util.HashMap<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory, android.graphics.drawable.Drawable>();

    /**
     * @hide
     */
    com.cyanogenmod.filemanager.ui.widgets.NavigationView[] mNavigationViews;
    /**
     * @hide
     */
    android.widget.ListView mEasyModeListView;
    private java.util.List<com.cyanogenmod.filemanager.model.History> mHistory;

    private static final java.util.List<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory> EASY_MODE_LIST = new java.util.ArrayList<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory>() {
        {
            add(NONE);
            add(IMAGE);
            add(VIDEO);
            add(AUDIO);
            add(DOCUMENT);
            add(APP);
        }
    };

    private android.widget.ArrayAdapter<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory> mEasyModeAdapter;
    private android.view.View.OnClickListener mEasyModeItemClickListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View view) {
            Integer position = (Integer) view.getTag();
            onClicked(position);
        }
    };

    private int mCurrentNavigationView;

    private android.view.ViewGroup mActionBar;
    private com.cyanogenmod.filemanager.ui.widgets.SelectionView mSelectionBar;

    //private android.support.v4.widget.DrawerLayout mDrawerLayout;
    //private android.view.ViewGroup mDrawer;
    private android.support.v4.app.ActionBarDrawerToggle mDrawerToggle;
    private android.widget.LinearLayout mDrawerHistory;
    private android.widget.TextView mDrawerHistoryEmpty;

    private android.widget.TextView mBookmarksTab;
    private android.widget.TextView mHistoryTab;
    private android.view.View mBookmarksLayout;
    private android.view.View mHistoryLayout;

    private com.cyanogenmod.filemanager.ui.widgets.ButtonItem mSettings;
    private com.cyanogenmod.filemanager.ui.widgets.ButtonItem mClearHistory;

    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> mBookmarks;
    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> mSdBookmarks;
    private android.widget.LinearLayout mDrawerBookmarks;

    private boolean mExitFlag = false;
    private long mExitBackTimeout = -1;

    private android.app.Dialog mActiveDialog = null;

    private int mOrientation;

    private boolean mNeedsEasyMode = false;

    /**
     * @hide
     */
    boolean mChRooted;

    /**
     * @hide
     */
    android.os.Handler mHandler;

    private android.os.AsyncTask<Void, Void, Boolean> mBookmarksTask;


    android.view.View mView;
    android.view.LayoutInflater mLayoutInflater;

    /**
     * {@inheritDoc}
     */
    @Override
    public android.view.View onCreateView(
            android.view.LayoutInflater inflater, android.view.ViewGroup container,
            Bundle savedInstanceState) {

        mLayoutInflater = inflater;
        
        if (DEBUG) {
            android.util.Log.d(TAG, "NavFrag.onCreate"); //$NON-NLS-1$
        }

        // Register the broadcast receiver
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_SETTING_CHANGED);
        filter.addAction(com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_FILE_CHANGED);
        filter.addAction(com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_THEME_CHANGED);
        filter.addAction(android.content.Intent.ACTION_DATE_CHANGED);
        filter.addAction(android.content.Intent.ACTION_TIME_CHANGED);
        filter.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(com.cyanogenmod.filemanager.preferences.FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
        getActivity().registerReceiver(this.mNotificationReceiver, filter);

        // This filter needs the file data scheme, so it must be defined separately.
        android.content.IntentFilter newFilter = new android.content.IntentFilter();
        newFilter.addAction(android.content.Intent.ACTION_MEDIA_MOUNTED);
        newFilter.addAction(android.content.Intent.ACTION_MEDIA_UNMOUNTED);
        newFilter.addDataScheme(android.content.ContentResolver.SCHEME_FILE);
        getActivity().registerReceiver(mNotificationReceiver, newFilter);

        //the input manager service
        mImm = (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(
                android.content.Context.INPUT_METHOD_SERVICE);

        // Set the theme before setContentView
        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());
        theme.setBaseThemeNoActionBar(getActivity());

        //Set the main layout of the activity


        mView = inflater.inflate(com.cyanogenmod.filemanager.R.layout.nav_fragment, container, false);

        //Initialize nfc adapter
        android.nfc.NfcAdapter mNfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getActivity());
        if (mNfcAdapter != null) {
            mNfcAdapter.setBeamPushUrisCallback(new android.nfc.NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public android.net.Uri[] createBeamUris(android.nfc.NfcEvent event) {
                    java.util.List<com.cyanogenmod.filemanager.model.FileSystemObject> selectedFiles =
                            getCurrentNavigationView().getSelectedFiles();
                    if (selectedFiles.size() > 0) {
                        java.util.List<android.net.Uri> fileUri = new java.util.ArrayList<android.net.Uri>();
                        for (com.cyanogenmod.filemanager.model.FileSystemObject f : selectedFiles) {
                            //Beam ignores folders and system files
                            if (!com.cyanogenmod.filemanager.util.FileHelper.isDirectory(f) && !com.cyanogenmod.filemanager.util.FileHelper.isSystemFile(f)) {
                                fileUri.add(android.net.Uri.fromFile(new java.io.File(f.getFullPath())));
                            }
                        }
                        if (fileUri.size() > 0) {
                            return fileUri.toArray(new android.net.Uri[fileUri.size()]);
                        }
                    }
                    return null;
                }
            }, getActivity());
        }

        //Initialize activity
        init();

        //Navigation views
        initNavigationViews();

        mToolBar = ((com.cyanogenmod.filemanager.activities.NavigationActivity) getActivity())
                .mToolbar;

        //Initialize action bars
        initTitleActionBar();
        initStatusActionBar();
        initSelectionBar();

        // Initialize navigation drawer
        //initDrawer();
        initBookmarks();

        // Adjust layout (only when start on landscape mode)
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            onLayoutChanged();
        }
        this.mOrientation = orientation;

        // Apply the theme
        applyTheme();

        // Show welcome message
        showWelcomeMsg();

        this.mHandler = new android.os.Handler();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Initialize console
                initConsole();

                //Initialize navigation
                int cc = com.cyanogenmod.filemanager.activities.NavFrag.this.mNavigationViews.length;
                for (int i = 0; i < cc; i++) {
                    initNavigation(i, false, getActivity().getIntent());
                }

                //Check the intent action
                checkIntent(getActivity().getIntent());
            }
        });

        MIME_TYPE_LOCALIZED_NAMES = com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.getFriendlyLocalizedNames(
                getActivity());

        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.NONE, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_all));
        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.IMAGE, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_image));
        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.VIDEO, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_video));
        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.AUDIO, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_music));
        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_document));
        EASY_MODE_ICONS.put(com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP, getResources().getDrawable(
                com.cyanogenmod.filemanager.R.drawable
                .ic_em_application));

        //Save state
        //super.onCreate(state);

        return mView;

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSearchView.getVisibility() == android.view.View.VISIBLE) {
            closeSearch();
        }

        // Check restrictions
        if (!com.cyanogenmod.filemanager.FileManagerApplication.checkRestrictSecondaryUsersAccess
                (getActivity(), mChRooted)) {
            return;
        }

        // Check that the current dir is mounted (for virtual filesystems)
        String curDir = mNavigationViews[mCurrentNavigationView].getCurrentDir();
        if (curDir != null) {
            com.cyanogenmod.filemanager.console.VirtualMountPointConsole vc = com.cyanogenmod.filemanager.console.VirtualMountPointConsole.getVirtualConsoleForPath(
                    mNavigationViews[mCurrentNavigationView].getCurrentDir());
            if (vc != null && !vc.isMounted()) {
                onRequestBookmarksRefresh();
                removeUnmountedHistory();
                removeUnmountedSelection();

                android.content.Intent intent = new android.content.Intent();
                intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                initNavigation(com.cyanogenmod.filemanager.activities.NavFrag.this.mCurrentNavigationView, false, intent);
            }
        }
    }

    /*@Override
    protected void onPostCreate(android.os.Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }*/



    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onLayoutChanged();
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        if (mNeedsEasyMode) {
            if (item.getItemId() == android.R.id.home) {
                if (mHistory.size() == 0 && !isEasyModeVisible()) {
                    performShowEasyMode();
                } else {
                    back();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) {
            android.util.Log.d(TAG, "NavFrag.onDestroy"); //$NON-NLS-1$
        }

        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
        }

        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        recycle();
        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * Method that returns the current navigation view.
     *
     * @return NavigationView The current navigation view
     */
    public com.cyanogenmod.filemanager.ui.widgets.NavigationView getCurrentNavigationView() {
        return getNavigationView(this.mCurrentNavigationView);
    }

    /**
     * Method that returns the current navigation view.
     *
     * @param viewId The view to return
     * @return NavigationView The current navigation view
     */
    public com.cyanogenmod.filemanager.ui.widgets.NavigationView getNavigationView(int viewId) {
        if (this.mNavigationViews == null) return null;
        return this.mNavigationViews[viewId];
    }

    /**
     * Method that initializes the activity.
     */
    private void init() {
        this.mHistory = new java.util.ArrayList<com.cyanogenmod.filemanager.model.History>();
        this.mChRooted = com.cyanogenmod.filemanager.FileManagerApplication.getAccessMode().compareTo(
                com.cyanogenmod.filemanager.preferences.AccessMode.SAFE) == 0;
    }

    /**
     * Method that displays a welcome message the first time the user
     * access the application
     */
    private void showWelcomeMsg() {

    }

    android.view.View titleLayout;

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Inflate the view and associate breadcrumb
        titleLayout = mLayoutInflater.inflate(
                com.cyanogenmod.filemanager.R.layout.navigation_view_customtitle, null, false);
        com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView title =
                (com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView)titleLayout.findViewById(
                        com.cyanogenmod.filemanager.R.id.navigation_title_flipper);
        title.setOnHistoryListener(this);
        com.cyanogenmod.filemanager.ui.widgets.Breadcrumb breadcrumb = (com.cyanogenmod.filemanager.ui.widgets.Breadcrumb)title.findViewById(
                com.cyanogenmod.filemanager.R.id.breadcrumb_view);
        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].setBreadcrumb(breadcrumb);
            this.mNavigationViews[i].setOnHistoryListener(this);
            this.mNavigationViews[i].setOnNavigationSelectionChangedListener(this);
            this.mNavigationViews[i].setOnNavigationOnRequestMenuListener(this);
            this.mNavigationViews[i].setCustomTitle(title);
        }

        // Set the free disk space warning level of the breadcrumb widget
        String fds = com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getString(
                com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                (String) com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
        breadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));

        //Configure the action bar options
        mToolBar.setBackgroundDrawable(
                getResources().getDrawable(com.cyanogenmod.filemanager.R.drawable.bg_material_titlebar));
        mToolBar.addView(titleLayout);
    }

    /**
     * Method that initializes the statusbar of the activity.
     */
    private void initStatusActionBar() {
        //Performs a width calculation of buttons. Buttons exceeds the width
        //of the action bar should be hidden
        //This application not use android ActionBar because the application
        //make uses of the title and bottom areas, and wants to force to show
        //the overflow button (without care of physical buttons)
        this.mActionBar = (android.view.ViewGroup) mView.findViewById(
                com.cyanogenmod.filemanager.R.id
                        .navigation_actionbar);
        this.mActionBar.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    android.view.View v, int left, int top, int right, int bottom, int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                //Get the width of the action bar
                int w = v.getMeasuredWidth();

                //Wake through children calculation his dimensions
                int bw = (int)getResources().getDimension(com.cyanogenmod.filemanager.R.dimen.default_buttom_width);
                int cw = 0;
                final android.view.ViewGroup abView = ((android.view.ViewGroup)v);
                int cc = abView.getChildCount();
                for (int i = 0; i < cc; i++) {
                    android.view.View child = abView.getChildAt(i);
                    child.setVisibility(cw + bw > w ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
                    cw += bw;
                }
            }
        });

        // Have overflow menu? Actually no. There is only a search action, so just hide
        // the overflow
        android.view.View overflow = mView.findViewById(
                com.cyanogenmod.filemanager.R.id.ab_overflow);
        overflow.setVisibility(android.view.View.GONE);

        // Show the status bar
        android.view.View statusBar = mView.findViewById(
                com.cyanogenmod.filemanager.R.id.navigation_statusbar_portrait_holder);
        statusBar.setVisibility(android.view.View.VISIBLE);


    }

    /**
     * Method that initializes the selectionbar of the activity.
     */
    private void initSelectionBar() {
        this.mSelectionBar = (com.cyanogenmod.filemanager.ui.widgets.SelectionView)mView.findViewById(
                com.cyanogenmod.filemanager.R.id.navigation_selectionbar);
    }

    /**
     * Method that initializes the navigation drawer of the activity.
     */
    private void initDrawer() {
        /*mDrawerLayout = (android.support.v4.widget.DrawerLayout) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.drawer_layout);
        //Set our status bar color
        mDrawerLayout.setStatusBarBackgroundColor(com.cyanogenmod.filemanager.R.color.material_palette_blue_primary_dark);
        mDrawer = (android.view.ViewGroup) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.drawer);
        mDrawerBookmarks = (android.widget.LinearLayout) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.bookmarks_list);
        mDrawerHistory = (android.widget.LinearLayout) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.history_list);
        mDrawerHistoryEmpty = (android.widget.TextView) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.history_empty);

        mBookmarksLayout = mView.findViewById(com.cyanogenmod.filemanager.R.id.drawer_bookmarks);
        mHistoryLayout = mView.findViewById(com.cyanogenmod.filemanager.R.id.drawer_history);
        mBookmarksTab = (android.widget.TextView) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.drawer_bookmarks_tab);
        mHistoryTab = (android.widget.TextView) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.drawer_history_tab);
        mBookmarksTab.setOnClickListener(mOnClickDrawerTabListener);
        mHistoryTab.setOnClickListener(mOnClickDrawerTabListener);

        mSettings = (com.cyanogenmod.filemanager.ui.widgets.ButtonItem) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.ab_settings);
        mSettings.setOnClickListener(mOnClickDrawerActionBarListener);
        mClearHistory = (com.cyanogenmod.filemanager.ui.widgets.ButtonItem) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.ab_clear_history);
        mClearHistory.setOnClickListener(mOnClickDrawerActionBarListener);

        // Restore the last tab pressed
        Integer lastTab = com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getInt(
                com.cyanogenmod.filemanager.preferences.FileManagerSettings.USER_PREF_LAST_DRAWER_TAB.getId(),
                (Integer) com.cyanogenmod.filemanager.preferences.FileManagerSettings.USER_PREF_LAST_DRAWER_TAB
                        .getDefaultValue());
        mOnClickDrawerTabListener.onClick(lastTab == 0 ? mBookmarksTab : mHistoryTab);

        // Set the navigation drawer "hamburger" icon
        mDrawerToggle = new android.support.v4.app.ActionBarDrawerToggle(getActivity(),
                mDrawerLayout,
                com.cyanogenmod.filemanager.R.drawable.ic_material_light_navigation_drawer,
                com.cyanogenmod.filemanager.R.string.drawer_open, com.cyanogenmod.filemanager.R.string.drawer_close) {

            public void onDrawerClosed(android.view.View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(android.view.View drawerView) {
                onDrawerLayoutOpened(drawerView);
                super.onDrawerOpened(drawerView);
            }
        };
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

                */
    }

    /***
     * Method that do something when the DrawerLayout opened.
     */
    private void onDrawerLayoutOpened(android.view.View drawerView){
        if (mSearchView != null && mSearchView.getVisibility() == android.view.View.VISIBLE) {
            closeSearch();
            hideSoftInput(drawerView);
        }
    }

    /**
     * Method that hide the software when the software showing.
     *
     * */
    private void hideSoftInput(android.view.View view){
        if (mImm != null) {
            mImm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Method that adds a history entry to the history list in the drawer
     */
    private void addHistoryToDrawer(int index, com.cyanogenmod.filemanager.parcelables.HistoryNavigable navigable) {
        // hide empty message
        mDrawerHistoryEmpty.setVisibility(android.view.View.GONE);

        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());
        com.cyanogenmod.filemanager.ui.IconHolder iconholder = new com.cyanogenmod.filemanager.ui
                .IconHolder(getActivity(), false);

        // inflate single bookmark layout item and fill it
        android.widget.LinearLayout view = (android.widget.LinearLayout) mLayoutInflater.inflate(
                com.cyanogenmod.filemanager.R.layout.history_item, null);

        android.widget.ImageView iconView = (android.widget.ImageView) view
                .findViewById(com.cyanogenmod.filemanager.R.id.history_item_icon);
        android.widget.TextView name = (android.widget.TextView) view.findViewById(
                com.cyanogenmod.filemanager.R.id.history_item_name);
        android.widget.TextView directory = (android.widget.TextView) view
                .findViewById(com.cyanogenmod.filemanager.R.id.history_item_directory);

        android.graphics.drawable.Drawable icon = iconholder.getDrawable("ic_fso_folder_drawable"); //$NON-NLS-1$
        if (navigable instanceof com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable) {
            icon = iconholder.getDrawable("ic_history_search_drawable"); //$NON-NLS-1$
        }
        iconView.setImageDrawable(icon);

        String title = navigable.getTitle();
        if (title == null || title.trim().length() == 0) {
            title = getString(com.cyanogenmod.filemanager.R.string.root_directory_name);
        }

        name.setText(title);
        directory.setText(navigable.getDescription());

        theme.setTextColor(getActivity(), name, "text_color");
        theme.setTextColor(getActivity(), directory, "text_color");

        // handle item click
        view.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                final int index = mDrawerHistory.indexOfChild(v);
                final int count = mDrawerHistory.getChildCount();
                final com.cyanogenmod.filemanager.model.History history = mHistory.get(count - index - 1);

                navigateToHistory(history);
                //mDrawerLayout.closeDrawer(android.view.Gravity.START);
            }
        });

        // add as first child
        mDrawerHistory.addView(view, 0);

        // Show clear button if history tab is selected
        mClearHistory.setVisibility(mHistoryTab.isSelected() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /**
     * Method takes a bookmark as argument and adds it to mBookmarks and the
     * list in the drawer
     */
    public void addBookmark(com.cyanogenmod.filemanager.model.Bookmark bookmark) {
        mBookmarks.add(bookmark);
        addBookmarkToDrawer(bookmark);
    }

    /**
     * Show the easy mode view
     */
    private void performShowEasyMode() {
        mEasyModeListView.setVisibility(android.view.View.VISIBLE);
        getCurrentNavigationView().setVisibility(android.view.View.GONE);
        performShowBackArrow(false);
    }

    /**
     * Hide the easy mode view
     */
    private void performHideEasyMode() {
        mEasyModeListView.setVisibility(android.view.View.GONE);
        getCurrentNavigationView().setVisibility(android.view.View.VISIBLE);
    }

    private void performShowBackArrow(boolean showBackArrow) {
        if (mNeedsEasyMode) {
            mDrawerToggle.setDrawerIndicatorEnabled(!showBackArrow);
        }
    }

    private boolean isEasyModeVisible() {
        return mEasyModeListView.getVisibility() != android.view.View.GONE;
    }

    /**
     * Method takes a bookmark as argument and adds it to the bookmark list in
     * the drawer
     */
    private void addBookmarkToDrawer(com.cyanogenmod.filemanager.model.Bookmark bookmark) {
        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());
        com.cyanogenmod.filemanager.ui.IconHolder iconholder = new com.cyanogenmod.filemanager.ui
                .IconHolder(getActivity(), false);

        // inflate single bookmark layout item and fill it
        android.widget.LinearLayout view = (android.widget.LinearLayout) mLayoutInflater.inflate(
                com.cyanogenmod.filemanager.R.layout.bookmarks_item, null);

        android.widget.ImageView icon = (android.widget.ImageView) view
                .findViewById(com.cyanogenmod.filemanager.R.id.bookmarks_item_icon);
        android.widget.TextView name = (android.widget.TextView) view.findViewById(
                com.cyanogenmod.filemanager.R.id.bookmarks_item_name);
        android.widget.TextView path = (android.widget.TextView) view.findViewById(
                com.cyanogenmod.filemanager.R.id.bookmarks_item_path);
        android.widget.ImageButton actionButton = (android.widget.ImageButton) view
                .findViewById(com.cyanogenmod.filemanager.R.id.bookmarks_item_action);

        name.setText(bookmark.mName);
        path.setText(bookmark.mPath);

        theme.setTextColor(getActivity(), name, "text_color");
        theme.setTextColor(getActivity(), path, "text_color");

        icon.setImageDrawable(iconholder.getDrawable(
                com.cyanogenmod.filemanager.util.BookmarksHelper
                .getIcon(bookmark)));

        android.graphics.drawable.Drawable action = null;
        String actionCd = null;
        if (bookmark.mType.compareTo(com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.HOME) == 0) {
            action = iconholder.getDrawable("ic_config_drawable"); //$NON-NLS-1$
            actionCd = getActivity().getApplicationContext().getString(
                    com.cyanogenmod.filemanager.R.string.bookmarks_button_config_cd);
        }
        else if (bookmark.mType.compareTo(com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USER_DEFINED) == 0) {
            action = iconholder.getDrawable("ic_close_drawable"); //$NON-NLS-1$
            actionCd = getActivity().getApplicationContext().getString(
                    com.cyanogenmod.filemanager.R.string.bookmarks_button_remove_bookmark_cd);
        }

        actionButton.setImageDrawable(action);
        actionButton.setVisibility(action != null ? android.view.View.VISIBLE : android.view.View.GONE);
        actionButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                final android.view.View v = (android.view.View) view.getParent();
                final int index = mDrawerBookmarks.indexOfChild(v);
                final com.cyanogenmod.filemanager.model.Bookmark bookmark = mBookmarks.get(index);

                // Configure home
                if (bookmark.mType.compareTo(
                        com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.HOME) == 0) {
                    // Show a dialog for configure initial directoryget
                    com.cyanogenmod.filemanager.ui.dialogs.InitialDirectoryDialog dialog = new com.cyanogenmod.filemanager.ui.dialogs.InitialDirectoryDialog(
                            getActivity());
                    dialog.setOnValueChangedListener(new com.cyanogenmod.filemanager.ui.dialogs.InitialDirectoryDialog.OnValueChangedListener() {
                        @Override
                        public void onValueChanged(String newInitialDir) {
                            bookmark.mPath = newInitialDir;

                            // reset drawer bookmarks list
                            initBookmarks();
                        }
                    });
                    dialog.show();
                    return;
                }

                // Remove bookmark
                if (bookmark.mType.compareTo(
                        com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USER_DEFINED) == 0) {
                    boolean result = com.cyanogenmod.filemanager.preferences.Bookmarks.removeBookmark(
                            getActivity().getApplicationContext(), bookmark);
                    if (!result) { // Show warning
                        com.cyanogenmod.filemanager.util.DialogHelper.showToast(getActivity()
                                        .getApplicationContext(),
                                com.cyanogenmod.filemanager.R.string.msgs_operation_failure,
                                android.widget.Toast.LENGTH_SHORT);
                        return;
                    }
                    mBookmarks.remove(bookmark);
                    mDrawerBookmarks.removeView(v);
                    return;
                }
            }
        });
        actionButton.setContentDescription(actionCd);

        // handle item click
        view.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                final int index = mDrawerBookmarks.indexOfChild(v);
                final com.cyanogenmod.filemanager.model.Bookmark bookmark = mBookmarks.get(index);

                boolean showEasyMode = (mSdBookmarks.contains(bookmark)) &&
                        getResources().getBoolean(com.cyanogenmod.filemanager.R.bool.cmcc_show_easy_mode);

                // try to navigate to the bookmark path
                try {
                    com.cyanogenmod.filemanager.model.FileSystemObject fso = com.cyanogenmod.filemanager.util.CommandHelper.getFileInfo(
                            getActivity().getApplicationContext(), bookmark.mPath, null);
                    if (fso != null) {
                        if (showEasyMode) {
                            performShowEasyMode();
                        } else {
                            performHideEasyMode();
                        }
                        performShowBackArrow(!mDrawerToggle.isDrawerIndicatorEnabled());
                        getCurrentNavigationView().open(fso);
                        //mDrawerLayout.closeDrawer(android.view.Gravity.START);
                    }
                    else {
                        // The bookmark does not exist, delete the user-defined
                        // bookmark
                        try {
                            com.cyanogenmod.filemanager.preferences.Bookmarks.removeBookmark
                                    (getActivity().getApplicationContext(),
                                    bookmark);

                            // reset bookmarks list to default
                            initBookmarks();
                        }
                        catch (Exception ex) {
                        }
                    }
                }
                catch (Exception e) { // Capture the exception
                    com.cyanogenmod.filemanager.util.ExceptionUtil
                            .translateException(
                                    getActivity(), e);
                    if (e instanceof com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory
                            || e instanceof java.io.FileNotFoundException) {
                        // The bookmark does not exist, delete the user-defined
                        // bookmark
                        try {
                            com.cyanogenmod.filemanager.preferences.Bookmarks.removeBookmark
                                    (getActivity().getApplicationContext(),
                                    bookmark);

                            // reset bookmarks list to default
                            initBookmarks();
                        }
                        catch (Exception ex) {
                        }
                    }
                    return;
                }
            }
        });

        mDrawerBookmarks.addView(view);
    }

    /**
     * Method that initializes the bookmarks.
     */
    private synchronized void initBookmarks() {
        /*if (mBookmarksTask != null &&
                !mBookmarksTask.getStatus().equals(android.os.AsyncTask.Status.FINISHED)) {
            return;
        }

        // Retrieve the loading view
        final android.view.View waiting = mView.findViewById(
                com.cyanogenmod.filemanager.R.id.bookmarks_loading);

        // Load bookmarks in background
        mBookmarksTask = new android.os.AsyncTask<Void, Void, Boolean>() {
            Exception mCause;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mBookmarks = loadBookmarks();
                    return Boolean.TRUE;

                }
                catch (Exception e) {
                    this.mCause = e;
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPreExecute() {
                waiting.setVisibility(android.view.View.VISIBLE);
                mDrawerBookmarks.removeAllViews();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                waiting.setVisibility(android.view.View.GONE);
                if (result.booleanValue()) {
                    for (com.cyanogenmod.filemanager.model.Bookmark bookmark : mBookmarks) {
                        addBookmarkToDrawer(bookmark);
                    }
                }
                else {
                    if (this.mCause != null) {
                        com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(
                                getActivity(), this.mCause);
                    }
                }
                mBookmarksTask = null;
            }

            @Override
            protected void onCancelled() {
                waiting.setVisibility(android.view.View.GONE);
                mBookmarksTask = null;
            }
        };
        mBookmarksTask.execute(); */
    }

    /**
     * Method that loads all kind of bookmarks and join in an array to be used
     * in the listview adapter.
     *
     * @return List<Bookmark>
     * @hide
     */
    java.util.List<com.cyanogenmod.filemanager.model.Bookmark> loadBookmarks() {
        // Bookmarks = HOME + FILESYSTEM + SD STORAGES + USER DEFINED
        // In ChRooted mode = SD STORAGES + USER DEFINED (from SD STORAGES)
        java.util.List<com.cyanogenmod.filemanager.model.Bookmark> bookmarks = new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();
        if (!this.mChRooted) {
            bookmarks.add(loadHomeBookmarks());
            bookmarks.addAll(loadFilesystemBookmarks());
        }
        mSdBookmarks = loadSdStorageBookmarks();
        bookmarks.addAll(mSdBookmarks);
        bookmarks.addAll(loadVirtualBookmarks());
        bookmarks.addAll(loadUserBookmarks());
        return bookmarks;
    }

    /**
     * Method that loads the home bookmark from the user preference.
     *
     * @return Bookmark The bookmark loaded
     */
    private com.cyanogenmod.filemanager.model.Bookmark loadHomeBookmarks() {
        String initialDir = com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getString(
                com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                (String) com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_INITIAL_DIR
                        .getDefaultValue());
        return new com.cyanogenmod.filemanager.model.Bookmark(
                com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.HOME,
                getString(com.cyanogenmod.filemanager.R.string.bookmarks_home), initialDir);
    }

    /**
     * Method that loads the filesystem bookmarks from the internal xml file.
     * (defined by this application)
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> loadFilesystemBookmarks() {
        try {
            // Initialize the bookmarks
            java.util.List<com.cyanogenmod.filemanager.model.Bookmark> bookmarks = new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();

            // Read the command list xml file
            android.content.res.XmlResourceParser parser = getResources().getXml(
                    com.cyanogenmod.filemanager.R.xml.filesystem_bookmarks);

            try {
                // Find the root element
                com.android.internal.util.XmlUtils.beginDocument(parser, TAG_BOOKMARKS);
                while (true) {
                    com.android.internal.util.XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (element == null) {
                        break;
                    }

                    if (TAG_BOOKMARK.equals(element)) {
                        CharSequence name = null;
                        CharSequence directory = null;

                        try {
                            name = getString(parser.getAttributeResourceValue(
                                    com.cyanogenmod.filemanager.R.styleable.Bookmark_name, 0));
                        }
                        catch (Exception e) {
                            /** NON BLOCK **/
                        }
                        try {
                            directory = getString(parser
                                    .getAttributeResourceValue(
                                            com.cyanogenmod.filemanager.R.styleable.Bookmark_directory, 0));
                        }
                        catch (Exception e) {
                            /** NON BLOCK **/
                        }
                        if (directory == null) {
                            directory = parser
                                    .getAttributeValue(com.cyanogenmod.filemanager.R.styleable.Bookmark_directory);
                        }
                        if (name != null && directory != null) {
                            bookmarks.add(new com.cyanogenmod.filemanager.model.Bookmark(
                                    com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.FILESYSTEM, name.toString(),
                                    directory.toString()));
                        }
                    }
                }

                // Return the bookmarks
                return bookmarks;

            }
            finally {
                parser.close();
            }
        }
        catch (Throwable ex) {
            android.util.Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }

        // No data
        return new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();
    }

    /**
     * Method that loads the secure digital card storage bookmarks from the
     * system.
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> loadSdStorageBookmarks() {
        // Initialize the bookmarks
        java.util.List<com.cyanogenmod.filemanager.model.Bookmark> bookmarks = new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();

        try {
            // Recovery sdcards from storage manager
            android.os.storage.StorageVolume[] volumes = com.cyanogenmod.filemanager.util.StorageHelper
                    .getStorageVolumes(getActivity().getApplication(), true);
            for (android.os.storage.StorageVolume volume: volumes) {
                if (volume != null) {
                    String mountedState = volume.getState();
                    String path = volume.getPath();
                    if (!android.os.Environment.MEDIA_MOUNTED.equalsIgnoreCase(mountedState) &&
                            !android.os.Environment.MEDIA_MOUNTED_READ_ONLY.equalsIgnoreCase(mountedState)) {
                        android.util.Log.w(TAG, "Ignoring '" + path + "' with state of '"+ mountedState + "'");
                        continue;
                    }
                    if (!android.text.TextUtils.isEmpty(path)) {
                        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);
                        com.cyanogenmod.filemanager.model.Bookmark bookmark;
                        if (lowerPath.contains(STR_USB)) {
                            bookmark = new com.cyanogenmod.filemanager.model.Bookmark(
                                    com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USB, com.cyanogenmod.filemanager.util.StorageHelper
                                    .getStorageVolumeDescription(getActivity().getApplication(),
                                            volume), path);
                        } else {
                            bookmark = new com.cyanogenmod.filemanager.model.Bookmark(
                                    com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SDCARD, com.cyanogenmod.filemanager.util.StorageHelper
                                    .getStorageVolumeDescription(getActivity().getApplication(),
                                            volume), path);
                        }
                        bookmarks.add(bookmark);
                    }
                }
            }

            // Return the bookmarks
            return bookmarks;
        }
        catch (Throwable ex) {
            android.util.Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }

        // No data
        return new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();
    }

    /**
     * Method that loads all virtual mount points.
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> loadVirtualBookmarks() {
        // Initialize the bookmarks
        java.util.List<com.cyanogenmod.filemanager.model.Bookmark> bookmarks = new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();
        java.util.List<com.cyanogenmod.filemanager.model.MountPoint> mps = com.cyanogenmod.filemanager.console.VirtualMountPointConsole.getVirtualMountPoints();
        for (com.cyanogenmod.filemanager.model.MountPoint mp : mps) {
            com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE type = null;
            String name = null;
            if (mp.isSecure()) {
                type = com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SECURE;
                name = getString(com.cyanogenmod.filemanager.R.string.bookmarks_secure);
            } else if (mp.isRemote()) {
                type = com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.REMOTE;
                name = getString(com.cyanogenmod.filemanager.R.string.bookmarks_remote);
            } else {
                continue;
            }
            bookmarks.add(new com.cyanogenmod.filemanager.model.Bookmark(type, name, mp.getMountPoint()));
        }
        return bookmarks;
    }

    /**
     * Method that loads the user bookmarks (added by the user).
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private java.util.List<com.cyanogenmod.filemanager.model.Bookmark> loadUserBookmarks() {
        java.util.List<com.cyanogenmod.filemanager.model.Bookmark> bookmarks = new java.util.ArrayList<com.cyanogenmod.filemanager.model.Bookmark>();
        android.database.Cursor cursor = com.cyanogenmod.filemanager.preferences.Bookmarks.getAllBookmarks(getActivity().getContentResolver());
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    com.cyanogenmod.filemanager.model.Bookmark bm = new com.cyanogenmod.filemanager.model.Bookmark(cursor);
                    if (this.mChRooted
                            && !com.cyanogenmod.filemanager.util.StorageHelper.isPathInStorageVolume(bm.mPath)) {
                        continue;
                    }
                    bookmarks.add(bm);
                }
                while (cursor.moveToNext());
            }
        }
        finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            }
            catch (Exception e) {
                /** NON BLOCK **/
            }
        }

        // Remove bookmarks from virtual storage if the filesystem is not mount
        int c = bookmarks.size() - 1;
        for (int i = c; i >= 0; i--) {
            com.cyanogenmod.filemanager.console.VirtualMountPointConsole vc =
                    com.cyanogenmod.filemanager.console.VirtualMountPointConsole.getVirtualConsoleForPath(bookmarks.get(i).mPath);
            if (vc != null && !vc.isMounted()) {
                bookmarks.remove(i);
            }
        }

        return bookmarks;
    }

    /**
     * Method that initializes the navigation views of the activity
     */
    private void initNavigationViews() {
        //Get the navigation views (wishlist: multiple view; for now only one view)
        this.mNavigationViews = new com.cyanogenmod.filemanager.ui.widgets.NavigationView[1];
        this.mCurrentNavigationView = 0;
        //- 0
        this.mNavigationViews[0] = (com.cyanogenmod.filemanager.ui.widgets.NavigationView)mView.findViewById(
                com.cyanogenmod.filemanager.R.id.navigation_view);
        this.mNavigationViews[0].setId(0);
        this.mEasyModeListView = (android.widget.ListView) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.lv_easy_mode);
        mEasyModeAdapter = new android.widget.ArrayAdapter<com.cyanogenmod.filemanager.util
                .MimeTypeHelper.MimeTypeCategory>(getActivity(), com.cyanogenmod.filemanager.R.layout
                .navigation_view_simple_item) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                convertView = (convertView == null) ?mLayoutInflater.inflate(
                        com.cyanogenmod.filemanager.R.layout
                        .navigation_view_simple_item, parent, false) : convertView;
                com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory item = getItem(position);
                String typeTitle = MIME_TYPE_LOCALIZED_NAMES[item.ordinal()];
                android.widget.TextView typeTitleTV = (android.widget.TextView) convertView
                        .findViewById(com.cyanogenmod.filemanager.R.id.navigation_view_item_name);
                android.widget.ImageView typeIconIV = (android.widget.ImageView) convertView
                        .findViewById(com.cyanogenmod.filemanager.R.id.navigation_view_item_icon);
                android.view.View checkBoxView = convertView.findViewById(
                        com.cyanogenmod.filemanager.R.id.navigation_view_item_check);
                checkBoxView.setVisibility(android.view.View.GONE);
                typeTitleTV.setText(typeTitle);
                typeIconIV.setImageDrawable(EASY_MODE_ICONS.get(item));
                convertView.setOnClickListener(mEasyModeItemClickListener);
                convertView.setTag(position);
                return convertView;
            }
        };
        mEasyModeAdapter.addAll(EASY_MODE_LIST);
        mEasyModeListView.setAdapter(mEasyModeAdapter);
    }

    private void onClicked(int position) {
        android.content.Intent intent = new android.content.Intent(getActivity(), com.cyanogenmod.filemanager.activities.SearchActivity.class);
        intent.setAction(android.content.Intent.ACTION_SEARCH);
        intent.putExtra(com.cyanogenmod.filemanager.activities.SearchActivity.EXTRA_SEARCH_DIRECTORY,
                getCurrentNavigationView().getCurrentDir());
        intent.putExtra(android.app.SearchManager.QUERY, "*"); // Use wild-card '*'

        if (position == 0) {
            // the user has selected all items, they want to see their folders so let's do that.
            performHideEasyMode();
            performShowBackArrow(true);
            return;

        } else {
            java.util.ArrayList<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory> searchCategories = new java.util.ArrayList<com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory>();
            com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory selectedCategory = EASY_MODE_LIST.get(position);
            searchCategories.add(selectedCategory);
            // a one off case where we implicitly want to also search for TEXT mimetypes when the
            // DOCUMENTS category is selected
            if (selectedCategory == com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT) {
                searchCategories.add(
                        com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.TEXT);
            }
            intent.putExtra(com.cyanogenmod.filemanager.activities.SearchActivity.EXTRA_SEARCH_MIMETYPE, searchCategories);
        }

        startActivity(intent);
    }

    /**
     * Method that initialize the console
     * @hide
     */
    void initConsole() {
        //Create the default console (from the preferences)
        try {
            com.cyanogenmod.filemanager.console.Console console = com.cyanogenmod.filemanager.console.ConsoleBuilder.getConsole(
                    getActivity());
            if (console == null) {
                throw new com.cyanogenmod.filemanager.console.ConsoleAllocException("console == null"); //$NON-NLS-1$
            }
        } catch (Throwable ex) {
            if (!com.cyanogenmod.filemanager.activities.NavFrag.this.mChRooted) {
                //Show exception and exit
                android.util.Log.e(TAG, getString(com.cyanogenmod.filemanager.R.string.msgs_cant_create_console), ex);
                // We don't have any console
                // Show exception and exit
                com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                        getActivity(),
                        com.cyanogenmod.filemanager.R.string.msgs_cant_create_console, android.widget.Toast.LENGTH_LONG);
                exit();
                return;
            }

            // We are in a trouble (something is not allowing creating the console)
            // Ask the user to return to prompt or root access mode mode with a
            // non-privileged console, prior to make crash the application
            askOrExit();
            return;
        }
    }

    /**
     * Method that initializes the navigation.
     *
     * @param viewId The navigation view identifier where apply the navigation
     * @param restore Initialize from a restore info
     * @param intent The current intent
     * @hide
     */
    void initNavigation(final int viewId, final boolean restore, final android.content.Intent intent) {
        final com.cyanogenmod.filemanager.ui.widgets.NavigationView navigationView = getNavigationView(viewId);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                //Is necessary navigate?
                if (!restore) {
                    applyInitialDir(navigationView, intent);
                }
            }
        });
    }

    /**
     * Method that applies the user-defined initial directory
     *
     * @param navigationView The navigation view
     * @param intent The current intent
     * @hide
     */
    void applyInitialDir(final com.cyanogenmod.filemanager.ui.widgets.NavigationView navigationView, final android.content.Intent intent) {
        //Load the user-defined initial directory
        String initialDir =
                com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().getString(
                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                    (String) com.cyanogenmod.filemanager.preferences.FileManagerSettings.
                        SETTINGS_INITIAL_DIR.getDefaultValue());

        // Check if request navigation to directory (use as default), and
        // ensure chrooted and absolute path
        String navigateTo = intent.getStringExtra(EXTRA_NAVIGATE_TO);
        if (navigateTo != null && navigateTo.length() > 0) {
            initialDir = navigateTo;
        }

        // Add to history
        final boolean addToHistory = intent.getBooleanExtra(EXTRA_ADD_TO_HISTORY, true);

        // We cannot navigate to a secure console if is unmount, go to root in that case
        com.cyanogenmod.filemanager.console.VirtualConsole vc = com.cyanogenmod.filemanager.console.VirtualMountPointConsole.getVirtualConsoleForPath(initialDir);
        if (vc != null && vc instanceof com.cyanogenmod.filemanager.console.secure.SecureConsole && !((com.cyanogenmod.filemanager.console.secure.SecureConsole) vc).isMounted()) {
            initialDir = com.cyanogenmod.filemanager.util.FileHelper.ROOT_DIRECTORY;
        }

        if (this.mChRooted) {
            // Initial directory is the first external sdcard (sdcard, emmc, usb, ...)
            if (!com.cyanogenmod.filemanager.util.StorageHelper.isPathInStorageVolume(initialDir)) {
                android.os.storage.StorageVolume[] volumes =
                        com.cyanogenmod.filemanager.util.StorageHelper.getStorageVolumes(getActivity(),
                                false);
                if (volumes != null && volumes.length > 0) {
                    initialDir = volumes[0].getPath();
                    //Ensure that initial directory is an absolute directory
                    initialDir = com.cyanogenmod.filemanager.util.FileHelper.getAbsPath(initialDir);
                } else {
                    // Show exception and exit
                    com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                            getActivity(),
                            com.cyanogenmod.filemanager.R.string.msgs_cant_create_console, android.widget.Toast.LENGTH_LONG);
                    exit();
                    return;
                }
            }
        } else {
            //Ensure that initial directory is an absolute directory
            final String userInitialDir = initialDir;
            initialDir = com.cyanogenmod.filemanager.util.FileHelper.getAbsPath(initialDir);
            final String absInitialDir = initialDir;
            java.io.File f = new java.io.File(initialDir);
            boolean exists = f.exists();
            if (!exists) {
                // Fix for /data/media/0. Libcore doesn't detect it correctly.
                try {
                    exists = com.cyanogenmod.filemanager.util.CommandHelper.getFileInfo(getActivity(),
                            initialDir, false, null) != null;
                } catch (com.cyanogenmod.filemanager.console.InsufficientPermissionsException ipex) {
                    com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(
                            getActivity(), ipex, false, true, new com.cyanogenmod.filemanager.util
                                    .ExceptionUtil.OnRelaunchCommandResult() {
                        @Override
                        public void onSuccess() {
                            navigationView.changeCurrentDir(absInitialDir, addToHistory);
                        }
                        @Override
                        public void onFailed(Throwable cause) {
                            showInitialInvalidDirectoryMsg(userInitialDir);
                            navigationView.changeCurrentDir(
                                    com.cyanogenmod.filemanager.util.FileHelper.ROOT_DIRECTORY,
                                    addToHistory);
                        }
                        @Override
                        public void onCancelled() {
                            showInitialInvalidDirectoryMsg(userInitialDir);
                            navigationView.changeCurrentDir(
                                    com.cyanogenmod.filemanager.util.FileHelper.ROOT_DIRECTORY,
                                    addToHistory);
                        }
                    });

                    // Asynchronous mode
                    return;
                } catch (Exception ex) {
                    // We are not interested in other exceptions
                    com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(getActivity(), ex,
                            true, false);
                }

                // Check again the initial directory
                if (!exists) {
                    showInitialInvalidDirectoryMsg(userInitialDir);
                    initialDir = com.cyanogenmod.filemanager.util.FileHelper.ROOT_DIRECTORY;
                }

                // Weird, but we have a valid initial directory
            }
        }

        boolean needsEasyMode = false;
        if (mSdBookmarks != null ) {
            for (com.cyanogenmod.filemanager.model.Bookmark bookmark :mSdBookmarks) {
                if (bookmark.mPath.equalsIgnoreCase(initialDir)) {
                    needsEasyMode = true;
                    break;
                }
            }
        }

        mNeedsEasyMode = getResources().getBoolean(com.cyanogenmod.filemanager.R.bool.cmcc_show_easy_mode);

        needsEasyMode = needsEasyMode && mNeedsEasyMode;
        if (needsEasyMode) {
            performShowEasyMode();
        } else {
            performHideEasyMode();
        }
        // Change the current directory to the user-defined initial directory
        navigationView.changeCurrentDir(initialDir, addToHistory);
    }

    /**
     * Displays a message reporting invalid directory
     *
     * @param initialDir The initial directory
     * @hide
     */
    void showInitialInvalidDirectoryMsg(String initialDir) {
        // Change to root directory
        com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                getActivity(),
                getString(
                        com.cyanogenmod.filemanager.R.string.msgs_settings_invalid_initial_directory,
                        initialDir),
                android.widget.Toast.LENGTH_SHORT);
    }

    /**
     * Method that verifies the intent passed to the activity, and checks
     * if a request is made like Search.
     *
     * @param intent The intent to check
     * @hide
     */
    void checkIntent(android.content.Intent intent) {
        //Search action
        if (android.content.Intent.ACTION_SEARCH.equals(intent.getAction())) {
            android.content.Intent searchIntent = new android.content.Intent(getActivity(), com.cyanogenmod
                    .filemanager.activities.SearchActivity.class);
            searchIntent.setAction(android.content.Intent.ACTION_SEARCH);
            //- SearchActivity.EXTRA_SEARCH_DIRECTORY
            searchIntent.putExtra(
                    com.cyanogenmod.filemanager.activities.SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationView().getCurrentDir());
            //- SearchManager.APP_DATA
            if (intent.getBundleExtra(android.app.SearchManager.APP_DATA) != null) {
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putAll(intent.getBundleExtra(android.app.SearchManager.APP_DATA));
                searchIntent.putExtra(android.app.SearchManager.APP_DATA, bundle);
            }
            //-- SearchManager.QUERY
            String query = intent.getStringExtra(android.app.SearchManager.QUERY);
            if (query != null) {
                searchIntent.putExtra(android.app.SearchManager.QUERY, query);
            }
            //- android.speech.RecognizerIntent.EXTRA_RESULTS
            java.util.ArrayList<String> extraResults =
                    intent.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (extraResults != null) {
                searchIntent.putStringArrayListExtra(
                        android.speech.RecognizerIntent.EXTRA_RESULTS, extraResults);
            }
            startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == INTENT_REQUEST_SETTINGS) {
            // reset bookmarks list to default as the user could changed the
            // root mode which changes the system bookmarks
            initBookmarks();
            return;
        }

        if (data != null) {
            switch (requestCode) {
                case INTENT_REQUEST_SEARCH:
                    if (resultCode == getActivity().RESULT_OK) {
                        //Change directory?
                        android.os.Bundle bundle = data.getExtras();
                        if (bundle != null) {
                            com.cyanogenmod.filemanager.model.FileSystemObject fso = (com.cyanogenmod.filemanager.model.FileSystemObject) bundle.getSerializable(
                                    EXTRA_SEARCH_ENTRY_SELECTION);
                            com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable searchInfo =
                                    bundle.getParcelable(EXTRA_SEARCH_LAST_SEARCH_DATA);
                            if (fso != null) {
                                //Goto to new directory
                                getCurrentNavigationView().open(fso, searchInfo);
                                performHideEasyMode();
                            }
                        }
                    } else if (resultCode == getActivity().RESULT_CANCELED) {
                        com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable searchInfo =
                                data.getParcelableExtra(EXTRA_SEARCH_LAST_SEARCH_DATA);
                        if (searchInfo != null && searchInfo.isSuccessNavigation()) {
                            //Navigate to previous history
                            back();
                        } else {
                            // I don't know is the search view was changed, so try to do a refresh
                            // of the navigation view
                            getCurrentNavigationView().refresh(true);
                        }
                    }
                    // reset bookmarks list to default as the user could have set a
                    // new bookmark in the search activity
                    initBookmarks();
                    break;

                // Paste selection
                case INTENT_REQUEST_COPY:
                    if (resultCode == Activity.RESULT_OK) {
                        Bundle extras = data.getExtras();
                        String destination = extras.getString(EXTRA_FOLDER_PATH);
                        List<FileSystemObject> selection =
                                getCurrentNavigationView().onRequestSelectedFiles();
                        if (!TextUtils.isEmpty(destination)) {
                            CopyMoveActionPolicy.copyFileSystemObjects(
                                    getActivity(),
                                    selection,
                                    destination,
                                    getCurrentNavigationView(),
                                    this);
                        }
                    }
                    break;

                // Move selection
                case INTENT_REQUEST_MOVE:
                    if (resultCode == Activity.RESULT_OK) {
                        Bundle extras = data.getExtras();
                        String destination = extras.getString(EXTRA_FOLDER_PATH);
                        List<FileSystemObject> selection =
                                getCurrentNavigationView().onRequestSelectedFiles();
                        if (!TextUtils.isEmpty(destination)) {
                            CopyMoveActionPolicy.moveFileSystemObjects(
                                    getActivity(),
                                    selection,
                                    destination,
                                    getCurrentNavigationView(),
                                    this);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewHistory(com.cyanogenmod.filemanager.parcelables.HistoryNavigable navigable) {
        //addHistoryToDrawer(this.mHistory.size(), navigable);
        //Recollect information about current status
        com.cyanogenmod.filemanager.model.History history = new com.cyanogenmod.filemanager.model.History(this.mHistory.size(), navigable);
        this.mHistory.add(history);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCheckHistory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        if (o instanceof com.cyanogenmod.filemanager.model.FileSystemObject) {
            // Refresh only the item
            this.getCurrentNavigationView().refresh((com.cyanogenmod.filemanager.model.FileSystemObject)o);
        } else if (o == null) {
            // Refresh all
            getCurrentNavigationView().refresh();
        }
        if (clearSelection) {
            this.getCurrentNavigationView().onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestBookmarksRefresh() {
        initBookmarks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {
        if (o instanceof com.cyanogenmod.filemanager.model.FileSystemObject) {
            // Remove from view
            this.getCurrentNavigationView().removeItem((com.cyanogenmod.filemanager.model.FileSystemObject)o);

            //Remove from history
            removeFromHistory((com.cyanogenmod.filemanager.model.FileSystemObject)o);
        } else {
            onRequestRefresh(null, clearSelection);
        }
        if (clearSelection) {
            this.getCurrentNavigationView().onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNavigateTo(Object o) {
        // Ignored
    }

    @Override
    public void onCancel(){
        // nop
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectionChanged(com.cyanogenmod.filemanager.ui.widgets.NavigationView navView, java.util.List<com.cyanogenmod.filemanager.model.FileSystemObject> selectedItems) {
        this.mSelectionBar.setSelection(selectedItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestMenu(com.cyanogenmod.filemanager.ui.widgets.NavigationView navView, com.cyanogenmod.filemanager.model.FileSystemObject item) {
        // Show the actions dialog
        openActionsDialog(item, false);
    }

    /**
     * Method that shows a popup with a menu associated a {@link FileManagerSettings}.
     *
     * @param anchor The action button that was pressed
     * @param settings The array of settings associated with the action button
     */
    public void showSettingsPopUp(
            android.view.View anchor, java.util.List<com.cyanogenmod.filemanager.preferences.FileManagerSettings> settings) {
        //Create the adapter
        final com.cyanogenmod.filemanager.adapters.MenuSettingsAdapter adapter = new com
                .cyanogenmod.filemanager.adapters.MenuSettingsAdapter(getActivity(), settings);

        //Create a show the popup menu
        final android.widget.ListPopupWindow popup = com.cyanogenmod.filemanager.util
                .DialogHelper.createListPopupWindow(getActivity(), adapter, anchor);
        popup.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, android.view.View v, int position, long id) {
                com.cyanogenmod.filemanager.preferences.FileManagerSettings setting =
                        ((com.cyanogenmod.filemanager.adapters.MenuSettingsAdapter)parent.getAdapter()).getSetting(position);
                final int value = ((com.cyanogenmod.filemanager.adapters.MenuSettingsAdapter)parent.getAdapter()).getId(position);
                popup.dismiss();
                try {
                    if (setting.compareTo(
                            com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_LAYOUT_MODE) == 0) {
                        //Need to change the layout
                        getCurrentNavigationView().changeViewMode(
                                com.cyanogenmod.filemanager.preferences.NavigationLayoutMode.fromId(value));
                    } else {
                        //Save and refresh
                        if (setting.getDefaultValue() instanceof Enum<?>) {
                            //Enumeration
                            com.cyanogenmod.filemanager.preferences.Preferences.savePreference(setting, new com.cyanogenmod.filemanager.preferences.ObjectIdentifier() {
                                @Override
                                public int getId() {
                                    return value;
                                }
                            }, false);
                        } else {
                            //Boolean
                            boolean newval =
                                    com.cyanogenmod.filemanager.preferences.Preferences.getSharedPreferences().
                                        getBoolean(
                                            setting.getId(),
                                            ((Boolean)setting.getDefaultValue()).booleanValue());
                            com.cyanogenmod.filemanager.preferences.Preferences.savePreference(setting, Boolean.valueOf(!newval), false);
                        }
                        getCurrentNavigationView().refresh();
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error applying navigation option", e); //$NON-NLS-1$
                    com.cyanogenmod.filemanager.activities.NavFrag.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                                    getActivity(),
                                    com.cyanogenmod.filemanager.R.string.msgs_settings_save_failure, android.widget.Toast.LENGTH_SHORT);
                        }
                    });

                } finally {
                    adapter.dispose();
                    getCurrentNavigationView().getCustomTitle().restoreView();
                }

            }
        });
        popup.setOnDismissListener(new android.widget.PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                adapter.dispose();
            }
        });
        popup.show();
    }

    /**
     * Method that show the information of a filesystem mount point.
     *
     * @param mp The mount point info
     * @param du The disk usage of the mount point
     */
    public void showMountPointInfo(com.cyanogenmod.filemanager.model.MountPoint mp, com.cyanogenmod
            .filemanager.model.DiskUsage du) {
        //Has mount point info?
        if (mp == null) {
            //There is no information
            android.app.AlertDialog alert =
                    com.cyanogenmod.filemanager.util.DialogHelper.createWarningDialog(
                            getActivity(),
                            com.cyanogenmod.filemanager.R.string.filesystem_info_warning_title,
                            com.cyanogenmod.filemanager.R.string.filesystem_info_warning_msg);
            com.cyanogenmod.filemanager.util.DialogHelper.delegateDialogShow(getActivity(), alert);
            return;
        }

        //Show a the filesystem info dialog
        com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog dialog = new com.cyanogenmod
                .filemanager.ui.dialogs.FilesystemInfoDialog(getActivity(), mp, du);
        dialog.setOnMountListener(new com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog.OnMountListener() {
            @Override
            public void onRemount(com.cyanogenmod.filemanager.model.MountPoint mountPoint) {
                //Update the statistics of breadcrumb, only if mount point is the same
                com.cyanogenmod.filemanager.ui.widgets.Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                if (breadcrumb.getMountPointInfo().compareTo(mountPoint) == 0) {
                    breadcrumb.updateMountPointInfo();
                }
                if (mountPoint.isSecure()) {
                    // Secure mountpoints only can be unmount, so we need to move the navigation
                    // to a secure storage (do not add to history)
                    android.content.Intent intent = new android.content.Intent();
                    intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                    initNavigation(com.cyanogenmod.filemanager.activities.NavFrag.this.mCurrentNavigationView, false, intent);
                }
            }
        });
        dialog.show();
    }

    /**
     * Method that checks the action that must be realized when the
     * back button is pushed.
     *
     * @return boolean Indicates if the action must be intercepted
     */
    private boolean checkBackAction() {
        // We need a basic structure to check this
        if (getCurrentNavigationView() == null) return false;

        if (mSearchView.getVisibility() == android.view.View.VISIBLE) {
            closeSearch();
        }

        //Check if the configuration view is showing. In this case back
        //action must be "close configuration"
        if (getCurrentNavigationView().getCustomTitle().isConfigurationViewShowing()) {
            getCurrentNavigationView().getCustomTitle().restoreView();
            return true;
        }

        //Do back operation over the navigation history
        boolean flag = this.mExitFlag;

        this.mExitFlag = !back();

        // Retrieve if the exit status timeout has expired
        long now = System.currentTimeMillis();
        boolean timeout = (this.mExitBackTimeout == -1 ||
                            (now - this.mExitBackTimeout) > RELEASE_EXIT_CHECK_TIMEOUT);

        //Check if there no history and if the user was advised in the last back action
        if (this.mExitFlag && (this.mExitFlag != flag || timeout)) {
            //Communicate the user that the next time the application will be closed
            this.mExitBackTimeout = System.currentTimeMillis();
            com.cyanogenmod.filemanager.util.DialogHelper.showToast(getActivity(), com.cyanogenmod.filemanager.R.string.msgs_push_again_to_exit, android.widget.Toast.LENGTH_SHORT);
            if (mNeedsEasyMode) {
                return isEasyModeVisible();
            } else {
                return true;
            }
        }

        //Back action not applied
        return !this.mExitFlag;
    }

    @Override
    public void startActivity(android.content.Intent intent) {
        // check if search intent
        if (android.content.Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(com.cyanogenmod.filemanager.activities.SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationView().getCurrentDir());
        }

        super.startActivity(intent);
    }

    /**
     * Method that returns the history size.
     */
    private void clearHistory() {
        this.mHistory.clear();
        mDrawerHistory.removeAllViews();
        mDrawerHistoryEmpty.setVisibility(android.view.View.VISIBLE);
    }

    /**
     * Method that navigates to the passed history reference.
     *
     * @param history The history reference
     * @return boolean A problem occurs while navigate
     */
    public synchronized boolean navigateToHistory(com.cyanogenmod.filemanager.model.History history) {
        try {
            //Gets the history
            com.cyanogenmod.filemanager.model.History realHistory = this.mHistory.get(history.getPosition());

            //Navigate to item. Check what kind of history is
            if (realHistory.getItem() instanceof com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) {
                //Navigation
                com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable info =
                        (com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable)realHistory.getItem();
                int viewId = info.getId();
                com.cyanogenmod.filemanager.ui.widgets.NavigationView view = getNavigationView(viewId);
                // Selected items must not be restored from on history navigation
                info.setSelectedFiles(view.getSelectedFiles());
                if (!view.onRestoreState(info)) {
                    return true;
                }

            } else if (realHistory.getItem() instanceof com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable) {
                //Search (open search with the search results)
                com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable info = (com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable)realHistory.getItem();
                android.content.Intent searchIntent = new android.content.Intent(getActivity(), com.cyanogenmod.filemanager.activities.SearchActivity.class);
                searchIntent.setAction(com.cyanogenmod.filemanager.activities.SearchActivity.ACTION_RESTORE);
                searchIntent.putExtra(com.cyanogenmod.filemanager.activities.SearchActivity.EXTRA_SEARCH_RESTORE, (android.os.Parcelable)info);
                startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            } else {
                //The type is unknown
                throw new IllegalArgumentException("Unknown history type"); //$NON-NLS-1$
            }

            //Remove the old history
            int cc = realHistory.getPosition();
            for (int i = this.mHistory.size() - 1; i >= cc; i--) {
                this.mHistory.remove(i);
                mDrawerHistory.removeViewAt(0);
            }

            if (mDrawerHistory.getChildCount() == 0) {
                mDrawerHistoryEmpty.setVisibility(android.view.View.VISIBLE);
            }

            //Navigate
            boolean clearHistory = mHistoryTab.isSelected() && mHistory.size() > 0;
            mClearHistory.setVisibility(clearHistory ? android.view.View.VISIBLE : android.view.View.GONE);
            return true;

        } catch (Throwable ex) {
            if (history != null) {
                android.util.Log.e(TAG,
                        String.format("Failed to navigate to history %d: %s", //$NON-NLS-1$
                                Integer.valueOf(history.getPosition()),
                                history.getItem().getTitle()), ex);
            } else {
                android.util.Log.e(TAG,
                        String.format("Failed to navigate to history: null", ex)); //$NON-NLS-1$
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                            getActivity(),
                            com.cyanogenmod.filemanager.R.string.msgs_history_unknown, android.widget.Toast.LENGTH_LONG);
                }
            });

            //Not change directory
            return false;
        }
    }

    /**
     * Method that request a back action over the navigation history.
     *
     * @return boolean If a back action was applied
     */
    public boolean back() {
        // Check that has valid history
        while (this.mHistory.size() > 0) {
            com.cyanogenmod.filemanager.model.History h = this.mHistory.get(this.mHistory.size() - 1);
            if (h.getItem() instanceof com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) {
                // Verify that the path exists
                String path = ((com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable)h.getItem()).getCurrentDir();

                try {
                    com.cyanogenmod.filemanager.model.FileSystemObject info = com.cyanogenmod
                            .filemanager.util.CommandHelper.getFileInfo(getActivity(), path, null);
                    if (info != null) {
                        break;
                    }
                    this.mHistory.remove(this.mHistory.size() - 1);
                } catch (Exception e) {
                    com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(getActivity(), e, true,
                            false);
                    this.mHistory.remove(this.mHistory.size() - 1);
                }
            } else {
                break;
            }
        }

        //Navigate to history
        if (this.mHistory.size() > 0) {
            return navigateToHistory(this.mHistory.get(this.mHistory.size() - 1));
        }

        //Nothing to apply
        mClearHistory.setVisibility(android.view.View.GONE);
        return false;
    }

    /**
     * Method that opens the actions dialog
     *
     * @param item The path or the {@link FileSystemObject}
     * @param global If the menu to display is the one with global actions
     */
    public void openActionsDialog(Object item, boolean global) {
        // Resolve the full path
        String path = String.valueOf(item);
        if (item instanceof com.cyanogenmod.filemanager.model.FileSystemObject) {
            path = ((com.cyanogenmod.filemanager.model.FileSystemObject)item).getFullPath();
        }

        // Prior to show the dialog, refresh the item reference
        com.cyanogenmod.filemanager.model.FileSystemObject fso = null;
        try {
            fso = com.cyanogenmod.filemanager.util.CommandHelper.getFileInfo(getActivity(), path, false,
                    null);
            if (fso == null) {
                throw new com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory(path);
            }

        } catch (Exception e) {
            // Notify the user
            com.cyanogenmod.filemanager.util.ExceptionUtil.translateException(getActivity(), e);

            // Remove the object
            if (e instanceof java.io.FileNotFoundException || e instanceof com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory) {
                // If have a FileSystemObject reference then there is no need to search
                // the path (less resources used)
                if (item instanceof com.cyanogenmod.filemanager.model.FileSystemObject) {
                    getCurrentNavigationView().removeItem((com.cyanogenmod.filemanager.model.FileSystemObject)item);
                } else {
                    getCurrentNavigationView().removeItem((String)item);
                }
            }
            return;
        }

        // Show the dialog
        com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog dialog = new com.cyanogenmod
                .filemanager.ui.dialogs.ActionsDialog(getActivity(), (NavigationActivity)
                getActivity(),
                fso,
                global, false);
        dialog.setOnRequestRefreshListener(this);
        dialog.setOnSelectionListener(getCurrentNavigationView());
        dialog.show();
    }

    /**
     * Method that opens the search view.
     *
     * @hide
     */
    void openSearch() {
        mSearchView.setVisibility(android.view.View.VISIBLE);
        mSearchView.onActionViewExpanded();
        mCustomTitleView.setVisibility(android.view.View.GONE);
    }

    void closeSearch() {
        mSearchView.setVisibility(android.view.View.GONE);
        mSearchView.onActionViewCollapsed();
        mCustomTitleView.setVisibility(android.view.View.VISIBLE);
    }

    /**
     * Method that opens the settings activity.
     *
     * @hide
     */
    void openSettings() {
        android.content.Intent settingsIntent = new android.content.Intent(
                getActivity(),
                com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences.class);
        startActivityForResult(settingsIntent, INTENT_REQUEST_SETTINGS);
    }

    /**
     * Method that remove the {@link FileSystemObject} from the history
     */
    private void removeFromHistory(com.cyanogenmod.filemanager.model.FileSystemObject fso) {
        if (this.mHistory != null) {
            int cc = this.mHistory.size() - 1;
            for (int i = cc; i >= 0 ; i--) {
                com.cyanogenmod.filemanager.model.History history = this.mHistory.get(i);
                if (history.getItem() instanceof com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) {
                    String p0 = fso.getFullPath();
                    String p1 = ((com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) history.getItem()).getCurrentDir();
                    if (p0.compareTo(p1) == 0) {
                        this.mHistory.remove(i);
                        mDrawerHistory.removeViewAt(mDrawerHistory.getChildCount() - i - 1);
                        mDrawerHistoryEmpty.setVisibility(
                                mDrawerHistory.getChildCount() == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
                        updateHistoryPositions();
                    }
                }
            }
        }
    }

    /**
     * Update the history positions after one of the history is removed from drawer
     */
    private void updateHistoryPositions() {
        int cc = this.mHistory.size() - 1;
        for (int i = 0; i <= cc ; i++) {
            com.cyanogenmod.filemanager.model.History history = this.mHistory.get(i);
            history.setPosition(i + 1);
        }
    }

    /**
     * Method that ask the user to change the access mode prior to crash.
     * @hide
     */
    void askOrExit() {
        //Show a dialog asking the user
        android.app.AlertDialog dialog =
            com.cyanogenmod.filemanager.util.DialogHelper.createYesNoDialog(
                getActivity(),
                com.cyanogenmod.filemanager.R.string.msgs_change_to_prompt_access_mode_title,
                com.cyanogenmod.filemanager.R.string.msgs_change_to_prompt_access_mode_msg,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface alertDialog, int which) {
                        if (which == android.content.DialogInterface.BUTTON_NEGATIVE) {
                            // We don't have any console
                            // Show exception and exit
                            com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                                    getActivity(),
                                    com.cyanogenmod.filemanager.R.string.msgs_cant_create_console, android.widget.Toast.LENGTH_LONG);
                            exit();
                            return;
                        }

                        // Ok. Now try to change to prompt mode. Any crash
                        // here is a fatal error. We won't have any console to operate.
                        try {
                            // Change console
                            com.cyanogenmod.filemanager.console.ConsoleBuilder.changeToNonPrivilegedConsole(
                                    getActivity());

                            // Save preferences
                            com.cyanogenmod.filemanager.preferences.Preferences.savePreference(
                                    com.cyanogenmod.filemanager.preferences.FileManagerSettings.SETTINGS_ACCESS_MODE,
                                    com.cyanogenmod.filemanager.preferences.AccessMode.PROMPT, true);

                        } catch (Exception e) {
                            // Displays an exception and exit
                            android.util.Log.e(TAG, getString(com.cyanogenmod.filemanager.R.string.msgs_cant_create_console), e);
                            com.cyanogenmod.filemanager.util.DialogHelper.showToast(
                                    getActivity(),
                                    com.cyanogenmod.filemanager.R.string.msgs_cant_create_console, android.widget.Toast.LENGTH_LONG);
                            exit();
                        }
                    }
               });
        com.cyanogenmod.filemanager.util.DialogHelper.delegateDialogShow(getActivity(), dialog);
    }

    /**
     * Method that creates a ChRooted environment, protecting the user to break anything in
     * the device
     * @hide
     */
    void createChRooted() {
        // If we are in a ChRooted mode, then do nothing
        if (this.mChRooted) return;
        this.mChRooted = true;

        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].createChRooted();
        }

        // Remove the selection
        cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            getCurrentNavigationView().onDeselectAll();
        }

        // Remove the history (don't allow to access to previous data)
        clearHistory();
    }

    /**
     * Method that exits from a ChRooted
     * @hide
     */
    void exitChRooted() {
        // If we aren't in a ChRooted mode, then do nothing
        if (!this.mChRooted) return;
        this.mChRooted = false;

        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].exitChRooted();
        }
    }

    /**
     * Method called when a controlled exit is required
     * @hide
     */
    void exit() {
        getActivity().finish();
    }

    private void recycle() {
        // Recycle the navigation views
        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].recycle();
        }
        try {
            com.cyanogenmod.filemanager.FileManagerApplication.destroyBackgroundConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            com.cyanogenmod.filemanager.console.ConsoleBuilder.destroyConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
    }

    /**
     * Method that reconfigures the layout for better fit in portrait and landscape modes
     */
    private void onLayoutChanged() {
        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());
       // boolean drawerOpen = mDrawerLayout.isDrawerOpen(android.view.Gravity.START);

        // Apply only when the orientation was changed
        int orientation = getResources().getConfiguration().orientation;
        if (this.mOrientation == orientation) return;
        this.mOrientation = orientation;

        // imitate a closed drawer while layout is rebuilt to avoid NullPointerException
        //if (drawerOpen) {
         //   mDrawerLayout.closeDrawer(android.view.Gravity.START);
        //}

        if (this.mOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape mode
            android.view.ViewGroup statusBar = (android.view.ViewGroup)mView.findViewById(
                    com.cyanogenmod.filemanager.R.id.navigation_statusbar);
            if (statusBar.getParent() != null) {
                android.view.ViewGroup parent = (android.view.ViewGroup) statusBar.getParent();
                parent.removeView(statusBar);
            }

            // Calculate the action button size (all the buttons must fit in the title bar)
            int bw = (int)getResources().getDimension(com.cyanogenmod.filemanager.R.dimen.default_buttom_width);
            int abw = this.mActionBar.getChildCount() * bw;
            int rbw = 0;
            int cc = statusBar.getChildCount();
            for (int i = 0; i < cc; i++) {
                android.view.View child = statusBar.getChildAt(i);
                if (child instanceof com.cyanogenmod.filemanager.ui.widgets.ButtonItem) {
                    rbw += bw;
                }
            }
            // Currently there isn't overflow menu
            int w = abw + rbw - bw;

            // Add to the new location
            android.view.ViewGroup newParent = (android.view.ViewGroup)mView.findViewById(
                    com.cyanogenmod.filemanager.R.id.navigation_title_landscape_holder);
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            w,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            statusBar.setLayoutParams(params);
            newParent.addView(statusBar);

            // Apply theme
            theme.setBackgroundDrawable(getActivity(), statusBar, "titlebar_drawable"); //$NON-NLS-1$

            // Hide holder
            android.view.View holder = mView.findViewById(
                    com.cyanogenmod.filemanager.R.id.navigation_statusbar_portrait_holder);
            holder.setVisibility(android.view.View.GONE);

        } else {
            // Portrait mode
            android.view.ViewGroup statusBar = (android.view.ViewGroup)mView.findViewById(
                    com.cyanogenmod.filemanager.R.id.navigation_statusbar);
            if (statusBar.getParent() != null) {
                android.view.ViewGroup parent = (android.view.ViewGroup) statusBar.getParent();
                parent.removeView(statusBar);
            }

            // Add to the new location
            android.view.ViewGroup newParent = (android.view.ViewGroup)mView.findViewById(
                    com.cyanogenmod.filemanager.R.id.navigation_statusbar_portrait_holder);
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            statusBar.setLayoutParams(params);
            newParent.addView(statusBar);

            // Apply theme
            theme.setBackgroundDrawable(getActivity(), statusBar, "statusbar_drawable"); //$NON-NLS-1$

            // Show holder
            newParent.setVisibility(android.view.View.VISIBLE);
        }

        // if drawer was open, imitate reopening
        //if (drawerOpen) {
        //    mDrawerToggle.onDrawerOpened(mDrawer);
        //}
    }

    /**
     * Method that removes all the history items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedHistory() {
        int cc = mHistory.size() - 1;
        for (int i = cc; i >= 0; i--) {
            com.cyanogenmod.filemanager.model.History history = mHistory.get(i);
            if (history.getItem() instanceof com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) {
                com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable navigableInfo =
                        ((com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable) history.getItem());
                com.cyanogenmod.filemanager.console.VirtualMountPointConsole vc =
                        com.cyanogenmod.filemanager.console.VirtualMountPointConsole.getVirtualConsoleForPath(
                                navigableInfo.getCurrentDir());
                if (vc != null && !vc.isMounted()) {
                    mHistory.remove(i);
                    //mDrawerHistory.removeViewAt(mDrawerHistory.getChildCount() - i - 1);
                }
            }
        }
        //mDrawerHistoryEmpty.setVisibility(
         //       mDrawerHistory.getChildCount() == 0 ? android.view.View.VISIBLE : android.view
         //       .View.GONE);
        updateHistoryPositions();
    }

    /**
     * Method that removes all the selection items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedSelection() {
        for (com.cyanogenmod.filemanager.ui.widgets.NavigationView view : mNavigationViews) {
            view.removeUnmountedSelection();
        }
        mSelectionBar.setSelection(getNavigationView(mCurrentNavigationView).getSelectedFiles());
    }

    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        int orientation = getResources().getConfiguration().orientation;
        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());
        theme.setBaseThemeNoActionBar(getActivity());
        applyTabTheme();


        //- Layout
        android.view.View v = mView.findViewById(com.cyanogenmod.filemanager.R.id.navigation_layout);
        theme.setBackgroundDrawable(getActivity(), v, "background_drawable"); //$NON-NLS-1$

        //- ActionBar
        //theme.setTitlebarDrawable(getActivity(), getActivity().getActionBar(),
        //        "titlebar_drawable");
        //$NON-NLS-1$

        // Hackery to theme search view
        mSearchView = (android.widget.SearchView) titleLayout.findViewById(
                com.cyanogenmod.filemanager.R.id.navigation_search_bar);
        int searchPlateId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        android.view.View searchPlate = mSearchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            int searchTextId = searchPlate.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            android.widget.TextView searchText = (android.widget.TextView) searchPlate.findViewById(searchTextId);
            if (searchText != null) {
                searchText.setTextColor(android.graphics.Color.WHITE);
                searchText.setHintTextColor(android.graphics.Color.WHITE);
            }

            int magId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            android.widget.ImageView magImage = (android.widget.ImageView) mSearchView.findViewById(magId);
            if (magImage != null) {
                magImage.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, 0));
            }
        }

        android.app.SearchManager searchManager = (android.app.SearchManager) getActivity()
                .getSystemService(
                        android.content.Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity()
                .getComponentName()));
        mSearchView.setIconifiedByDefault(false);

        mCustomTitleView = (com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView) titleLayout.findViewById(
                com.cyanogenmod.filemanager.R.id.navigation_title_flipper);
        mCustomTitleView.setVisibility(android.view.View.VISIBLE);

        //- StatusBar
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.navigation_statusbar);
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            theme.setBackgroundDrawable(getActivity(), v, "titlebar_drawable"); //$NON-NLS-1$
        } else {
            theme.setBackgroundDrawable(getActivity(), v, "statusbar_drawable"); //$NON-NLS-1$
        }
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_overflow);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_overflow_drawable"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_actions);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_actions_drawable"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_search);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_search_drawable"); //$NON-NLS-1$

        //- Expanders
        v = titleLayout.findViewById(com.cyanogenmod.filemanager.R.id.ab_configuration);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "expander_open_drawable"); //$NON-NLS-1$
        v = titleLayout.findViewById(com.cyanogenmod.filemanager.R.id.ab_close);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "expander_close_drawable"); //$NON-NLS-1$
        v = titleLayout.findViewById(com.cyanogenmod.filemanager.R.id.ab_sort_mode);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_sort_mode_drawable"); //$NON-NLS-1$
        v = titleLayout.findViewById(com.cyanogenmod.filemanager.R.id.ab_layout_mode);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_layout_mode_drawable"); //$NON-NLS-1$
        v = titleLayout.findViewById(com.cyanogenmod.filemanager.R.id.ab_view_options);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_view_options_drawable"); //$NON-NLS-1$

        //- SelectionBar
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.navigation_selectionbar);
        theme.setBackgroundDrawable(getActivity(), v, "selectionbar_drawable"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_selection_done);
        theme.setImageDrawable(getActivity(), (android.widget.ImageView)v, "ab_selection_done_drawable"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.navigation_status_selection_label);
        theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$

        // - Navigation drawer
        //v = mView.findViewById(com.cyanogenmod.filemanager.R.id.history_empty);
        //theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$

        /*for (int i=0; i<mDrawerHistory.getChildCount(); i++) {
            android.view.View item = mDrawerHistory.getChildAt(i);

            v = item.findViewById(com.cyanogenmod.filemanager.R.id.history_item_name);
            theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$
            v = item.findViewById(com.cyanogenmod.filemanager.R.id.history_item_directory);
            theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$
        } */

        //- NavigationView
        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            getNavigationView(i).applyTheme();
        }

        // if drawer was open, imitate reopening
        /*if (drawerOpen) {
            mDrawerToggle.onDrawerOpened(mDrawer);
        }*/
    }

    /**
     * Method that applies the current theme to the tab host
     */
    private void applyTabTheme() {
        // Apply the theme
        com.cyanogenmod.filemanager.ui.ThemeManager.Theme theme = com.cyanogenmod.filemanager.ui
                .ThemeManager.getCurrentTheme(getActivity());

        /*android.view.View v = mView.findViewById(com.cyanogenmod.filemanager.R.id.drawer);
        theme.setBackgroundDrawable(getActivity(), v, "background_drawable"); //$NON-NLS-1$

        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.drawer_bookmarks_tab);
        theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.drawer_history_tab);
        theme.setTextColor(getActivity(), (android.widget.TextView)v, "text_color"); //$NON-NLS-1$

        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_settings);
        theme.setImageDrawable(getActivity(), (com.cyanogenmod.filemanager.ui.widgets.ButtonItem) v, "ab_settings_drawable"); //$NON-NLS-1$
        v = mView.findViewById(com.cyanogenmod.filemanager.R.id.ab_clear_history);
        theme.setImageDrawable(getActivity(), (com.cyanogenmod.filemanager.ui.widgets.ButtonItem)
         v, "ab_delete_drawable"); //$NON-NLS-1$ */
    }

    public void updateActiveDialog(android.app.Dialog dialog) {
        mActiveDialog = dialog;
    }
}
