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

package com.cyanogenmod.filemanager.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.storage.StorageVolume;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.MainActivity;
import com.cyanogenmod.filemanager.activities.SearchActivity;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.dialogs.SortViewOptions;
import com.cyanogenmod.filemanager.listeners.OnHistoryListener;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.History;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.parcelables.HistoryNavigable;
import com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable;
import com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationLayoutMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.PreferenceHelper;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.tasks.FileSystemInfoTask;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog.OnMountListener;
import com.cyanogenmod.filemanager.ui.dialogs.InputNameDialog;
import com.cyanogenmod.filemanager.ui.policy.CompressActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.DeleteActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.ExecutionActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.InfoActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.NewActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.PrintActionPolicy;
import com.cyanogenmod.filemanager.ui.widgets.Breadcrumb;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnBackRequestListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnDirectoryChangedListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationRequestMenuListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationSelectionChangedListener;
import com.cyanogenmod.filemanager.ui.widgets.SelectionView;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.cyanogenmod.filemanager.activities.PickerActivity.ACTION_MODE.COPY;
import static com.cyanogenmod.filemanager.activities.PickerActivity.ACTION_MODE.MOVE;
import static com.cyanogenmod.filemanager.activities.PickerActivity.EXTRA_ACTION;
import static com.cyanogenmod.filemanager.activities.PickerActivity.EXTRA_FOLDER_PATH;
import static com.cyanogenmod.filemanager.activities.PickerActivity.INTENT_FOLDER_SELECT;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, NavigationFragment is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class NavigationFragment extends Fragment
        implements OnHistoryListener, OnRequestRefreshListener,
        OnNavigationRequestMenuListener, OnNavigationSelectionChangedListener,
        OnDirectoryChangedListener, Toolbar.OnMenuItemClickListener{

    private static final String TAG = "NavigationFragment"; //$NON-NLS-1$

    private static boolean DEBUG = false;

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

    private Toolbar mToolBar;
    private SearchView mSearchView;
    private View mTitleLayout;

    private OnBackRequestListener mOnBackRequestListener;
    private OnGoHomeRequestListener mOnGoHomeRequestListener;
    private OnDirectoryChangedListener mOnDirectoryChangedListener;

    /**
     * An interface to communicate a request to go home
     */
    public interface OnGoHomeRequestListener {
        /**
         * Method invoked when requested to go home
         *
         */
        void onGoHomeRequested(String message);
    }

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_SETTING_CHANGED) == 0) {
                    // The settings has changed
                    String key = intent.getStringExtra(FileManagerSettings.
                            EXTRA_SETTING_CHANGED_KEY);
                    if (key != null) {
                        // Disk usage warning level
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_DISK_USAGE_WARNING_LEVEL.getId()) == 0) {

                            // Set the free disk space warning level of the breadcrumb widget
                            Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                            if (breadcrumb != null) {
                                String fds = Preferences.getSharedPreferences().getString(
                                        FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                                        (String)FileManagerSettings.
                                                SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
                                mFreeDiskSpaceWarningLevel = Integer.parseInt(fds);
                                breadcrumb.setFreeDiskSpaceWarningLevel(mFreeDiskSpaceWarningLevel);
                                breadcrumb.updateMountPointInfo();
                            }
                            updateMountPointInfo();
                            return;
                        }

                        // Case sensitive sort, show dir first, show hidden, system, symlink files
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_CASE_SENSITIVE_SORT.getId()) == 0
                                || key.compareTo(FileManagerSettings.
                                SETTINGS_SHOW_DIRS_FIRST.getId()) == 0
                                || key.compareTo(FileManagerSettings.
                                SETTINGS_SHOW_HIDDEN.getId()) == 0
                                || key.compareTo(FileManagerSettings.
                                SETTINGS_SHOW_SYSTEM.getId()) == 0
                                || key.compareTo(FileManagerSettings.
                                SETTINGS_SHOW_SYMLINKS.getId()) == 0) {
                            getCurrentNavigationView().refresh();
                            return;
                        }

                        // Display thumbs
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_DISPLAY_THUMBS.getId()) == 0) {
                            // Clean the icon cache applying the current theme
                            applyTheme();
                            return;
                        }

                        // Use flinger
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_USE_FLINGER.getId()) == 0) {
                            boolean useFlinger =
                                    Preferences.getSharedPreferences().getBoolean(
                                            FileManagerSettings.SETTINGS_USE_FLINGER.getId(),
                                            (Boolean) FileManagerSettings.
                                                    SETTINGS_USE_FLINGER.
                                                    getDefaultValue());
                            getCurrentNavigationView().setUseFlinger(useFlinger);
                            return;
                        }

                        // Access mode
                        if (key.compareTo(FileManagerSettings.
                                        SETTINGS_ACCESS_MODE.getId()) == 0) {
                            // Is it necessary to create or exit of the ChRooted?
                            boolean chRooted =
                                    FileManagerApplication.
                                            getAccessMode().compareTo(AccessMode.SAFE) == 0;
                            if (chRooted != NavigationFragment.this.mChRooted) {
                                if (chRooted) {
                                    createChRooted();
                                } else {
                                    exitChRooted();
                                }
                            }
                        }

                        // Restricted access
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS.getId()) == 0) {
                            if (AndroidHelper.isSecondaryUser(context)) {
                                try {
                                    Preferences.savePreference(
                                            FileManagerSettings.SETTINGS_ACCESS_MODE,
                                            AccessMode.SAFE, true);
                                } catch (Throwable ex) {
                                    Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
                                }
                                ConsoleBuilder.changeToNonPrivilegedConsole(context);
                                createChRooted();
                            }
                        }

                        // Filetime format mode
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_FILETIME_FORMAT_MODE.getId()) == 0) {
                            // Refresh the data
                            synchronized (FileHelper.DATETIME_SYNC) {
                                FileHelper.sReloadDateTimeFormats = true;
                                getCurrentNavigationView().refresh();
                            }
                        }
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_FILE_CHANGED) == 0) {
                    // Retrieve the file that was changed
                    String file =
                            intent.getStringExtra(FileManagerSettings.EXTRA_FILE_CHANGED_KEY);
                    try {
                        FileSystemObject fso = CommandHelper.getFileInfo(context, file, null);
                        if (fso != null) {
                            getCurrentNavigationView().refresh(fso);
                        }
                    } catch (Exception e) {
                        ExceptionUtil.translateException(context, e, true, false);
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();

                } else if (intent.getAction().compareTo(Intent.ACTION_TIME_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_DATE_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_TIMEZONE_CHANGED) == 0) {
                    // Refresh the data
                    synchronized (FileHelper.DATETIME_SYNC) {
                        FileHelper.sReloadDateTimeFormats = true;
                        NavigationFragment.this.getCurrentNavigationView().refresh();
                    }
                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED) == 0 ||
                            intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ||
                            intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    removeUnmountedHistory();
                    removeUnmountedSelection();
                    if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                        // Check if current path is within unmounted media
                        String path = getCurrentNavigationView().getCurrentDir();
                        final String volumeName =
                                StorageHelper.getStorageVolumeNameIfUnMounted(context, path);
                        if (!TextUtils.isEmpty(volumeName)) {
                            if (mOnGoHomeRequestListener != null) {
                                // Go back to last valid view
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String format = getString(
                                                R.string.snackbar_storage_volume_unmounted);
                                        String message = String.format(format, volumeName);
                                        mOnGoHomeRequestListener.onGoHomeRequested(message);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    };

    /**
     * @hide
     */
    NavigationView[] mNavigationViews;
    private List<History> mHistory;

    private int mCurrentNavigationView;

    private SelectionView mSelectionBar;

    private Dialog mActiveDialog = null;

    private int mOrientation;

    private FileSystemInfoTask mFileSystemInfoTask;
    private MountPoint mMountPoint;
    private DiskUsage mDiskUsage;
    private int mFreeDiskSpaceWarningLevel;

    /**
     * @hide
     */
    public boolean mChRooted;

    /**
     * @hide
     */
    Handler mHandler;

    View mView;
    LayoutInflater mLayoutInflater;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mLayoutInflater = inflater;

        if (DEBUG) {
            Log.d(TAG, "NavigationFragment.onCreate"); //$NON-NLS-1$
        }

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileManagerSettings.INTENT_SETTING_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_FILE_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_THEME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
        getActivity().registerReceiver(this.mNotificationReceiver, filter);

        // This filter needs the file data scheme, so it must be defined separately.
        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        newFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        newFilter.addDataScheme(ContentResolver.SCHEME_FILE);
        getActivity().registerReceiver(mNotificationReceiver, newFilter);

        // Set the theme before setContentView
        ThemeManager.Theme theme = ThemeManager.getCurrentTheme(getActivity());
        theme.setBaseThemeNoActionBar(getActivity());

        //Set the main layout of the activity
        mView = inflater.inflate(R.layout.nav_fragment, container, false);

        //Initialize activity
        init();

        initNavigationViews();

        mToolBar = (Toolbar) mView.findViewById(R.id.material_toolbar);
        ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
        actionBarActivity.setSupportActionBar(mToolBar);
        actionBarActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeButtonEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBarActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Initialize action bars
        initTitleActionBar();
        initSelectionBar();

        // Apply the theme
        applyTheme();

        this.mHandler = new Handler();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Initialize console
                initConsole();

                //Initialize navigation
                int cc = NavigationFragment.this.mNavigationViews.length;
                for (int i = 0; i < cc; i++) {
                    initNavigation(i, false, getActivity().getIntent());
                }
            }
        });

        // Adjust layout (only when start on landscape mode)
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onLayoutChanged();
        }
        this.mOrientation = orientation;

        setHasOptionsMenu(true);

        return mView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.navigation_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        fileActions(item, null);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSearchView.getVisibility() == View.VISIBLE) {
            closeSearch();
        }

        // Check restrictions
        if (!FileManagerApplication.checkRestrictSecondaryUsersAccess(getActivity(), mChRooted)) {
            return;
        }

        // Check that the current dir is mounted (for virtual filesystems)
        String curDir = mNavigationViews[mCurrentNavigationView].getCurrentDir();
        if (curDir != null) {
            VirtualMountPointConsole vc = VirtualMountPointConsole.getVirtualConsoleForPath(
                    mNavigationViews[mCurrentNavigationView].getCurrentDir());
            if (vc != null && !vc.isMounted()) {
                removeUnmountedHistory();
                removeUnmountedSelection();

                Intent intent = new Intent();
                intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                initNavigation(NavigationFragment.this.mCurrentNavigationView, false, intent);
            }
        }

        attachNavigationViewListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onLayoutChanged();
    }

    @Override
    public void onPause() {
        super.onPause();

        removeNavigationViewListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "NavigationFragment.onDestroy"); //$NON-NLS-1$
        }

        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
        }

        if (mFileSystemInfoTask != null) {
            mFileSystemInfoTask.cancel(true);
            mFileSystemInfoTask = null;
        }

        recycle();

        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyView() {
        if (DEBUG) {
            Log.d(TAG, "NavigationFragment.onDestroyView"); //$NON-NLS-1$
        }

        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        super.onDestroyView();
    }

    /**
     * Method that returns the current navigation view.
     *
     * @return NavigationView The current navigation view
     */
    public NavigationView getCurrentNavigationView() {
        return getNavigationView(this.mCurrentNavigationView);
    }

    /**
     * Method that returns the current navigation view.
     *
     * @param viewId The view to return
     * @return NavigationView The current navigation view
     */
    public NavigationView getNavigationView(int viewId) {
        if (this.mNavigationViews == null) return null;
        return this.mNavigationViews[viewId];
    }

    /**
     * Method that initializes the activity.
     */
    private void init() {
        this.mHistory = new ArrayList<History>();
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Inflate the view and associate breadcrumb
        mTitleLayout = mLayoutInflater.inflate(
                R.layout.navigation_view_customtitle, null, false);

        //Configure the action bar options
        mToolBar.addView(mTitleLayout);
    }

    /**
     * Method that initializes the selectionbar of the activity.
     */
    private void initSelectionBar() {
        this.mSelectionBar = (SelectionView) mView.findViewById(R.id.navigation_selectionbar);
        this.mSelectionBar.setMenuClickListener(this);
    }

    /**
     * Method that initializes the navigation views of the activity
     */
    private void initNavigationViews() {
        //Get the navigation views (wishlist: multiple view; for now only one view)
        this.mNavigationViews = new NavigationView[1];
        this.mCurrentNavigationView = 0;
        //- 0
        this.mNavigationViews[0] = (NavigationView) mView.findViewById(R.id.navigation_view);
        this.mNavigationViews[0].setId(0);
    }

    /**
     * Method that adds listeners for the navigation views of the activity
     */
    private void attachNavigationViewListeners() {
        this.mNavigationViews[0].setOnHistoryListener(this);
        this.mNavigationViews[0].setOnNavigationSelectionChangedListener(this);
        this.mNavigationViews[0].setOnNavigationOnRequestMenuListener(this);
        this.mNavigationViews[0].setOnDirectoryChangedListener(this);
        this.mNavigationViews[0].setOnBackRequestListener(mOnBackRequestListener);
    }

    /**
     * Method that removes listeners for the navigation views of the activity
     */
    private void removeNavigationViewListeners() {
        this.mNavigationViews[0].setOnHistoryListener(null);
        this.mNavigationViews[0].setOnNavigationSelectionChangedListener(null);
        this.mNavigationViews[0].setOnNavigationOnRequestMenuListener(null);
        this.mNavigationViews[0].setOnDirectoryChangedListener(null);
        this.mNavigationViews[0].setOnBackRequestListener(null);
    }

    /**
     * Method that initialize the console
     * @hide
     */
    void initConsole() {
        //Create the default console (from the preferences)
        try {
            Console console = ConsoleBuilder.getConsole(getActivity());
            if (console == null) {
                throw new ConsoleAllocException("console == null"); //$NON-NLS-1$
            }
        } catch (Throwable ex) {
            if (!NavigationFragment.this.mChRooted) {
                //Show exception and exit
                Log.e(TAG, getString(R.string.msgs_cant_create_console), ex);
                // We don't have any console
                // Show exception and exit
                DialogHelper.showToast(
                        getActivity(),
                        R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
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
    void initNavigation(final int viewId, final boolean restore, final Intent intent) {
        final NavigationView navigationView = getNavigationView(viewId);
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
    void applyInitialDir(final NavigationView navigationView, final Intent intent) {
        //Load the user-defined initial directory
        String initialDir =
                Preferences.getSharedPreferences().getString(
                    FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                    (String)FileManagerSettings.
                        SETTINGS_INITIAL_DIR.getDefaultValue());

        // Check if request navigation to directory (use as default), and
        // ensure chrooted and absolute path
        String navigateTo = intent.getStringExtra(EXTRA_NAVIGATE_TO);
        if (navigateTo != null && navigateTo.length() > 0) {
            initialDir = navigateTo;
        }

        // Add to history
        final boolean addToHistory = intent.getBooleanExtra(EXTRA_ADD_TO_HISTORY, true);

        if (this.mChRooted) {
            // Initial directory is the first external sdcard (sdcard, emmc, usb, ...)
            if (!StorageHelper.isPathInStorageVolume(initialDir)) {
                StorageVolume[] volumes =
                        StorageHelper.getStorageVolumes(getActivity(), false);
                if (volumes != null && volumes.length > 0) {
                    initialDir = volumes[0].getPath();
                    //Ensure that initial directory is an absolute directory
                    initialDir = FileHelper.getAbsPath(initialDir);
                } else {
                    // Show exception and exit
                    DialogHelper.showToast(
                            getActivity(),
                            R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                    exit();
                    return;
                }
            }
        } else {
            boolean storageProvider = StorageApiConsole.getStorageApiConsoleForPath(initialDir)
                    != null;
            //Ensure that initial directory is an absolute directory
            final String userInitialDir = initialDir;
            if (!storageProvider) {
                initialDir = FileHelper.getAbsPath(initialDir);
            }
            final String absInitialDir = initialDir;
            File f = new File(initialDir);
            boolean exists = f.exists() || storageProvider;
            if (!exists) {
                // Fix for /data/media/0. Libcore doesn't detect it correctly.
                try {
                    exists = CommandHelper.getFileInfo(getActivity(),
                            initialDir, false, null) != null;
                } catch (InsufficientPermissionsException ipex) {
                    ExceptionUtil.translateException(
                            getActivity(), ipex, false, true,
                            new ExceptionUtil.OnRelaunchCommandResult() {
                                @Override
                                public void onSuccess() {
                                    navigationView.changeCurrentDir(absInitialDir, addToHistory);
                                }
                                @Override
                                public void onFailed(Throwable cause) {
                                    showInitialInvalidDirectoryMsg(userInitialDir);
                                    navigationView.changeCurrentDir(
                                            FileHelper.ROOT_DIRECTORY,
                                            addToHistory);
                                }
                                @Override
                                public void onCancelled() {
                                    showInitialInvalidDirectoryMsg(userInitialDir);
                                    navigationView.changeCurrentDir(
                                            FileHelper.ROOT_DIRECTORY,
                                            addToHistory);
                                }
                            });

                    // Asynchronous mode
                    return;
                } catch (Exception ex) {
                    // We are not interested in other exceptions
                    ExceptionUtil.translateException(getActivity(), ex, true, false);
                }

                // Check again the initial directory
                if (!exists) {
                    showInitialInvalidDirectoryMsg(userInitialDir);
                    initialDir = FileHelper.ROOT_DIRECTORY;
                }

                // Weird, but we have a valid initial directory
            }
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
        DialogHelper.showToast(
                getActivity(),
                getString(
                        R.string.msgs_settings_invalid_initial_directory,
                        initialDir),
                Toast.LENGTH_SHORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_REQUEST_SETTINGS) {
            return;
        }

        if (data != null) {
            switch (requestCode) {
                case INTENT_REQUEST_SEARCH:
                    if (resultCode == Activity.RESULT_OK) {
                        //Change directory?
                        Bundle bundle = data.getExtras();
                        if (bundle != null) {
                            FileSystemObject fso = (FileSystemObject) bundle.getSerializable(
                                    EXTRA_SEARCH_ENTRY_SELECTION);
                            SearchInfoParcelable searchInfo =
                                    bundle.getParcelable(EXTRA_SEARCH_LAST_SEARCH_DATA);
                            if (fso != null) {
                                //Goto to new directory
                                getCurrentNavigationView().open(fso, searchInfo);
                            }
                        }
                    } else if (resultCode == Activity.RESULT_CANCELED) {
                        SearchInfoParcelable searchInfo =
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
                                    mView,
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
                                    mView,
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
    public void onNewHistory(HistoryNavigable navigable) {
        //Recollect information about current status
        History history = new History(this.mHistory.size(), navigable);
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
        if (o instanceof FileSystemObject) {
            // Refresh only the item
            this.getCurrentNavigationView().refresh((FileSystemObject) o);
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
    public void onRequestRemove(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Remove from view
            this.getCurrentNavigationView().removeItem((FileSystemObject)o);

            //Remove from history
            removeFromHistory((FileSystemObject) o);
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

    private List<FileSystemObject> mCurrentSelection;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectionChanged(NavigationView navView, List<FileSystemObject> selectedItems) {
        mCurrentSelection = selectedItems;
        this.mSelectionBar.setSelection(selectedItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestMenu(NavigationView navView, FileSystemObject item) {
        // Show the actions dialog
        openActionsDialog(item, false);
    }

    /**
     * Updates the {@link FileManagerSettings} to the value passed in and refreshes the view
     *
     * @param setting {@link FileManagerSettings} to modify
     * @param value The value to set the setting to
     */
    public void updateSetting(FileManagerSettings setting, final int value) {
        try {
            if (setting.compareTo(FileManagerSettings.SETTINGS_LAYOUT_MODE) == 0) {
                //Need to change the layout
                getCurrentNavigationView().changeViewMode(
                        NavigationLayoutMode.fromId(value));
            } else {
                //Save and refresh
                if (setting.getDefaultValue() instanceof Enum<?>) {
                    //Enumeration
                    Preferences.savePreference(setting, new ObjectIdentifier() {
                        @Override
                        public int getId() {
                            return value;
                        }
                    }, false);
                } else {
                    //Boolean
                    boolean newval =
                            Preferences.getSharedPreferences().
                                    getBoolean(
                                            setting.getId(),
                                            (Boolean) setting.getDefaultValue());
                    Preferences.savePreference(setting, !newval, false);
                }
                getCurrentNavigationView().refresh();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying navigation option", e); //$NON-NLS-1$
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DialogHelper.showToast(
                            getActivity(),
                            R.string.msgs_settings_save_failure, Toast.LENGTH_SHORT);
                }
            });

        }
    }

    /**
     * Method that show the information of a filesystem mount point.
     *
     * @param mp The mount point info
     * @param du The disk usage of the mount point
     */
    public void showMountPointInfo(MountPoint mp, DiskUsage du) {
        //Has mount point info?
        if (mp == null) {
            //There is no information
            AlertDialog alert =
                    DialogHelper.createWarningDialog(
                            getActivity(),
                            R.string.filesystem_info_warning_title,
                            R.string.filesystem_info_warning_msg);
            DialogHelper.delegateDialogShow(getActivity(), alert);
            return;
        }

        //Show a the filesystem info dialog
        FilesystemInfoDialog dialog = new FilesystemInfoDialog(getActivity(), mp, du);
        dialog.setOnMountListener(new OnMountListener() {
            @Override
            public void onRemount(MountPoint mountPoint) {
                //Update the statistics of breadcrumb, only if mount point is the same
                Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                if (breadcrumb.getMountPointInfo().compareTo(mountPoint) == 0) {
                    breadcrumb.updateMountPointInfo();
                    updateMountPointInfo();
                }
                if (mountPoint.isSecure()) {
                    // Secure mountpoints only can be unmount, so we need to move the navigation
                    // to a secure storage (do not add to history)
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                    initNavigation(NavigationFragment.this.mCurrentNavigationView, false, intent);
                }
            }
        });
        dialog.show();
    }

    private void updateMountPointInfo() {
        //Cancel the current execution (if any) and launch again
        if (mFileSystemInfoTask != null) {
            mFileSystemInfoTask.cancel(true);
        }

        mFileSystemInfoTask = new FileSystemInfoTask(this, mFreeDiskSpaceWarningLevel);
        mFileSystemInfoTask.execute(getCurrentNavigationView().getCurrentDir());
    }

    public void setMountPoint(MountPoint mp) {
        mMountPoint = mp;
    }

    public void setDiskUsage(DiskUsage ds) {
        mDiskUsage = ds;
    }

    @Override
    public void startActivity(Intent intent) {
        // check if search intent
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationView().getCurrentDir());
        }

        super.startActivity(intent);
    }

    /**
     * Method that returns the history size.
     */
    private void clearHistory() {
        this.mHistory.clear();
    }

    /**
     * Method that navigates to the passed history reference.
     *
     * @param history The history reference
     * @return boolean A problem occurs while navigate
     */
    public synchronized boolean navigateToHistory(History history) {
        try {
            //Navigate to item. Check what kind of history is
            if (history.getItem() instanceof NavigationViewInfoParcelable) {
                //Navigation
                NavigationViewInfoParcelable info =
                        (NavigationViewInfoParcelable)history.getItem();
                int viewId = info.getId();
                NavigationView view = getNavigationView(viewId);
                // Selected items must not be restored from on history navigation
                info.setSelectedFiles(view.getSelectedFiles());
                if (!view.onRestoreState(info)) {
                    return true;
                }

            } else if (history.getItem() instanceof SearchInfoParcelable) {
                //Search (open search with the search results)
                SearchInfoParcelable info = (SearchInfoParcelable)history.getItem();
                Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
                searchIntent.setAction(SearchActivity.ACTION_RESTORE);
                searchIntent.putExtra(SearchActivity.EXTRA_SEARCH_RESTORE, (Parcelable)info);
                startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            } else {
                //The type is unknown
                throw new IllegalArgumentException("Unknown history type"); //$NON-NLS-1$
            }

            //Remove the old history
            int cc = mHistory.lastIndexOf(history);
            for (int i = this.mHistory.size() - 1; i >= cc; i--) {
                this.mHistory.remove(i);
            }

            return true;

        } catch (Throwable ex) {
            if (history != null) {
                Log.e(TAG,
                        String.format("Failed to navigate to history %d: %s", //$NON-NLS-1$
                                history.getPosition(),
                                history.getItem().getTitle()), ex);
            } else {
                Log.e(TAG,
                        String.format("Failed to navigate to history: null", ex)); //$NON-NLS-1$
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DialogHelper.showToast(
                            getActivity(),
                            R.string.msgs_history_unknown, Toast.LENGTH_LONG);
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
            History h = this.mHistory.get(this.mHistory.size() - 1);
            if (h.getItem() instanceof NavigationViewInfoParcelable) {
                // Verify that the path exists
                String path = ((NavigationViewInfoParcelable)h.getItem()).getCurrentDir();

                try {
                    boolean storageProvider = StorageApiConsole.getStorageApiConsoleForPath(path)
                            != null;
                    if (storageProvider) {
                        break;
                    }
                    FileSystemObject info = CommandHelper.getFileInfo(getActivity(), path, null);
                    if (info != null) {
                        break;
                    }
                    this.mHistory.remove(this.mHistory.size() - 1);
                } catch (Exception e) {
                    ExceptionUtil.translateException(getActivity(), e, true, false);
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
        return false;
    }

    /**
     * Method that opens the actions dialog
     *
     * @param fso The {@link FileSystemObject}
     * @param global If the menu to display is the one with global actions
     */
    public void openActionsDialog(FileSystemObject fso, final boolean global) {
        if (fso == null) {
            fso = getCurrentNavigationView().getCurrentFso();
        }

        final NavigationFragment backRef = this;
        final OnRequestRefreshListener onRequestRefreshListener = this;

        // Show the dialog
        ActionsDialog dialog =
                new ActionsDialog(getActivity(), backRef, fso, global, false);
        dialog.setOnRequestRefreshListener(onRequestRefreshListener);
        dialog.setOnSelectionListener(getCurrentNavigationView());
        dialog.show();
    }

    public void toggleSearch() {
        if (mSearchView.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else {
            openSearch();
        }
    }
    /**
     * Method that opens the search view.
     *
     * @hide
     */
    public void openSearch() {
        mSearchView.setVisibility(View.VISIBLE);
        mSearchView.onActionViewExpanded();
        mTitleLayout.findViewById(R.id.navigation_title_landscape_holder)
                .setVisibility(View.GONE);
    }

    void closeSearch() {
        mSearchView.setVisibility(View.GONE);
        mSearchView.onActionViewCollapsed();
        mTitleLayout.findViewById(R.id.navigation_title_landscape_holder)
                .setVisibility(View.VISIBLE);
    }

    /**
     * Method that remove the {@link FileSystemObject} from the history
     */
    private void removeFromHistory(FileSystemObject fso) {
        if (this.mHistory != null) {
            int cc = this.mHistory.size() - 1;
            for (int i = cc; i >= 0 ; i--) {
                History history = this.mHistory.get(i);
                if (history.getItem() instanceof NavigationViewInfoParcelable) {
                    String p0 = fso.getFullPath();
                    String p1 = ((NavigationViewInfoParcelable) history.getItem()).getCurrentDir();
                    if (p0.compareTo(p1) == 0) {
                        this.mHistory.remove(i);
                    }
                }
            }
        }
    }

    /**
     * Method that ask the user to change the access mode prior to crash.
     * @hide
     */
    void askOrExit() {
        //Show a dialog asking the user
        AlertDialog dialog =
            DialogHelper.createYesNoDialog(
                getActivity(),
                R.string.msgs_change_to_prompt_access_mode_title,
                R.string.msgs_change_to_prompt_access_mode_msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface alertDialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            // We don't have any console
                            // Show exception and exit
                            DialogHelper.showToast(
                                    getActivity(),
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                            return;
                        }

                        // Ok. Now try to change to prompt mode. Any crash
                        // here is a fatal error. We won't have any console to operate.
                        try {
                            // Change console
                            ConsoleBuilder.changeToNonPrivilegedConsole(getActivity());

                            // Save preferences
                            Preferences.savePreference(
                                    FileManagerSettings.SETTINGS_ACCESS_MODE,
                                    AccessMode.PROMPT, true);

                        } catch (Exception e) {
                            // Displays an exception and exit
                            Log.e(TAG, getString(R.string.msgs_cant_create_console), e);
                            DialogHelper.showToast(
                                    getActivity(),
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                        }
                    }
                });
        DialogHelper.delegateDialogShow(getActivity(), dialog);
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
        for (NavigationView mNavigationView : this.mNavigationViews) {
            mNavigationView.recycle();
        }
        try {
            FileManagerApplication.destroyBackgroundConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            ConsoleBuilder.destroyConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
    }

    /**
     * Method that reconfigures the layout for better fit in portrait and landscape modes
     */
    private void onLayoutChanged() {
        // Apply only when the orientation was changed
        int orientation = getResources().getConfiguration().orientation;
        if (this.mOrientation == orientation) return;
        this.mOrientation = orientation;
    }

    /**
     * Method that removes all the history items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedHistory() {
        int cc = mHistory.size() - 1;
        for (int i = cc; i >= 0; i--) {
            History history = mHistory.get(i);
            if (history.getItem() instanceof NavigationViewInfoParcelable) {
                NavigationViewInfoParcelable navigableInfo =
                        ((NavigationViewInfoParcelable) history.getItem());
                VirtualMountPointConsole vc =
                        VirtualMountPointConsole.getVirtualConsoleForPath(
                                navigableInfo.getCurrentDir());
                if (vc != null && !vc.isMounted()) {
                    mHistory.remove(i);
                }
            }
        }
    }

    /**
     * Method that removes all the selection items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedSelection() {
        for (NavigationView view : mNavigationViews) {
            view.removeUnmountedSelection();
        }
        mSelectionBar.setSelection(getNavigationView(mCurrentNavigationView).getSelectedFiles());
    }

    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        //int orientation = getResources().getConfiguration().orientation;
        //Theme theme = ThemeManager.getCurrentTheme(getActivity());
        applyTabTheme();

        //- Layout
        View navLayout = mView.findViewById(R.id.navigation_layout);
        navLayout.setBackgroundResource(R.drawable.background_drawable);

        // Hackery to theme search view
        mSearchView = (SearchView) mTitleLayout.findViewById(R.id.navigation_search_bar);
        int searchPlateId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = mSearchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            int searchTextId = searchPlate.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
            if (searchText != null) {
                searchText.setTextColor(Color.WHITE);
                searchText.setHintTextColor(Color.WHITE);
            }

            int magId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            ImageView magImage = (ImageView) mSearchView.findViewById(magId);
            if (magImage != null) {
                magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            }
        }

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context
                .SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity()
                .getComponentName()));
        mSearchView.setIconifiedByDefault(false);

        //- SelectionBar
        // sick themes yo
        //View v = mView.findViewById(R.id.navigation_selectionbar);
        //theme.setBackgroundDrawable(getActivity(), v, "selectionbar_drawable"); //$NON-NLS-1$
        //v = mView.findViewById(R.id.ab_selection_done);
        //theme.setImageDrawable(getActivity(),
        //        (ImageView)v, "ab_selection_done_drawable"); //$NON-NLS-1$
        //v = mView.findViewById(R.id.navigation_status_selection_label);
        //theme.setTextColor(getActivity(), (TextView)v, "text_color"); //$NON-NLS-1$
    }

    /**
     * Method that applies the current theme to the tab host
     */
    private void applyTabTheme() {
        // Apply the theme
        ThemeManager.getCurrentTheme(getActivity());
    }

    /*
     * Method that sets the listener for back requests
     *
     * @param onBackRequestListener The listener reference
     */
    public void setOnBackRequestListener(OnBackRequestListener onBackRequestListener) {
        mOnBackRequestListener = onBackRequestListener;
    }

    /*
     * Method that sets the listener for go home requests
     *
     * @param onGoHomeRequestListener The listener reference
     */
    public void setOnGoHomeRequestListener(OnGoHomeRequestListener onGoHomeRequestListener) {
        mOnGoHomeRequestListener = onGoHomeRequestListener;
    }

    /**
     * Method that sets the listener for directory changes
     *
     * @param onDirectoryChangedListener The listener reference
     */
    public void setOnDirectoryChangedListener(
            OnDirectoryChangedListener onDirectoryChangedListener) {
        mOnDirectoryChangedListener = onDirectoryChangedListener;
    }


    @Override
    public void onDirectoryChanged(FileSystemObject item) {
        MainActivity mainActivity = (MainActivity) getActivity();
        TextView title = (TextView) mTitleLayout.findViewById(R.id.drawer_title);
        title.setText(item.getName());

        int foregroundColor = getResources().getColor(R.color.status_bar_foreground_color);
        int backgroundColor = mainActivity.getColorForPath(
                getCurrentNavigationView().getCurrentDir());
        int statusBarColor = ColorUtils.compositeColors(foregroundColor, backgroundColor);
        mainActivity.getWindow().setStatusBarColor(statusBarColor);

        mToolBar.setBackgroundColor(backgroundColor);
        getCurrentNavigationView().setPrimaryColor(backgroundColor);

        updateMountPointInfo();

        if (mOnDirectoryChangedListener != null) {
            mOnDirectoryChangedListener.onDirectoryChanged(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        if (mSelectionBar != null && mCurrentSelection != null) {
            fileActions(menuItem, mCurrentSelection);
            return true;
        }

        return false;
    }

    /**
     * Method that show a new dialog for input a name for an existing fso.
     *
     * @param menuItem The item menu associated
     * @param fso The file system object
     * @param allowFsoName If allow that the name of the fso will be returned
     */
    private void showFsoInputNameDialog(
            final MenuItem menuItem, final FileSystemObject fso, final boolean allowFsoName) {

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        getActivity(),
                        mCurrentSelection,
                        fso,
                        allowFsoName,
                        menuItem.getTitle().toString());
        inputNameDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                /// TODO: turn down for what
            }
        });
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                String name = inputNameDialog.getName();
                switch (menuItem.getItemId()) {
                    case R.id.mnu_actions_rename:
                    case R.id.mnu_actions_rename_selection:
                        // Rename the fso
                        if (getCurrentNavigationView() != null) {
                            CopyMoveActionPolicy.renameFileSystemObject(
                                    getActivity(),
                                    getCurrentNavigationView(),
                                    inputNameDialog.mFso,
                                    name,
                                    getCurrentNavigationView(),
                                    getCurrentNavigationView());
                        }
                        break;

                    case R.id.mnu_actions_create_link:
                    case R.id.mnu_actions_create_link_global:
                        // Create a link to the fso
                        if (getCurrentNavigationView() != null) {
                            NewActionPolicy.createSymlink(
                                    getActivity(),
                                    inputNameDialog.mFso,
                                    name,
                                    getCurrentNavigationView(),
                                    getCurrentNavigationView());
                        }
                        break;

                    default:
                        break;
                }

            }
        });
        inputNameDialog.show();
    }

    private void fileActions(MenuItem item, List<FileSystemObject> selection) {
        switch (item.getItemId()) {

            // Uncompress
            case R.id.mnu_actions_extract:
                // make this recursivly uncompress
                CompressActionPolicy.uncompress(
                        getActivity(),
                        selection.get(0),
                        this);
                break;

            // Compress
            case R.id.mnu_actions_compress:
                if (getCurrentNavigationView() != null) {
                    CompressActionPolicy.compress(
                            getActivity(),
                            selection,
                            getCurrentNavigationView(),
                            this);
                }
                break;

            // Paste
            case R.id.mnu_actions_paste:
                if (getCurrentNavigationView() != null) {
                    // Select destination
                    Intent intent = new Intent(INTENT_FOLDER_SELECT);
                    intent.putExtra(EXTRA_ACTION, COPY.ordinal());
                    startActivityForResult(intent, INTENT_REQUEST_COPY);
                }
                break;

            // Move
            case R.id.mnu_actions_move:
                if (getCurrentNavigationView() != null) {
                    // Select destination
                    Intent intent = new Intent(INTENT_FOLDER_SELECT);
                    intent.putExtra(EXTRA_ACTION, MOVE.ordinal());
                    startActivityForResult(intent, INTENT_REQUEST_MOVE);
                }
                break;

            // Add shortcut
            case R.id.mnu_actions_add_shortcut:
                IntentsActionPolicy.createShortcut(getActivity(), selection.get(0));
                break;

            // Compute checksum
            case R.id.mnu_actions_compute_checksum:
                InfoActionPolicy.showComputeChecksumDialog(getActivity(), selection.get(0));
                break;

            // Print
            case R.id.mnu_actions_print:
                PrintActionPolicy.printDocument(getActivity(), selection.get(0));
                break;

            // Create copy
            case R.id.mnu_actions_create_copy:
                // copy all the things
                for (FileSystemObject fso : selection) {
                    CopyMoveActionPolicy.createCopyFileSystemObject(
                            getActivity(),
                            getCurrentNavigationView(),
                            fso,
                            getCurrentNavigationView(),
                            this);
                }
                break;

            // Delete
            case R.id.mnu_actions_delete:
                DeleteActionPolicy.removeFileSystemObjects(
                        getActivity(),
                        getCurrentNavigationView(),
                        selection,
                        getCurrentNavigationView(),
                        getCurrentNavigationView(),
                        null);
                break;

            // Open
            case R.id.mnu_actions_open:
                IntentsActionPolicy.openFileSystemObject(
                        getActivity(), getCurrentNavigationView(), selection.get(0), false, null, null);
                break;

            // Open with
            case R.id.mnu_actions_open_with:
                IntentsActionPolicy.openFileSystemObject(
                        getActivity(), getCurrentNavigationView(), selection.get(0), true, null, null);
                break;

            // Execute
            case R.id.mnu_actions_execute:
                ExecutionActionPolicy.execute(getActivity(), selection.get(0));
                break;

            // Send
            case R.id.mnu_actions_send:
                IntentsActionPolicy.sendMultipleFileSystemObject(
                        getActivity(), selection, null, null);
                break;

            // Rename
            case R.id.mnu_actions_rename:
                if (getCurrentNavigationView() != null) {
                    showFsoInputNameDialog(item, selection.get(0), false);
                }
                break;

            // select all
            case R.id.mnu_actions_select_all:
                getCurrentNavigationView().onSelectAllVisibleItems();
                break;

            // refresh
            case R.id.mnu_actions_refresh:
                onRequestRefresh(null, false);
                break;

            // search
            case R.id.mnu_actions_search:
                toggleSearch();
                break;

            // sort
            case R.id.mnu_actions_sort:
                SortViewOptions.createSortDialog(getActivity(),
                        FileManagerSettings.SETTINGS_SORT_MODE,
                        new SortViewOptions.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, int result) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    if (PreferenceHelper.getIntPreference(
                                            FileManagerSettings.SETTINGS_SORT_MODE) != result) {
                                        updateSetting(
                                                FileManagerSettings.SETTINGS_SORT_MODE, result);
                                    }
                                }
                            }
                        })
                        .show();
                break;

            // new directory
            case R.id.mnu_actions_new_directory:
                ActionsDialog.showInputNameDialog(getActivity(),
                        getString(R.string.actions_menu_new_directory), item.getItemId(),
                        getCurrentNavigationView().getFiles(), getCurrentNavigationView(), this);
                break;

            // new file
            case R.id.mnu_actions_new_file:
                ActionsDialog.showInputNameDialog(getActivity(),
                        getString(R.string.actions_menu_new_file), item.getItemId(),
                        getCurrentNavigationView().getFiles(), getCurrentNavigationView(), this);
                break;

            // get properties
            case R.id.mnu_actions_properties_current_folder:
                InfoActionPolicy.showPropertiesDialog(getActivity(),
                        getCurrentNavigationView().getCurrentFso(), this);
                break;

            // get file system info
            case R.id.mnu_actions_file_system_info:
                if (mMountPoint != null && mDiskUsage != null) {
                    showMountPointInfo(mMountPoint, mDiskUsage);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.file_system_info_unavailable),
                            Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }
}
