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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.CheckableListAdapter;
import com.cyanogenmod.filemanager.adapters.CheckableListAdapter.CheckableItem;
import com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter;
import com.cyanogenmod.filemanager.adapters.PickerAdapter;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.preferences.DisplayRestrictions;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnDirectoryChangedListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnFilePickedListener;
import com.cyanogenmod.filemanager.ui.widgets.PickerHeaderView;
import com.cyanogenmod.filemanager.ui.widgets.ToggleSwipeViewPager;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyanogenmod.filemanager.adapters.PickerAdapter.ListType;

/**
 * The activity for allow to use a {@link NavigationView} like, to pick a file from other
 * application.
 */
public class PickerActivity extends Activity implements OnCancelListener, OnDismissListener,
        OnFilePickedListener, OnDirectoryChangedListener, OnRequestRefreshListener {

    private static final String TAG = "PickerActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    // The result code
    private static final int RESULT_CROP_IMAGE = 1;

    // The component that holds the crop operation. We use Gallery3d because we are confidence
    // of his input parameters
    private static final ComponentName CROP_COMPONENT =
                                    new ComponentName(
                                            "com.android.gallery3d", //$NON-NLS-1$
                                            "com.android.gallery3d.filtershow.crop.CropActivity"); //$NON-NLS-1$

    // Gallery crop editor action
    private static final String ACTION_CROP = "com.android.camera.action.CROP"; //$NON-NLS-1$

    // Extra data for Gallery CROP action
    private static final String EXTRA_CROP = "crop"; //$NON-NLS-1$

    // Intent for folder picker
    public static final String INTENT_FOLDER_SELECT = "com.android.fileexplorer.action.DIR_SEL";
    // String extra for folder selection
    public static final String EXTRA_FOLDER_PATH = "def_file_manager_result_dir";

    /**
     * Constant for extra information for picker activity mode
     */
    public static final String EXTRA_ACTION = "extra_picker_activity_mode";

    /**
     * The Picker action mode
     * @hide
     */
    public enum ACTION_MODE {
        /**
         * The picker activity is configured for select action.
         * This is default behavior if not specified.
         */
        SELECT,
        /**
         * The picker activity is configured for copy action.
         */
        COPY,
        /**
         * The picker activity is configured for move action.
         */
        MOVE,
    }

    FileSystemObject mFso;  // The picked item
    FileSystemObject mCurrentDirectory;
    private AlertDialog mDialog;
    /**
     * @hide
     */
    NavigationView mNavigationView;
    /**
     * @hide
     */
    ListView mRootListView;
    private View mRootView;
    private PickerHeaderView mHeaderView;
    private ToggleSwipeViewPager mViewPager;

    private int mPrimaryColor;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        if (DEBUG) {
            Log.d(TAG, "PickerActivity.onCreate"); //$NON-NLS-1$
        }

        // Set the theme before setContentView
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, true);

        // Initialize the activity
        init();

        //Save state
        super.onCreate(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "PickerActivity.onDestroy"); //$NON-NLS-1$
        }

        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        measureHeight();
    }

    /**
     * Method that displays a dialog with a {@link NavigationView} to select the
     * proposed file
     */
    private void init() {
        final boolean pickingDirectory;
        final Intent intent = getIntent();

        if (isFilePickIntent(intent)) {
            // ok
            Log.d(TAG, "PickerActivity: got file pick intent: " + String.valueOf(intent)); //$NON-NLS-1$
            pickingDirectory = false;
        } else if (isDirectoryPickIntent(getIntent())) {
            // ok
            Log.d(TAG, "PickerActivity: got folder pick intent: " + String.valueOf(intent)); //$NON-NLS-1$
            pickingDirectory = true;
        } else {
            Log.d(TAG, "PickerActivity got unrecognized intent: " + String.valueOf(intent)); //$NON-NLS-1$
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        // Display restrictions
        Bundle extras = getIntent().getExtras();
        Map<DisplayRestrictions, Object> restrictions = new HashMap<DisplayRestrictions, Object>();
        //- Mime/Type restriction
        String mimeType = getIntent().getType();
        if (mimeType != null) {
            if (!MimeTypeHelper.isMimeTypeKnown(this, mimeType)) {
                Log.i(TAG,
                        String.format(
                                "Mime type %s unknown, falling back to wildcard.", //$NON-NLS-1$
                                mimeType));
                mimeType = MimeTypeHelper.ALL_MIME_TYPES;
            }
            restrictions.put(DisplayRestrictions.MIME_TYPE_RESTRICTION, mimeType);
        } else {
            String[] mimeTypes = getIntent().getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
            if (mimeTypes != null && mimeTypes.length > 0) {
                restrictions.put(DisplayRestrictions.MIME_TYPE_RESTRICTION, mimeTypes);
            }
        }
        // Other restrictions
        Log.d(TAG, "PickerActivity. extras: " + String.valueOf(extras)); //$NON-NLS-1$
        if (extras != null) {
            //-- File size
            if (extras.containsKey(android.provider.MediaStore.Audio.Media.EXTRA_MAX_BYTES)) {
                long size =
                        extras.getLong(android.provider.MediaStore.Audio.Media.EXTRA_MAX_BYTES);
                restrictions.put(DisplayRestrictions.SIZE_RESTRICTION, Long.valueOf(size));
            }
            //-- Local filesystems only
            if (extras.containsKey(Intent.EXTRA_LOCAL_ONLY)) {
                boolean localOnly = extras.getBoolean(Intent.EXTRA_LOCAL_ONLY);
                restrictions.put(
                        DisplayRestrictions.LOCAL_FILESYSTEM_ONLY_RESTRICTION,
                        Boolean.valueOf(localOnly));
            }
        }
        if (pickingDirectory) {
            restrictions.put(DisplayRestrictions.DIRECTORY_ONLY_RESTRICTION, Boolean.TRUE);
        }

        // Create or use the console
        if (!initializeConsole()) {
            // Something when wrong. Display a message and exit
            DialogHelper.showToast(this, R.string.msgs_cant_create_console, Toast.LENGTH_SHORT);
            cancel();
            return;
        }

        this.mPrimaryColor = getResources().getColor(R.color.picker_header_color);

        // Create the root file
        this.mRootView = getLayoutInflater().inflate(R.layout.picker, null, false);
        this.mRootView.post(new Runnable() {
            @Override
            public void run() {
                measureHeight();
            }
        });

        // Get the viewPager
        this.mViewPager = (ToggleSwipeViewPager)this.mRootView.findViewById(R.id.picker_viewpager);
        this.mViewPager.setAdapter(new PickerAdapter());
        this.mViewPager.setSwipeEnabled(false);

        // Roots listview
        mRootListView = (ListView)this.mRootView.findViewById(R.id.roots_listview);
        mRootListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position,
                    long id) {
                // Navigate to new directory
                final FileSystemObject fso =
                        ((FileSystemObjectAdapter)parent.getAdapter()).getItem(position);
                final RootDirectory rootDir = (RootDirectory)fso;
                PickerActivity.this.mNavigationView.setPrimaryColor(rootDir.getPrimaryColor());
                PickerActivity.this.mNavigationView.changeCurrentDir(rootDir.getRootPath());
                PickerActivity.this.mPrimaryColor = rootDir.getPrimaryColor();
            }
        });
        GetStorageVolumesTask task =
                new GetStorageVolumesTask(this.getApplicationContext(), mRootListView);
        task.execute();

        // Navigation view
        this.mNavigationView = (NavigationView)this.mRootView.findViewById(R.id.navigation_view);
        this.mNavigationView.setRestrictions(restrictions);
        this.mNavigationView.setOnFilePickedListener(this);
        this.mNavigationView.setOnDirectoryChangedListener(this);

        // Get dialog title and positive button, default to picker_title and select respectively
        ACTION_MODE pickerMode = ACTION_MODE.SELECT;
        if (extras != null) {
            if (extras.containsKey(EXTRA_ACTION)) {
                int mode = extras.getInt(EXTRA_ACTION);
                pickerMode = ACTION_MODE.values()[mode];
            }
        }
        int titleId = (pickingDirectory) ? R.string.directory_picker_title : R.string.picker_title;
        int buttonId = R.string.select;
        switch (pickerMode) {
            case COPY:
                titleId = R.string.picker_copy_title;
                buttonId = R.string.copy;
                break;
            case MOVE:
                titleId = R.string.picker_move_title;
                buttonId = R.string.move;
                break;
            default:
                break;
        }

        mHeaderView =
                (PickerHeaderView) this.mRootView.findViewById(R.id.picker_header);
        mHeaderView.setActionText(titleId);
        mHeaderView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create new directory
                ActionsDialog.showInputNameDialog(PickerActivity.this,
                        getString(R.string.actions_menu_new_directory),
                        R.id.mnu_actions_new_directory, mNavigationView.getFiles(),
                        mNavigationView, PickerActivity.this);
            }
        });

        // Create the dialog
        this.mDialog = DialogHelper.createDialog(this, 0, null, this.mRootView);
        this.mDialog.setCustomTitle(null);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                dlg.cancel();
            }
        });
        if (pickingDirectory) {
            this.mDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    getString(buttonId),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dlg, int which) {
                    PickerActivity.this.mFso = PickerActivity.this.mCurrentDirectory;
                    dlg.dismiss();
                }
            });
        }
        this.mDialog.setCancelable(true);
        this.mDialog.setOnCancelListener(this);
        this.mDialog.setOnDismissListener(this);
        DialogHelper.delegateDialogShow(this, this.mDialog);

        final File initialDir = getInitialDirectoryFromIntent(getIntent());
        final String rootDirectory;

        if (initialDir == null) {
            mViewPager.setCurrentItem(ListType.ROOTS_LISTVIEW.ordinal(), false);
        } else {
            rootDirectory = initialDir.getAbsolutePath();

            // Navigate to. The navigation view will redirect to the appropriate directory
            PickerActivity.this.mNavigationView.changeCurrentDir(rootDirectory);
        }

    }

    /**
     * Method that measure the height needed to avoid resizing when
     * change to a new directory. This method fixed the height of the window
     * @hide
     */
    void measureHeight() {
        // Calculate the dialog size based on the window height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        final int height = displaymetrics.heightPixels;

        Configuration config = getResources().getConfiguration();
        int percent = config.orientation == Configuration.ORIENTATION_LANDSCAPE ? 55 : 70;

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, (height * percent) / 100);
        this.mRootView.setLayoutParams(params);
    }

    /**
     * Method that initializes a console
     */
    private boolean initializeConsole() {
        try {
            // Create a ChRooted console
            ConsoleBuilder.createDefaultConsole(this, false, false);
            // There is a console allocated. Use it.
            return true;
        } catch (Throwable _throw) {
            // Capture the exception
            ExceptionUtil.translateException(this, _throw, true, false);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_CROP_IMAGE:
                // Return what the callee activity returns
                setResult(resultCode, data);
                finish();
                return;

            default:
                break;
        }

        // The response is not understood
        Log.w(TAG,
                String.format(
                        "Ignore response. requestCode: %s, resultCode: %s, data: %s", //$NON-NLS-1$
                        Integer.valueOf(requestCode),
                        Integer.valueOf(resultCode),
                        data));
        DialogHelper.showToast(this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.mFso != null) {
            File src = new File(this.mFso.getFullPath());
            if (getIntent().getExtras() != null) {
                // Some AOSP applications use the gallery to edit and crop the selected image
                // with the Gallery crop editor. In this case pass the picked file to the
                // CropActivity with the requested parameters
                // Expected result is on onActivityResult
                Bundle extras = getIntent().getExtras();
                String crop = extras.getString(EXTRA_CROP);
                if (Boolean.parseBoolean(crop)) {
                    // We want to use the Gallery3d activity because we know about it, and his
                    // parameters. At least we have a compatible one.
                    Intent intent = new Intent(ACTION_CROP);
                    if (getIntent().getType() != null) {
                        intent.setType(getIntent().getType());
                    }
                    intent.setData(Uri.fromFile(src));
                    intent.putExtras(extras);
                    intent.setComponent(CROP_COMPONENT);
                    try {
                        startActivityForResult(intent, RESULT_CROP_IMAGE);
                        return;
                    } catch (ActivityNotFoundException e) {
                        Log.w(TAG, "Failed to find crop activity!");
                    }
                    intent.setComponent(null);
                    try {
                        startActivityForResult(intent, RESULT_CROP_IMAGE);
                        return;
                    } catch (ActivityNotFoundException e) {
                        Log.w(TAG, "Failed to find any crop activity!");
                    }
                }
            }

            if (INTENT_FOLDER_SELECT.equals(getIntent().getAction())) {
                Intent result = new Intent();
                result.putExtra(EXTRA_FOLDER_PATH, mFso.getFullPath());
                setResult(Activity.RESULT_OK, result);
                finish();
                return;
            }

            // Return the picked file, as expected (this activity should fill the intent data
            // and return RESULT_OK result)
            Intent result = new Intent();
            result.setData(getResultUriForFileFromIntent(this, src, getIntent()));
            setResult(Activity.RESULT_OK, result);
            finish();

        } else {
            cancel();
        }
    }

    private static boolean isFilePickIntent(Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_GET_CONTENT.equals(action)) {
            return true;
        }
        if (Intent.ACTION_PICK.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && FileHelper.FILE_URI_SCHEME.equals(data.getScheme())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isDirectoryPickIntent(Intent intent) {
        if (INTENT_FOLDER_SELECT.equals(intent.getAction())) {
            return true;
        }

        if (Intent.ACTION_PICK.equals(intent.getAction()) && intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            if (FileHelper.FOLDER_URI_SCHEME.equals(scheme)
                    || FileHelper.DIRECTORY_URI_SCHEME.equals(scheme)) {
                return true;
            }
        }

        return false;
    }

    private static File getInitialDirectoryFromIntent(Intent intent) {
        if (!Intent.ACTION_PICK.equals(intent.getAction())) {
            return null;
        }

        if (INTENT_FOLDER_SELECT.equals(intent.getAction())) {
            return Environment.getExternalStorageDirectory();
        }

        final Uri data = intent.getData();
        if (data == null) {
            return null;
        }

        final String path = data.getPath();
        if (path == null) {
            return null;
        }

        final File file = new File(path);
        if (!file.exists() || !file.isAbsolute()) {
            return null;
        }

        if (file.isDirectory()) {
            return file;
        }
        return file.getParentFile();
    }

    private static Uri getResultUriForFileFromIntent(Context context, File src, Intent intent) {
        // Try to find the preferred uri scheme
        Uri result = MediaHelper.fileToContentUri(context, src);
        if (result == null) {
            result = Uri.fromFile(src);
        }

        if (Intent.ACTION_PICK.equals(intent.getAction()) && intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            if (scheme != null) {
                result = result.buildUpon().scheme(scheme).build();
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        cancel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFilePicked(FileSystemObject item) {
        this.mFso = item;
        this.mDialog.dismiss();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDirectoryChanged(FileSystemObject item) {
        this.mCurrentDirectory = item;
        if (TextUtils.equals(mCurrentDirectory.getName(), FileHelper.ROOTS_LIST)
                && mCurrentDirectory.getParent() == null) {
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            // show roots list
            changeListView(ListType.ROOTS_LISTVIEW);
            mPrimaryColor = getResources().getColor(R.color.picker_header_color);
            //mHeaderView.showMenu(false);
        } else {
            changeListView(ListType.NAVIGATION_VIEW);
            //mHeaderView.showMenu(true);
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        }
        mHeaderView.setDirectory(mCurrentDirectory.getName());
        mHeaderView.setPrimaryColor(mPrimaryColor);
    }

    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(View view) {
        switch (view.getId()) {
            //######################
            //Breadcrumb Actions
            //######################
            case R.id.ab_filesystem_info:
                //Show a popup with the storage volumes to select
                showStorageVolumesPopUp(view);
                break;

            default:
                break;
        }
    }

    /**
     * Method that cancels the activity
     */
    private void cancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void changeListView(ListType type) {
        if (mViewPager.getCurrentItem() != type.ordinal()) {
            mViewPager.setCurrentItem(type.ordinal(), false);
        }
    }

    /**
     * Method that shows a popup with the storage volumes
     *
     * @param anchor The view on which anchor the popup
     */
    private void showStorageVolumesPopUp(View anchor) {
        // Create a list (but not checkable)
        final StorageVolume[] volumes = StorageHelper.getStorageVolumes(PickerActivity.this, false);
        List<CheckableItem> descriptions = new ArrayList<CheckableItem>();
        if (volumes != null) {
            int cc = volumes.length;
            for (int i = 0; i < cc; i++) {
                StorageVolume volume = volumes[i];
                if (volumes[i] != null) {
                    String mountedState = volumes[i].getState();
                    String path = volumes[i].getPath();
                    if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(mountedState) &&
                            !Environment.MEDIA_MOUNTED_READ_ONLY.equalsIgnoreCase(mountedState)) {
                        Log.w(TAG, "Ignoring '" + path + "' with state of '"+ mountedState + "'");
                        continue;
                    }
                    if (!TextUtils.isEmpty(path)) {
                        String desc = StorageHelper.getStorageVolumeDescription(this, volumes[i]);
                        CheckableItem item = new CheckableItem(desc, false, false);
                        descriptions.add(item);
                    }
                }
            }

        }
        CheckableListAdapter adapter =
                new CheckableListAdapter(getApplicationContext(), descriptions);

        //Create a show the popup menu
        final ListPopupWindow popup = DialogHelper.createListPopupWindow(this, adapter, anchor);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                popup.dismiss();
                if (volumes != null) {
                    PickerActivity.this.
                            mNavigationView.changeCurrentDir(volumes[position].getPath());
                }
            }
        });
        popup.show();
    }

    private static class GetStorageVolumesTask
            extends AsyncTask<Void, String, List<FileSystemObject>> {
        private Context mContext;
        private WeakReference<ListView> mView;

        public GetStorageVolumesTask(final Context context, final ListView view) {
            mContext = context.getApplicationContext();
            mView = new WeakReference<ListView>(view);
        }

        @Override
        protected List<FileSystemObject> doInBackground(Void... params) {
            List<FileSystemObject> volumes =
                    StorageHelper.getStorageVolumesFileSystemObjectList(mContext);
            return volumes;
        }

        @Override
        protected void onPostExecute(List<FileSystemObject> volumes) {
            FileSystemObjectAdapter fsoAdapter = new FileSystemObjectAdapter(mContext, volumes,
                    R.layout.navigation_view_simple_item, true);
            if (mView != null && mView.get() != null) {
                mView.get().setAdapter(fsoAdapter);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Refresh only the item
            mNavigationView.refresh((FileSystemObject) o);
        } else if (o == null) {
            // Refresh all
            mNavigationView.refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {
        // Ignored
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
}
