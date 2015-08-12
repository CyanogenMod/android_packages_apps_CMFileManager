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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.*;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter;
import com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter.OnSelectionChangedListener;
import com.cyanogenmod.filemanager.console.AuthenticationFailedException;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.listeners.OnHistoryListener;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.listeners.OnSelectionListener;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable;
import com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.DisplayRestrictions;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationLayoutMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.policy.DeleteActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;
import com.cyanogenmod.filemanager.ui.widgets.FlingerListView.OnItemFlingerListener;
import com.cyanogenmod.filemanager.ui.widgets.FlingerListView.OnItemFlingerResponder;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The file manager implementation view (contains the graphical representation and the input
 * management for a file manager; shows the folders/files, the mode view, react touch events,
 * navigate, ...).
 */
public class NavigationView extends RelativeLayout implements
AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
BreadcrumbListener, OnSelectionChangedListener, OnSelectionListener, OnRequestRefreshListener {

    private static final String TAG = "NavigationView"; //$NON-NLS-1$

    /**
     * An interface to communicate selection changes events.
     */
    public interface OnNavigationSelectionChangedListener {
        /**
         * Method invoked when the selection changed.
         *
         * @param navView The navigation view that generate the event
         * @param selectedItems The new selected items
         */
        void onSelectionChanged(NavigationView navView, List<FileSystemObject> selectedItems);
    }

    /**
     * An interface to communicate a request when the user choose a file.
     */
    public interface OnFilePickedListener {
        /**
         * Method invoked when a request when the user choose a file.
         *
         * @param item The item choose
         */
        void onFilePicked(FileSystemObject item);
    }

    /**
     * An interface to communicate a change of the current directory
     */
    public interface OnDirectoryChangedListener {
        /**
         * Method invoked when the current directory changes
         *
         * @param item The newly active directory
         */
        void onDirectoryChanged(FileSystemObject item);
    }

    /**
     * An interface to communicate a request to go back to previous view
     */
    public interface OnBackRequestListener {
        /**
         * Method invoked when a back (previous view) is requested
         *
         */
        void onBackRequested();
    }

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
     * A listener for flinging events from {@link FlingerListView}
     */
    private final OnItemFlingerListener mOnItemFlingerListener = new OnItemFlingerListener() {

        @Override
        public boolean onItemFlingerStart(
                AdapterView<?> parent, View view, int position, long id) {
            try {
                // Response if the item can be removed
                FileSystemObjectAdapter adapter = (FileSystemObjectAdapter)parent.getAdapter();

                // Short circuit to protect OOBE
                if (position < 0 || position >= adapter.getCount()) {
                    return false;
                }

                FileSystemObject fso = adapter.getItem(position);
                if (fso != null) {
                    return !(fso instanceof ParentDirectory);
                }
            } catch (Exception e) {
                ExceptionUtil.translateException(getContext(), e, true, false);
            }
            return false;
        }

        @Override
        public void onItemFlingerEnd(OnItemFlingerResponder responder,
                AdapterView<?> parent, View view, int position, long id) {

            try {
                // Response if the item can be removed
                FileSystemObjectAdapter adapter = (FileSystemObjectAdapter)parent.getAdapter();
                FileSystemObject fso = adapter.getItem(position);
                if (fso != null) {
                    DeleteActionPolicy.removeFileSystemObjects(
                            getContext(),
                            NavigationView.this,
                            fso,
                            NavigationView.this,
                            NavigationView.this,
                            responder);
                    return;
                }

                // Cancels the flinger operation
                responder.cancel();

            } catch (Exception e) {
                ExceptionUtil.translateException(getContext(), e, true, false);
                responder.cancel();
            }
        }
    };

    private class NavigationTask extends AsyncTask<String, Integer, List<FileSystemObject>> {
        private final boolean mUseCurrent;
        private final boolean mAddToHistory;
        private final boolean mReload;
        private boolean mHasChanged;
        private boolean mIsNewHistory;
        private String mNewDirChecked;
        private final SearchInfoParcelable mSearchInfo;
        private final FileSystemObject mScrollTo;
        private FileSystemObject mNewDirFSO;

        public NavigationTask(boolean useCurrent, boolean addToHistory, boolean reload,
                SearchInfoParcelable searchInfo, FileSystemObject scrollTo) {
            super();
            this.mUseCurrent = useCurrent;
            this.mAddToHistory = addToHistory;
            this.mSearchInfo = searchInfo;
            this.mReload = reload;
            this.mScrollTo = scrollTo;
            this.mNewDirFSO = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<FileSystemObject> doInBackground(String... params) {

            if (TextUtils.equals(params[0], FileHelper.ROOTS_LIST)) {
                // If new directory is "Roots list" don't check ChRooted evnironment
                mNewDirChecked = params[0];
            } else {
                // Check navigation security (don't allow to go outside the ChRooted environment if one
                // is created)
                mNewDirChecked = checkChRootedNavigation(params[0]);
            }

            //Check that it is really necessary change the directory
            if (!mReload && NavigationView.this.mCurrentDir != null &&
                    NavigationView.this.mCurrentDir.compareTo(mNewDirChecked) == 0) {
                return null;
            }

            mHasChanged = !(NavigationView.this.mCurrentDir != null &&
                    NavigationView.this.mCurrentDir.compareTo(mNewDirChecked) == 0);
            mIsNewHistory = (NavigationView.this.mCurrentDir != null);

            try {
                //Start of loading data
                if (NavigationView.this.mBreadcrumb != null) {
                    try {
                        NavigationView.this.mBreadcrumb.startLoading();
                    } catch (Throwable ex) {
                        /**NON BLOCK**/
                    }
                }

                List<FileSystemObject> files;
                if (TextUtils.equals(mNewDirChecked, FileHelper.ROOTS_LIST)) {
                    files = StorageHelper.getStorageVolumesFileSystemObjectList(getContext());
                } else {
                    //Get the files, resolve links and apply configuration
                    //(sort, hidden, ...)
                    files = NavigationView.this.mFiles;
                    if (!mUseCurrent) {
                        files = CommandHelper.listFiles(getContext(), mNewDirChecked, null);
                        mNewDirFSO = CommandHelper.getFileInfo(getContext(), mNewDirChecked, null);
                    }
                }
                return files;

            } catch (final ConsoleAllocException e) {
                //Show exception and exists
                NavigationView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        Context ctx = getContext();
                        Log.e(TAG, ctx.getString(
                                R.string.msgs_cant_create_console), e);
                        DialogHelper.showToast(ctx,
                                R.string.msgs_cant_create_console,
                                Toast.LENGTH_LONG);
                        ((Activity)ctx).finish();
                    }
                });

            } catch (Exception ex) {
                //End of loading data
                if (NavigationView.this.mBreadcrumb != null) {
                    try {
                        NavigationView.this.mBreadcrumb.endLoading();
                    } catch (Throwable ex2) {
                        /**NON BLOCK**/
                    }
                }
                if (ex instanceof CancelledOperationException ||
                        ex instanceof AuthenticationFailedException) {
                    return null;
                }

                //Capture exception (attach task, and use listener to do the anim)
                ExceptionUtil.attachAsyncTask(
                        ex,
                        new AsyncTask<Object, Integer, Boolean>() {
                            private List<FileSystemObject> mTaskFiles = null;
                            @Override
                            @SuppressWarnings({
                                "unchecked", "unqualified-field-access"
                            })
                            protected Boolean doInBackground(Object... taskParams) {
                                mTaskFiles = (List<FileSystemObject>)taskParams[0];
                                return Boolean.TRUE;
                            }

                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            protected void onPostExecute(Boolean result) {
                                if (!result.booleanValue()) {
                                    return;
                                }
                                onPostExecuteTask(
                                        mTaskFiles, mAddToHistory, mIsNewHistory, mHasChanged,
                                        mSearchInfo, mNewDirChecked, mNewDirFSO, mScrollTo);
                            }
                        });
                final OnRelaunchCommandResult exListener =
                        new OnRelaunchCommandResult() {
                    @Override
                    public void onSuccess() {
                        done();
                    }
                    @Override
                    public void onFailed(Throwable cause) {
                        done();
                    }
                    @Override
                    public void onCancelled() {
                        done();
                    }
                    private void done() {
                        // Do animation
                        fadeEffect(false);
                    }
                };
                ExceptionUtil.translateException(
                        getContext(), ex, false, true, exListener);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCancelled(List<FileSystemObject> result) {
            onCancelled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(List<FileSystemObject> files) {
            // This means an exception. This method will be recalled then
            if (files != null) {
                // Do animation
                fadeEffect(true);

                onPostExecuteTask(files, mAddToHistory, mIsNewHistory, mHasChanged,
                        mSearchInfo, mNewDirChecked, mNewDirFSO, mScrollTo);


            } else {
                if (TextUtils.isEmpty(mCurrentDir)) {
                    if (mOnBackRequestListener != null) {
                        // Go back to previous view
                        post(new Runnable() {
                            @Override
                            public void run() {
                                mOnBackRequestListener.onBackRequested();
                            }
                        });
                    }
                } else {
                    // Reload current directory
                    changeCurrentDir(mCurrentDir);
                }
            }
        }

        /**
         * Method that performs a fade animation.
         *
         * @param out Fade out (true); Fade in (false)
         */
        void fadeEffect(final boolean out) {
            Activity activity = (Activity)getContext();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Animation fadeAnim = out ?
                            new AlphaAnimation(1, 0) :
                                new AlphaAnimation(0, 1);
                            fadeAnim.setDuration(400L);
                            fadeAnim.setFillAfter(true);
                            fadeAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                            NavigationView.this.startAnimation(fadeAnim);
                }
            });
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
         * @param newDirFSO the new directory in FSO form
         * @param scrollTo If not null, then listview must scroll to this item
         * @hide
         */
        void onPostExecuteTask(
                List<FileSystemObject> files, boolean addToHistory, boolean isNewHistory,
                boolean hasChanged, SearchInfoParcelable searchInfo,
                String newDir, final FileSystemObject newDirFSO, final FileSystemObject scrollTo) {
            try {
                //Check that there is not errors and have some data
                if (files == null) {
                    return;
                }

                if (!TextUtils.equals(FileHelper.ROOTS_LIST, newDir)) {
                    //Apply user preferences
                    files = FileHelper.applyUserPreferences(files, mRestrictions, mChRooted);
                }

                //Remove parent directory if we are in the root of a chrooted environment
                if (mChRooted && StorageHelper.isStorageVolume(newDir) ||
                        TextUtils.equals(newDir, FileHelper.ROOT_DIRECTORY)) {
                    if (files.size() > 0 && files.get(0) instanceof ParentDirectory) {
                        files.remove(0);
                    }
                    if (mNavigationMode.compareTo(NAVIGATION_MODE.PICKABLE) == 0) {
                        files.add(0, new ParentDirectory(FileHelper.ROOTS_LIST));
                    }
                } else if (!TextUtils.equals(FileHelper.ROOTS_LIST, newDir) &&
                        files.size() > 0 && !(files.get(0) instanceof ParentDirectory)) {
                    if (mNavigationMode.compareTo(NAVIGATION_MODE.PICKABLE) == 0) {
                        files.add(0, new ParentDirectory(FileHelper.ROOTS_LIST));
                    }
                }

                //Add to history?
                if (addToHistory && hasChanged && isNewHistory) {
                    if (mOnHistoryListener != null) {
                        //Communicate the need of a history change
                        mOnHistoryListener.onNewHistory(onSaveState());
                    }
                }

                //Load the data
                loadData(files);
                mFiles = files;
                if (searchInfo != null) {
                    searchInfo.setSuccessNavigation(true);
                }

                //Change the breadcrumb
                if (mBreadcrumb != null) {
                    mBreadcrumb.changeBreadcrumbPath(newDir, mChRooted);
                }

                //If scrollTo is null, the position will be set to 0
                scrollTo(scrollTo);

                //The current directory is now the "newDir"
                mCurrentDir = newDir;
                mCurrentFileSystemObject = newDirFSO;
                if (mOnDirectoryChangedListener != null) {
                    FileSystemObject dir = (newDirFSO != null) ?
                            newDirFSO : FileHelper.createFileSystemObject(new File(newDir));
                    mOnDirectoryChangedListener.onDirectoryChanged(dir);
                }
            } finally {
                //If calling activity is search, then save the search history
                if (searchInfo != null) {
                    mOnHistoryListener.onNewHistory(searchInfo);
                }

                //End of loading data
                try {
                    mBreadcrumb.endLoading();
                } catch (Throwable ex) {
                    /**NON BLOCK**/
                }

                fadeEffect(false);

            }
        }

        /**
         * Method that ensures that the user don't go outside the ChRooted environment
         *
         * @param newDir The new directory to navigate to
         * @return String
         */
        private String checkChRootedNavigation(String newDir) {
            // If we aren't in ChRooted environment, then there is nothing to check
            if (!mChRooted) return newDir;

            // Check if the path is owned by one of the storage volumes
            if (!StorageHelper.isPathInStorageVolume(newDir)) {
                StorageVolume[] volumes = StorageHelper.getStorageVolumes(getContext(), false);
                if (volumes != null && volumes.length > 0) {
                    return volumes[0].getPath();
                }
            }
            return newDir;
        }
    }

    private int mId;
    private String mCurrentDir;
    private FileSystemObject mCurrentFileSystemObject;
    private NavigationLayoutMode mCurrentMode;
    /**
     * @hide
     */
    List<FileSystemObject> mFiles;
    private FileSystemObjectAdapter mAdapter;

    private OnHistoryListener mOnHistoryListener;
    private OnNavigationSelectionChangedListener mOnNavigationSelectionChangedListener;
    private OnFilePickedListener mOnFilePickedListener;
    private OnDirectoryChangedListener mOnDirectoryChangedListener;
    private OnBackRequestListener mOnBackRequestListener;

    private boolean mChRooted;

    private NAVIGATION_MODE mNavigationMode;

    // Restrictions
    private Map<DisplayRestrictions, Object> mRestrictions;

    /**
     * @hide
     */
    Breadcrumb mBreadcrumb;

    /**
     * @hide
     */
    AdapterView<?> mAdapterView;

    //The layout for icons mode
    private static final int RESOURCE_MODE_ICONS_LAYOUT = R.layout.navigation_view_icons;
    private static final int RESOURCE_MODE_ICONS_ITEM = R.layout.navigation_view_icons_item;
    //The layout for simple mode
    private static final int RESOURCE_MODE_SIMPLE_LAYOUT = R.layout.navigation_view_simple;
    private static final int RESOURCE_MODE_SIMPLE_ITEM = R.layout.navigation_view_simple_item;
    //The layout for details mode
    private static final int RESOURCE_MODE_DETAILS_LAYOUT = R.layout.navigation_view_details;
    private static final int RESOURCE_MODE_DETAILS_ITEM = R.layout.navigation_view_details_item;

    //The current layout identifier (is shared for all the mode layout)
    private static final int RESOURCE_CURRENT_LAYOUT = R.id.navigation_view_layout;

    /**
     * Constructor of <code>NavigationView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public NavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Navigable);
        try {
            init(a);
        } finally {
            a.recycle();
        }
    }

    /**
     * Constructor of <code>NavigationView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public NavigationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Navigable, defStyle, 0);
        try {
            init(a);
        } finally {
            a.recycle();
        }
    }

    /**
     * Invoked when the instance need to be saved.
     *
     * @return NavigationViewInfoParcelable The serialized info
     */
    public NavigationViewInfoParcelable onSaveState() {
        //Return the persistent the data
        NavigationViewInfoParcelable parcel = new NavigationViewInfoParcelable();
        parcel.setId(this.mId);
        parcel.setCurrentDir(this.mCurrentDir);
        parcel.setCurrentFso(this.mCurrentFileSystemObject);
        parcel.setChRooted(this.mChRooted);
        parcel.setSelectedFiles(this.mAdapter.getSelectedItems());
        parcel.setFiles(this.mFiles);

        int firstVisiblePosition = mAdapterView.getFirstVisiblePosition();
        if (firstVisiblePosition >= 0 && firstVisiblePosition < mAdapter.getCount()) {
            FileSystemObject firstVisible = mAdapter
                    .getItem(firstVisiblePosition);
            parcel.setFirstVisible(firstVisible);
        }

        return parcel;
    }

    /**
     * Invoked when the instance need to be restored.
     *
     * @param info The serialized info
     * @return boolean If can restore
     */
    public boolean onRestoreState(NavigationViewInfoParcelable info) {
        //Restore the data
        this.mId = info.getId();
        this.mCurrentDir = info.getCurrentDir();
        this.mCurrentFileSystemObject = info.getCurrentFso();
        this.mChRooted = info.getChRooted();
        this.mFiles = info.getFiles();
        this.mAdapter.setSelectedItems(info.getSelectedFiles());

        final FileSystemObject firstVisible = info.getFirstVisible();

        //Update the views
        refresh(firstVisible);
        return true;
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view.
     *
     * @param tarray The type array
     */
    private void init(TypedArray tarray) {
        // Retrieve the mode
        this.mNavigationMode = NAVIGATION_MODE.BROWSABLE;
        int mode = tarray.getInteger(
                R.styleable.Navigable_navigation,
                NAVIGATION_MODE.BROWSABLE.ordinal());
        if (mode >= 0 && mode < NAVIGATION_MODE.values().length) {
            this.mNavigationMode = NAVIGATION_MODE.values()[mode];
        }

        // Initialize default restrictions (no restrictions)
        this.mRestrictions = new HashMap<DisplayRestrictions, Object>();

        //Initialize variables
        this.mFiles = new ArrayList<FileSystemObject>();

        // Is ChRooted environment?
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;

        //Retrieve the default configuration
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
            SharedPreferences preferences = Preferences.getSharedPreferences();
            int viewMode = preferences.getInt(
                    FileManagerSettings.SETTINGS_LAYOUT_MODE.getId(),
                    ((ObjectIdentifier)FileManagerSettings.
                            SETTINGS_LAYOUT_MODE.getDefaultValue()).getId());
            changeViewMode(NavigationLayoutMode.fromId(viewMode));
        } else {
            // Pick mode has always a simple layout
            changeViewMode(NavigationLayoutMode.SIMPLE);
        }
    }

    /**
     * Method that returns the display restrictions to apply to this view.
     *
     * @return Map<DisplayRestrictions, Object> The restrictions to apply
     */
    public Map<DisplayRestrictions, Object> getRestrictions() {
        return this.mRestrictions;
    }

    /**
     * Method that sets the display restrictions to apply to this view.
     *
     * @param mRestrictions The restrictions to apply
     */
    public void setRestrictions(Map<DisplayRestrictions, Object> mRestrictions) {
        this.mRestrictions = mRestrictions;
    }

    /**
     * Method that returns the current file list of the navigation view.
     *
     * @return List<FileSystemObject> The current file list of the navigation view
     */
    public List<FileSystemObject> getFiles() {
        if (this.mFiles == null) {
            return null;
        }
        return new ArrayList<FileSystemObject>(this.mFiles);
    }

    /**
     * Method that returns the current file list of the navigation view.
     *
     * @return List<FileSystemObject> The current file list of the navigation view
     */
    public List<FileSystemObject> getSelectedFiles() {
        if (this.mAdapter != null && this.mAdapter.getSelectedItems() != null) {
            return new ArrayList<FileSystemObject>(this.mAdapter.getSelectedItems());
        }
        return null;
    }

    /**
     * Method that returns the breadcrumb associated with this navigation view.
     *
     * @return Breadcrumb The breadcrumb view fragment
     */
    public Breadcrumb getBreadcrumb() {
        return this.mBreadcrumb;
    }

    /**
     * Method that associates the breadcrumb with this navigation view.
     *
     * @param breadcrumb The breadcrumb view fragment
     */
    public void setBreadcrumb(Breadcrumb breadcrumb) {
        this.mBreadcrumb = breadcrumb;
        this.mBreadcrumb.addBreadcrumbListener(this);
    }

    /**
     * Method that sets the listener for communicate history changes.
     *
     * @param onHistoryListener The listener for communicate history changes
     */
    public void setOnHistoryListener(OnHistoryListener onHistoryListener) {
        this.mOnHistoryListener = onHistoryListener;
    }

    /**
     * Method that sets the listener which communicates selection changes.
     *
     * @param onNavigationSelectionChangedListener The listener reference
     */
    public void setOnNavigationSelectionChangedListener(
            OnNavigationSelectionChangedListener onNavigationSelectionChangedListener) {
        this.mOnNavigationSelectionChangedListener = onNavigationSelectionChangedListener;
    }

    /**
     * @return the mOnFilePickedListener
     */
    public OnFilePickedListener getOnFilePickedListener() {
        return this.mOnFilePickedListener;
    }

    /**
     * Method that sets the listener for picked items
     *
     * @param onFilePickedListener The listener reference
     */
    public void setOnFilePickedListener(OnFilePickedListener onFilePickedListener) {
        this.mOnFilePickedListener = onFilePickedListener;
    }

    /**
     * Method that sets the listener for directory changes
     *
     * @param onDirectoryChangedListener The listener reference
     */
    public void setOnDirectoryChangedListener(
            OnDirectoryChangedListener onDirectoryChangedListener) {
        this.mOnDirectoryChangedListener = onDirectoryChangedListener;
    }

    /**
     * Method that sets the listener for back requests
     *
     * @param onBackRequestListener The listener reference
     */
    public void setOnBackRequestListener(
            OnBackRequestListener onBackRequestListener) {
        this.mOnBackRequestListener = onBackRequestListener;
    }


    /**
     * Method that sets if the view should use flinger gesture detection.
     *
     * @param useFlinger If the view should use flinger gesture detection
     */
    public void setUseFlinger(boolean useFlinger) {
        // TODO: Re-enable when icons layout implementation is finished
        /*if (this.mCurrentMode.compareTo(NavigationLayoutMode.ICONS) == 0) {
            // Not supported
            return;
        }*/
        // Set the flinger listener (only when navigate)
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
            if (this.mAdapterView instanceof FlingerListView) {
                if (useFlinger) {
                    ((FlingerListView)this.mAdapterView).
                    setOnItemFlingerListener(this.mOnItemFlingerListener);
                } else {
                    ((FlingerListView)this.mAdapterView).setOnItemFlingerListener(null);
                }
            }
        }
    }

    /**
     * Method that forces the view to scroll to the file system object passed.
     *
     * @param fso The file system object
     */
    public void scrollTo(final FileSystemObject fso) {

        this.mAdapterView.post(new Runnable() {

            @Override
            public void run() {
                if (fso != null) {
                    try {
                        int position = mAdapter.getPosition(fso);
                        mAdapterView.setSelection(position);

                        // Make the scrollbar appear
                        if (position > 0) {
                            mAdapterView.scrollBy(0, 1);
                            mAdapterView.scrollBy(0, -1);
                        }

                    } catch (Exception e) {
                        mAdapterView.setSelection(0);
                    }
                } else {
                    mAdapterView.setSelection(0);
                }
            }
        });

    }

    /**
     * Method that refresh the view data.
     */
    public void refresh() {
        refresh(false);
    }

    /**
     * Method that refresh the view data.
     *
     * @param restore Restore previous position
     */
    public void refresh(boolean restore) {
        FileSystemObject fso = null;
        // Try to restore the previous scroll position
        if (restore) {
            try {
                if (this.mAdapterView != null && this.mAdapter != null) {
                    int position = this.mAdapterView.getFirstVisiblePosition();
                    fso = this.mAdapter.getItem(position);
                }
            } catch (Throwable _throw) {/**NON BLOCK**/}
        }
        refresh(fso);
    }

    /**
     * Method that refresh the view data.
     *
     * @param scrollTo Scroll to object
     */
    public void refresh(FileSystemObject scrollTo) {
        //Check that current directory was set
        if (this.mCurrentDir == null || this.mFiles == null) {
            return;
        }

        //Reload data
        changeCurrentDir(this.mCurrentDir, false, true, false, null, scrollTo);
    }

    /**
     * Method that recycles this object
     */
    public void recycle() {
        if (this.mAdapter != null) {
            this.mAdapter.dispose();
        }
    }

    /**
     * Method that change the view mode.
     *
     * @param newMode The new mode
     */
    @SuppressWarnings("unchecked")
    public void changeViewMode(final NavigationLayoutMode newMode) {
        //Check that it is really necessary change the mode
        if (this.mCurrentMode != null && this.mCurrentMode.compareTo(newMode) == 0) {
            return;
        }

        // If we should set the listview to response to flinger gesture detection
        boolean useFlinger =
                Preferences.getSharedPreferences().getBoolean(
                        FileManagerSettings.SETTINGS_USE_FLINGER.getId(),
                        ((Boolean)FileManagerSettings.
                                SETTINGS_USE_FLINGER.
                                getDefaultValue()).booleanValue());

        //Creates the new layout
        AdapterView<ListAdapter> newView = null;
        int itemResourceId = -1;
        // TODO: Re-enable when icons layout implementation is finished
        /*if (newMode.compareTo(NavigationLayoutMode.ICONS) == 0) {
            newView = (AdapterView<ListAdapter>)inflate(
                    getContext(), RESOURCE_MODE_ICONS_LAYOUT, null);
            itemResourceId = RESOURCE_MODE_ICONS_ITEM;

        } else */if (newMode.compareTo(NavigationLayoutMode.SIMPLE) == 0) {
            newView = (AdapterView<ListAdapter>)LayoutInflater.from(getContext()).inflate(
                    RESOURCE_MODE_SIMPLE_LAYOUT, this, false);
            itemResourceId = RESOURCE_MODE_SIMPLE_ITEM;

        } else if (newMode.compareTo(NavigationLayoutMode.DETAILS) == 0) {
            newView = (AdapterView<ListAdapter>)LayoutInflater.from(getContext()).inflate(
                    RESOURCE_MODE_DETAILS_LAYOUT, this, false);
            itemResourceId = RESOURCE_MODE_DETAILS_ITEM;
        }

        // Set the flinger listener (only when navigate)
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
            if (useFlinger && newView instanceof FlingerListView) {
                ((FlingerListView)newView).
                        setOnItemFlingerListener(this.mOnItemFlingerListener);
            }
        }

        //Get the current adapter and its adapter list
        List<FileSystemObject> files = new ArrayList<FileSystemObject>(this.mFiles);
        final AdapterView<ListAdapter> current =
                (AdapterView<ListAdapter>)findViewById(RESOURCE_CURRENT_LAYOUT);
        FileSystemObjectAdapter adapter =
                new FileSystemObjectAdapter(
                        getContext(),
                        new ArrayList<FileSystemObject>(),
                        itemResourceId,
                        this.mNavigationMode.compareTo(NAVIGATION_MODE.PICKABLE) == 0);
        adapter.setOnSelectionChangedListener(this);

        //Remove current layout
        if (current != null) {
            if (current.getAdapter() != null) {
                //Save selected items before dispose adapter
                FileSystemObjectAdapter currentAdapter =
                        ((FileSystemObjectAdapter)current.getAdapter());
                adapter.setSelectedItems(currentAdapter.getSelectedItems());
                currentAdapter.dispose();
            }
            removeView(current);
        }
        this.mFiles = files;
        adapter.addAll(files);

        //Set the adapter
        this.mAdapter = adapter;
        newView.setAdapter(this.mAdapter);
        newView.setOnItemClickListener(NavigationView.this);

        //Add the new layout
        this.mAdapterView = newView;
        addView(newView, 0);
        this.mCurrentMode = newMode;

        // Pick mode doesn't implements the onlongclick
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
            this.mAdapterView.setOnItemLongClickListener(this);
        } else {
            this.mAdapterView.setOnItemLongClickListener(null);
        }

        //Save the preference (only in navigation browse mode)
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
            try {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_LAYOUT_MODE, newMode, true);
            } catch (Exception ex) {
                Log.e(TAG, "Save of view mode preference fails", ex); //$NON-NLS-1$
            }
        }
    }

    /**
     * Method that removes a {@link FileSystemObject} from the view
     *
     * @param fso The file system object
     */
    public void removeItem(FileSystemObject fso) {
        // Delete also from internal list
        if (fso != null) {
            int cc = this.mFiles.size()-1;
            for (int i = cc; i >= 0; i--) {
                FileSystemObject f = this.mFiles.get(i);
                if (f != null && f.compareTo(fso) == 0) {
                    this.mFiles.remove(i);
                    break;
                }
            }
        }
        this.mAdapter.remove(fso);
    }

    /**
     * Method that removes a file system object from his path from the view
     *
     * @param path The file system object path
     */
    public void removeItem(String path) {
        FileSystemObject fso = this.mAdapter.getItem(path);
        if (fso != null) {
            this.mAdapter.remove(fso);
        }
    }

    /**
     * Method that returns the current directory.
     *
     * @return String The current directory
     */
    public String getCurrentDir() {
        return this.mCurrentDir;
    }

    /**
     * Method that returns the current directory's {@link FileSystemObject}
     *
     * @return String The current directory
     */
    public FileSystemObject getCurrentFso() {
        return this.mCurrentFileSystemObject;
    }

    /**
     * Method that changes the current directory of the view.
     *
     * @param newDir The new directory location
     */
    public void changeCurrentDir(final String newDir) {
        changeCurrentDir(newDir, true, false, false, null, null);
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

    /**
     * Method that changes the current directory of the view.
     *
     * @param newDir The new directory location
     * @param searchInfo The search information (if calling activity is {@link "SearchActivity"})
     */
    public void changeCurrentDir(final String newDir, SearchInfoParcelable searchInfo) {
        changeCurrentDir(newDir, true, false, false, searchInfo, null);
    }

    /**
     * Method that changes the current directory of the view.
     *
     * @param newDir The new directory location
     * @param addToHistory Add the directory to history
     * @param reload Force the reload of the data
     * @param useCurrent If this method must use the actual data (for back actions)
     * @param searchInfo The search information (if calling activity is {@link "SearchActivity"})
     * @param scrollTo If not null, then listview must scroll to this item
     */
    private void changeCurrentDir(
            final String newDir, final boolean addToHistory,
            final boolean reload, final boolean useCurrent,
            final SearchInfoParcelable searchInfo, final FileSystemObject scrollTo) {
        NavigationTask task = new NavigationTask(useCurrent, addToHistory, reload,
                searchInfo, scrollTo);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newDir);
    }

    /**
     * Remove all unmounted files in the current selection
     */
    public void removeUnmountedSelection() {
        List<FileSystemObject> selection = mAdapter.getSelectedItems();
        int cc = selection.size() - 1;
        for (int i = cc; i >= 0; i--) {
            FileSystemObject item = selection.get(i);
            VirtualMountPointConsole vc =
                    VirtualMountPointConsole.getVirtualConsoleForPath(item.getFullPath());
            if (vc != null && !vc.isMounted()) {
                selection.remove(i);
            }
        }
        mAdapter.setSelectedItems(selection);
        mAdapter.notifyDataSetChanged();

        // Do not call the selection listener. This method is supposed to be called by the
        // listener itself
    }

    /**
     * Method that loads the files in the adapter.
     *
     * @param files The files to load in the adapter
     * @hide
     */
    @SuppressWarnings("unchecked")
    private void loadData(final List<FileSystemObject> files) {
        //Notify data to adapter view
        final AdapterView<ListAdapter> view =
                (AdapterView<ListAdapter>)findViewById(RESOURCE_CURRENT_LAYOUT);
        FileSystemObjectAdapter adapter = (FileSystemObjectAdapter)view.getAdapter();
        adapter.setNotifyOnChange(false);
        adapter.clear();
        adapter.addAll(files);
        adapter.notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // Different actions depending on user preference

        // Get the adapter and the fso
        FileSystemObjectAdapter adapter = ((FileSystemObjectAdapter)parent.getAdapter());
        if (adapter == null || position < 0 || (position >= adapter.getCount())) {
            return false;
        }
        FileSystemObject fso = adapter.getItem(position);

        // Parent directory hasn't actions
        if (fso instanceof ParentDirectory) {
            return false;
        }

        // Pick mode doesn't implements the onlongclick
        if (this.mNavigationMode.compareTo(NAVIGATION_MODE.PICKABLE) == 0) {
            return false;
        }

        if (this.mAdapter != null) {
            View v = view.findViewById(R.id.navigation_view_item_icon);
            this.mAdapter.toggleSelection(v, fso);
        }
        return true; //Always consume the event
    }

    /**
     * Method that opens or navigates to the {@link FileSystemObject}
     *
     * @param fso The file system object
     */
    public void open(FileSystemObject fso) {
        open(fso, null);
    }

    /**
     * Method that opens or navigates to the {@link FileSystemObject}
     *
     * @param fso The file system object
     * @param searchInfo The search info
     */
    public void open(FileSystemObject fso, SearchInfoParcelable searchInfo) {
        // If is a folder, then navigate to
        if (FileHelper.isDirectory(fso)) {
            changeCurrentDir(fso.getFullPath(), searchInfo);
        } else {
            // Open the file with the preferred registered app
            IntentsActionPolicy.openFileSystemObject(getContext(), this, fso, false, null, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileSystemObject fso = ((FileSystemObjectAdapter) parent.getAdapter()).getItem(position);
        if (!((FileSystemObjectAdapter) parent.getAdapter()).isSelected(position)) {
            try {
                if (fso instanceof ParentDirectory) {
                    changeCurrentDir(fso.getParent(), true, false, false, null, null);
                    return;
                } else if (fso instanceof Directory) {
                    changeCurrentDir(fso.getFullPath(), true, false, false, null, null);
                    return;
                } else if (fso instanceof Symlink) {
                    Symlink symlink = (Symlink) fso;
                    if (symlink.getLinkRef() != null && symlink.getLinkRef() instanceof Directory) {
                        changeCurrentDir(
                                symlink.getLinkRef().getFullPath(), true, false, false, null, null);
                        return;
                    }

                    // Open the link ref
                    fso = symlink.getLinkRef();
                } else if (fso instanceof RootDirectory) {
                    RootDirectory rootDirectory = (RootDirectory) fso;
                    changeCurrentDir(rootDirectory.getRootPath(), true);
                    return;
                }

                // Open the file (edit or pick)
                if (this.mNavigationMode.compareTo(NAVIGATION_MODE.BROWSABLE) == 0) {
                    // Open the file with the preferred registered app
                    IntentsActionPolicy.openFileSystemObject(getContext(),
                            NavigationView.this, fso, false, null, null);
                } else {
                    // Request a file pick selection
                    if (this.mOnFilePickedListener != null) {
                        this.mOnFilePickedListener.onFilePicked(fso);
                    }
                }
            } catch (Throwable ex) {
                ExceptionUtil.translateException(getContext(), ex);
            }
        } else {
            onToggleSelection(fso);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            refresh((FileSystemObject) o);
        } else if (o == null) {
            refresh();
        }
        if (clearSelection) {
            onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {
        if (o != null && o instanceof FileSystemObject) {
            removeItem((FileSystemObject) o);
        } else {
            onRequestRefresh(null, clearSelection);
        }
        if (clearSelection) {
            onDeselectAll();
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
    public void onCancel() {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeCurrentDir(item.getItemPath(), true, true, false, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectionChanged(final List<FileSystemObject> selectedItems) {
        if (this.mOnNavigationSelectionChangedListener != null) {
            this.mOnNavigationSelectionChangedListener.onSelectionChanged(this, selectedItems);
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onToggleSelection(FileSystemObject fso) {
        if (this.mAdapter != null) {
            this.mAdapter.toggleSelection(fso);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeselectAll() {
        if (this.mAdapter != null) {
            this.mAdapter.deselectedAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectAllVisibleItems() {
        if (this.mAdapter != null) {
            this.mAdapter.selectedAllVisibleItems();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeselectAllVisibleItems() {
        if (this.mAdapter != null) {
            this.mAdapter.deselectedAllVisibleItems();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> onRequestSelectedFiles() {
        return this.getSelectedFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> onRequestCurrentItems() {
        return this.getFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String onRequestCurrentDir() {
        return this.mCurrentDir;
    }

    /**
     * Method that sets the primary color for the current volume
     *
     * @param color hex color of to be used as primary color for the current volume
     */
    public void setPrimaryColor(int color) {
        if (this.mAdapter != null) {
            this.mAdapter.setPrimaryColor(color);
        }
    }

    /**
     * Method that creates a ChRooted environment, protecting the user to break anything
     * in the device
     * @hide
     */
    public void createChRooted() {
        // If we are in a ChRooted environment, then do nothing
        if (this.mChRooted) return;
        this.mChRooted = true;

        //Change to first storage volume
        StorageVolume[] volumes =
                StorageHelper.getStorageVolumes(getContext(), false);
        if (volumes != null && volumes.length > 0) {
            changeCurrentDir(volumes[0].getPath(), false, true, false, null, null);
        }
    }

    /**
     * Method that exits from a ChRooted environment
     * @hide
     */
    public void exitChRooted() {
        // If we aren't in a ChRooted environment, then do nothing
        if (!this.mChRooted) return;
        this.mChRooted = false;

        // Refresh
        refresh();
    }
}