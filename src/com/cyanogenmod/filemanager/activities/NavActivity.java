package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toolbar;
import android.widget.Toast;
import android.os.storage.StorageVolume;
import android.os.Environment;

import com.android.internal.util.XmlUtils;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences;
import com.cyanogenmod.filemanager.adapters.MenuSettingsAdapter;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.VirtualConsole;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.listeners.OnHistoryListener;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.History;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE;
import com.cyanogenmod.filemanager.parcelables.HistoryNavigable;
import com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable;
import com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.Bookmarks;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationLayoutMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog;
import com.cyanogenmod.filemanager.ui.dialogs.InitialDirectoryDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog.OnMountListener;
import com.cyanogenmod.filemanager.ui.widgets.Breadcrumb;
import com.cyanogenmod.filemanager.ui.widgets.ButtonItem;
import com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView;
import com.cyanogenmod.filemanager.ui.widgets.SelectionView;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.BookmarksHelper;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.StorageHelper;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.model.Directory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;

/**
 * Created by bird on 6/3/15.
 */
public class NavActivity extends Activity {

    private static final String TAG = "NavActivity";

    Toolbar mToolBar;
    private FileSystemObjectAdapter mAdapter;
    private ListView mListView;
    private Activity mActivity = this;
    private NAVIGATION_MODE mNavigationMode;

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
     * Constant for extra information about last search data.
     */
    public static final String EXTRA_SEARCH_LAST_SEARCH_DATA =
            "extra_search_last_search_data"; //$NON-NLS-1$

    /**
     * Constant for extra information about selected search entry.
     */
    public static final String EXTRA_SEARCH_ENTRY_SELECTION =
            "extra_search_entry_selection"; //$NON-NLS-1$


    private List<History> mHistory;

    /**
     * @hide
     */
    boolean mChRooted;


    static String MIME_TYPE_LOCALIZED_NAMES[];

    /**
     * The navigation view mode
     * @hide
     */
    public enum NAVIGATION_MODE {
        /**
         * The navigation view acts as a browser, and allow open files itself.
         */
        BROWSABLE,
        /**
         * The navigation view acts as a picker of files
         */
        PICKABLE,
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {

        /*if (DEBUG) {
            android.util.Log.d(TAG, "NavigationActivity.onCreate"); //$NON-NLS-1$
        } */

        //Set the main layout of the activity
        setContentView(R.layout.nav);

        mToolBar = (android.widget.Toolbar) findViewById(R.id.material_toolbar);
        setActionBar(mToolBar);

        //initDrawer();

        // Show welcome message
        //showWelcomeMsg();

        //Initialize activity
        init();

        applyInitialDir();

        //Save state
        super.onCreate(state);
    }

    /**
     * Method that displays a welcome message the first time the user
     * access the application
     */
    private void showWelcomeMsg() {
        boolean firstUse = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_FIRST_USE.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_FIRST_USE.getDefaultValue()).booleanValue());

        if (firstUse && FileManagerApplication.hasShellCommands()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);

            try {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_FIRST_USE, Boolean.FALSE, true);
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * Method that changes the current directory of the view.
     *
     * @param newDir The new directory location
     * @param addToHistory Add the directory to history
     */
    public void changeCurrentDir(final String newDir, boolean addToHistory) {
        changeCurrentDir(newDir, addToHistory, false, false, null, null);
    }

    private void changeCurrentDir(
            final String newDir, final boolean addToHistory,
            final boolean reload, final boolean useCurrent,
            final SearchInfoParcelable searchInfo, final FileSystemObject scrollTo) {
        NavigationTask task = new NavigationTask(useCurrent, addToHistory, reload,
                searchInfo, scrollTo, this);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newDir);
    }

    /**
     * Method invoked when a execution ends.
     *
     * @param files The files obtains from the list
     * @param addToHistory If add path to history
     * @param isNewHistory If is new history
     * @param hasChanged If current directory was changed
     * @param searchInfo The search information (if calling activity is {@link "SearchActivity"})
     * @param newDir The new directory
     * @param scrollTo If not null, then listview must scroll to this item
     * @hide
     */
    void onPostExecuteTask(
            List<FileSystemObject> files, boolean addToHistory, boolean isNewHistory,
            boolean hasChanged, SearchInfoParcelable searchInfo,
            String newDir, final FileSystemObject scrollTo) {

        Log.v(TAG, "Got File List: " + files.size(), new Exception());
        mAdapter = new com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter(
                this,files, R.layout.navigation_view_simple_item, true);

        mListView.setAdapter(mAdapter);
    }

    /**
     * Method that initializes the activity.
     */
    private void init() {
        this.mHistory = new ArrayList<History>();

        // Retrieve the mode
        this.mNavigationMode = NAVIGATION_MODE.BROWSABLE;


        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;
        this.mListView = (ListView) findViewById(R.id.lv_easy_mode);
        mListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> adapter,
                                    android.view.View view,
                                    int position, long l) {
                try {
                    FileSystemObject fso =
                            ((FileSystemObjectAdapter) adapter.getAdapter()).getItem(position);
                    if (fso instanceof ParentDirectory) {
                        changeCurrentDir(fso.getParent(), true, false, false, null, null);
                        return;
                    } else if (fso instanceof Directory) {
                        changeCurrentDir(fso.getFullPath(), true, false, false, null, null);
                        return;
                    } else if (fso instanceof Symlink) {
                        Symlink symlink = (Symlink) fso;
                        if (symlink.getLinkRef() != null && symlink
                                .getLinkRef() instanceof Directory) {
                            changeCurrentDir(
                                    symlink.getLinkRef().getFullPath(), true, false, false, null,
                                    null);
                            return;
                        }

                        // Open the link ref
                        fso = symlink.getLinkRef();
                    }

                    // Open the file (edit or pick)
                    if (mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
                        // Open the file with the preferred registered app
                        IntentsActionPolicy
                                .openFileSystemObject(mActivity, fso, false, null, null);
                    }
                } catch (Throwable ex) {
                    //ExceptionUtil.translateException(getContext(), ex);
                }
            }
        });
        mListView.setOnItemLongClickListener(
                new android.widget.AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                                   long id) {
                        // Different actions depending on user preference

                        // Get the adapter and the fso
                        FileSystemObjectAdapter adapter =
                                ((FileSystemObjectAdapter) parent.getAdapter());
                        if (adapter == null || position < 0 || (position >= adapter.getCount())) {
                            return false;
                        }
                        FileSystemObject fso = adapter.getItem(position);

                        // Parent directory hasn't actions
                        if (fso instanceof ParentDirectory) {
                            return false;
                        }

                        // Pick mode doesn't implements the onlongclick
                        if (mNavigationMode.compareTo(NAVIGATION_MODE.PICKABLE) == 0) {
                            return false;
                        }

                        //onRequestMenu(fso);
                        return true; //Always consume the event
                    }

                }
        );
    }


    /**
     * Method that applies the user-defined initial directory
     *
     * @hide
     */
    void applyInitialDir() {
        //Load the user-defined initial directory
        String initialDir =
                Preferences.getSharedPreferences().getString(
                        FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                        (String)FileManagerSettings.
                                SETTINGS_INITIAL_DIR.getDefaultValue());

        // We cannot navigate to a secure console if it is unmounted. So go to root in that case
        VirtualConsole vc = VirtualMountPointConsole.getVirtualConsoleForPath(initialDir);
        if (vc != null && vc instanceof SecureConsole && !((SecureConsole) vc).isMounted()) {
            initialDir = FileHelper.ROOT_DIRECTORY;
        }

        if (this.mChRooted) {
            // Initial directory is the first external sdcard (sdcard, emmc, usb, ...)
            if (!StorageHelper.isPathInStorageVolume(initialDir)) {
                StorageVolume[] volumes =
                        StorageHelper.getStorageVolumes(this, false);
                if (volumes != null && volumes.length > 0) {
                    initialDir = volumes[0].getPath();
                    int count = volumes.length;
                    for (int i = 0; i < count; i++) {
                        StorageVolume volume = volumes[i];
                        if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(volume.getState())) {
                            initialDir = volume.getPath();
                            break;
                        }
                    }
                    //Ensure that initial directory is an absolute directory
                    initialDir = FileHelper.getAbsPath(initialDir);
                } else {
                    // Show exception and exit
                    DialogHelper.showToast(
                            this,
                            R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                    finish();
                    return;
                }
            }
        } else {
            //Ensure that initial directory is an absolute directory
            final String userInitialDir = initialDir;
            initialDir = FileHelper.getAbsPath(initialDir);
            final String absInitialDir = initialDir;
            File f = new File(initialDir);
            boolean exists = f.exists();
            if (!exists) {
                // Fix for /data/media/0. Libcore doesn't detect it correctly.
                try {
                    exists = CommandHelper.getFileInfo(this, initialDir, false, null) != null;
                } catch (InsufficientPermissionsException ipex) {
                    ExceptionUtil.translateException(
                            this, ipex, false, true, new OnRelaunchCommandResult() {
                                @Override
                                public void onSuccess() {
                                    changeCurrentDir(absInitialDir, false);
                                }
                                @Override
                                public void onFailed(Throwable cause) {
                                    //showInitialInvalidDirectoryMsg(userInitialDir);
                                    changeCurrentDir(FileHelper.ROOT_DIRECTORY,
                                            false);
                                }
                                @Override
                                public void onCancelled() {
                                    //showInitialInvalidDirectoryMsg(userInitialDir);
                                    changeCurrentDir(FileHelper.ROOT_DIRECTORY,
                                            false);
                                }
                            });

                    // Asynchronous mode
                    return;
                } catch (Exception ex) {
                    // We are not interested in other exceptions
                    ExceptionUtil.translateException(this, ex, true, false);
                }

                // Check again the initial directory
                if (!exists) {
                    //showInitialInvalidDirectoryMsg(userInitialDir);
                    initialDir = FileHelper.ROOT_DIRECTORY;
                }

                // Weird, but we have a valid initial directory
            }
        }

        // Change the current directory to the user-defined initial directory
        changeCurrentDir(initialDir, false);
    }


    public void addBookmark(Bookmark b) {
        //stub
    }


}
