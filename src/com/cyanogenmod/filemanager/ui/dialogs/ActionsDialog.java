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

package com.cyanogenmod.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment;
import com.cyanogenmod.filemanager.adapters.TwoColumnsMenuListAdapter;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.listeners.OnSelectionListener;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.model.SystemFile;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.policy.BookmarksActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.CompressActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.DeleteActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.ExecutionActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.InfoActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.NavigationActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.NewActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.PrintActionPolicy;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.SelectionHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.io.InvalidClassException;
import java.util.List;

import static com.cyanogenmod.filemanager.activities.PickerActivity.INTENT_FOLDER_SELECT;
import static com.cyanogenmod.filemanager.activities.PickerActivity.EXTRA_ACTION;
import static com.cyanogenmod.filemanager.activities.PickerActivity.ACTION_MODE.COPY;
import static com.cyanogenmod.filemanager.activities.PickerActivity.ACTION_MODE.MOVE;
import static com.cyanogenmod.filemanager.ui.fragments.NavigationFragment.INTENT_REQUEST_COPY;
import static com.cyanogenmod.filemanager.ui.fragments.NavigationFragment.INTENT_REQUEST_MOVE;

/**
 * A class that wraps a dialog for showing the list of actions that
 * the user can do.
 */
public class ActionsDialog implements OnItemClickListener, OnItemLongClickListener {
    /**
     * @hide
     */
    final Context mContext;
    final NavigationFragment mBackRef;
    private final boolean mGlobal;
    private final boolean mSearch;
    private final boolean mChRooted;

    /**
     * @hide
     */
    AlertDialog mDialog;
    private ListView mListView;
    /**
     * @hide
     */
    final FileSystemObject mFso;

    /**
     * @hide
     */
    OnRequestRefreshListener mOnRequestRefreshListener;
    /**
     * @hide
     */
    OnSelectionListener mOnSelectionListener;

    /**
     * Constructor of <code>ActionsDialog</code>.
     *
     * @param context The current context
     * @param fso The file system object associated
     * @param global If the menu to display will be the global one (Global actions)
     * @param search If the call is from search activity
     */
    public ActionsDialog(Context context, NavigationFragment backRef, FileSystemObject fso,
            boolean global, boolean search) {
        super();

        //Save the data
        this.mFso = fso;
        this.mContext = context;
        this.mBackRef = backRef;
        this.mGlobal = global;
        this.mSearch = search;
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;

        //Initialize dialog
        init(context, global ? R.id.mnu_actions_global : R.id.mnu_actions_fso);
    }

    /**
     * Method that initializes the dialog.
     *
     * @param context The current context
     * @param group The group of action menus to show
     */
    private void init(Context context, int group) {
        //Create the menu adapter
        TwoColumnsMenuListAdapter adapter =
                new TwoColumnsMenuListAdapter(context, R.menu.actions, group, false);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);

        //Create the list view
        this.mListView = new ListView(context);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.mListView.setLayoutParams(params);
        this.mListView.setAdapter(adapter);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, this.mListView, "background_drawable"); //$NON-NLS-1$
        this.mListView.setDivider(
                theme.getDrawable(context, "horizontal_divider_drawable")); //$NON-NLS-1$

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        R.string.actions_dialog_title,
                                        this.mListView);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                this.mContext.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener) null);
    }

    /**
     * Method that sets the listener for communicate a refresh request.
     *
     * @param onRequestRefreshListener The request refresh listener
     */
    public void setOnRequestRefreshListener(OnRequestRefreshListener onRequestRefreshListener) {
        this.mOnRequestRefreshListener = onRequestRefreshListener;
    }

    /**
     * Method that sets the listener for requesting selection data
     *
     * @param onSelectionListener The request selection data  listener
     */
    public void setOnSelectionListener(OnSelectionListener onSelectionListener) {
        this.mOnSelectionListener = onSelectionListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        TwoColumnsMenuListAdapter adapter =
                (TwoColumnsMenuListAdapter)this.mListView.getAdapter();
        configureMenu(adapter.getMenu());
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {

        //Retrieve the menu item
        MenuItem menuItem = ((TwoColumnsMenuListAdapter)parent.getAdapter()).getItemById((int)id);

        //What action was selected?
        switch ((int)id) {
            //- Rename
            case R.id.mnu_actions_rename:
            case R.id.mnu_actions_rename_selection:
                FileSystemObject fso = mFso;
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection = this.mOnSelectionListener
                            .onRequestSelectedFiles();
                    if (selection.size() == 1) {
                        fso = selection.get(0);
                    }
                }
                showFsoInputNameDialog(menuItem, fso, false);
                break;

            //- Create link
            case R.id.mnu_actions_create_link:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    showFsoInputNameDialog(menuItem, this.mFso, true);
                    return;
                }
                break;
            case R.id.mnu_actions_create_link_global:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    // The selection must be only 1 item
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    if (selection != null && selection.size() == 1) {
                        showFsoInputNameDialog(menuItem, selection.get(0), true);
                    }
                    return;
                }
                break;

            //- Delete
            case R.id.mnu_actions_delete:
                DeleteActionPolicy.removeFileSystemObjects(
                        this.mContext,
                        mListView,
                        this.mFso,
                        this.mOnSelectionListener,
                        this.mOnRequestRefreshListener,
                        null);
                break;

            //- Select/Deselect
            case R.id.mnu_actions_select:
            case R.id.mnu_actions_deselect:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onToggleSelection(this.mFso);
                }
                break;

            // Paste selection
            case R.id.mnu_actions_paste_selection:
                if (this.mOnSelectionListener != null) {
                    // Select destination
                    Intent intent = new Intent(INTENT_FOLDER_SELECT);
                    intent.putExtra(EXTRA_ACTION, COPY.ordinal());
                    mBackRef.startActivityForResult(intent, INTENT_REQUEST_COPY);
                }
                break;
            // Move selection
            case R.id.mnu_actions_move_selection:
                if (this.mOnSelectionListener != null) {
                    // Select destination
                    Intent intent = new Intent(INTENT_FOLDER_SELECT);
                    intent.putExtra(EXTRA_ACTION, MOVE.ordinal());
                    mBackRef.startActivityForResult(intent, INTENT_REQUEST_MOVE);
                }
                break;
            // Delete selection
            case R.id.mnu_actions_delete_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    DeleteActionPolicy.removeFileSystemObjects(
                            this.mContext,
                            mListView,
                            selection,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener,
                            null);
                }
                break;




            //- Properties
            case R.id.mnu_actions_properties:
            case R.id.mnu_actions_properties_current_folder:
                fso = this.mFso;
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection = this.mOnSelectionListener
                            .onRequestSelectedFiles();
                    if (selection.size() == 1) {
                        fso = selection.get(0);
                    }
                }
                InfoActionPolicy.showPropertiesDialog(
                        this.mContext, fso, this.mOnRequestRefreshListener);
                break;

            //- Navigate to parent
            case R.id.mnu_actions_open_parent_folder:
                NavigationActionPolicy.openParentFolder(
                        this.mContext, this.mFso, this.mOnRequestRefreshListener);
                break;

            default:
                break;
        }

        //Dismiss the dialog
        this.mDialog.dismiss();
    }

    /**
     * Method that show a new dialog for input a name.
     *
     * @param context Context for showing dialog and creating file system object
     * @param title The dialog title
     * @param id The menu id to determine if creating a directory or file
     * @param fileSystemObjects The list of file system objects in the current directory
     * @param onSelectionListener
     * @param onRequestRefreshListener
     */
    public static void showInputNameDialog(final Context context, String title, final int id,
            List <FileSystemObject> fileSystemObjects, final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {
        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        context,
                        fileSystemObjects,
                        title);
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                String name = inputNameDialog.getName();
                createNewFileSystemObject(context, id, name, onSelectionListener,
                        onRequestRefreshListener);
            }
        });
        inputNameDialog.show();
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
        //Hide the dialog
        this.mDialog.hide();

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this.mContext,
                        this.mOnSelectionListener.onRequestCurrentItems(),
                        fso,
                        allowFsoName,
                        menuItem.getTitle().toString());
        inputNameDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Show the menu again
                DialogHelper.delegateDialogShow(
                        ActionsDialog.this.mContext, ActionsDialog.this.mDialog);
            }
        });
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    switch (menuItem.getItemId()) {
                        case R.id.mnu_actions_rename:
                        case R.id.mnu_actions_rename_selection:
                            // Rename the fso
                            if (ActionsDialog.this.mOnSelectionListener != null) {
                                CopyMoveActionPolicy.renameFileSystemObject(
                                        ActionsDialog.this.mContext,
                                        mListView,
                                        inputNameDialog.mFso,
                                        name,
                                        ActionsDialog.this.mOnSelectionListener,
                                        ActionsDialog.this.mOnRequestRefreshListener);
                            }
                            break;

                        case R.id.mnu_actions_create_link:
                        case R.id.mnu_actions_create_link_global:
                            // Create a link to the fso
                            if (ActionsDialog.this.mOnSelectionListener != null) {
                                NewActionPolicy.createSymlink(
                                        ActionsDialog.this.mContext,
                                        inputNameDialog.mFso,
                                        name,
                                        ActionsDialog.this.mOnSelectionListener,
                                        ActionsDialog.this.mOnRequestRefreshListener);
                            }
                            break;

                        default:
                            break;
                    }

                } finally {
                    ActionsDialog.this.mDialog.dismiss();
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        DialogHelper.showToast(
                this.mContext,
                ((TextView)view).getText().toString(),
                Toast.LENGTH_SHORT);
        return true;
    }

    /**
     * Method that create the a new file system object.
     *
     * @param context Context to use for creating File System object
     * @param menuId The menu identifier (need to determine the fso type)
     * @param name The name of the file system object
     * @param onSelectionListener
     * @param onRequestRefreshListener
     * @hide
     */
    public static void createNewFileSystemObject(Context context, final int menuId,
            final String name, OnSelectionListener onSelectionListener,
            OnRequestRefreshListener onRequestRefreshListener) {
        switch (menuId) {
            case R.id.mnu_actions_new_directory:
                NewActionPolicy.createNewDirectory(
                        context, name, onSelectionListener, onRequestRefreshListener);
                break;
            case R.id.mnu_actions_new_file:
                NewActionPolicy.createNewFile(
                        context, name, onSelectionListener, onRequestRefreshListener);
                break;
            default:
                break;
        }
    }

    /**
     * Method that configure the menu to show according the actual information,
     * the kind of request, the file selection, the mount point, ...
     *
     * @param menu The menu to configure
     */
    private void configureMenu(Menu menu) {
        // Selection
        List<FileSystemObject> selection = null;
        if (this.mOnSelectionListener != null) {
            selection = this.mOnSelectionListener.onRequestSelectedFiles();
        }

        //- Check actions that needs a valid reference
        if (!this.mGlobal && this.mFso != null) {
            //- Select/Deselect -> Only one of them
            if (this.mOnSelectionListener != null) {
                boolean selected =
                        SelectionHelper.isFileSystemObjectSelected(
                                this.mOnSelectionListener.onRequestSelectedFiles(),
                                this.mFso);
                menu.removeItem(selected ? R.id.mnu_actions_select : R.id.mnu_actions_deselect);

            } else {
                // Remove both menus
                menu.removeItem(R.id.mnu_actions_select);
                menu.removeItem(R.id.mnu_actions_deselect);

                // Not allowed because we need a list of the current files (only from navigation
                // activity)
                menu.removeItem(R.id.mnu_actions_rename);
                menu.removeItem(R.id.mnu_actions_create_copy);
            }

            //- Open/Open with -> Only when the fso is not a folder and is not a system file
            if (FileHelper.isDirectory(this.mFso) || FileHelper.isSystemFile(this.mFso)) {
                menu.removeItem(R.id.mnu_actions_open);
                menu.removeItem(R.id.mnu_actions_open_with);
                menu.removeItem(R.id.mnu_actions_send);
            }

            // Create link (not allow in storage volume)
            if (StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }

            //Execute only if mime/type category is EXEC
            MimeTypeCategory category = MimeTypeHelper.getCategory(this.mContext, this.mFso);
            if (category.compareTo(MimeTypeCategory.EXEC) != 0) {
                menu.removeItem(R.id.mnu_actions_execute);
            }

            //- Checksum (only supported for files)
            if (FileHelper.isDirectory(this.mFso) || this.mFso instanceof Symlink) {
                menu.removeItem(R.id.mnu_actions_compute_checksum);
            }

            //- Print (only for text and image categories)
            if (!PrintActionPolicy.isPrintedAllowed(mContext, mFso)) {
                menu.removeItem(R.id.mnu_actions_print);
            }
        } else if (mGlobal && (selection != null && selection.size() != 1)) {
            menu.removeItem(R.id.mnu_actions_rename_selection);
            menu.removeItem(R.id.mnu_actions_create_copy_from_selection);
        }

        //- Remove properties option if multiple files selected
        if (selection != null && selection.size() > 1) {
            menu.removeItem(R.id.mnu_actions_properties);
            menu.removeItem(R.id.mnu_actions_properties_current_folder);
        }

        //- Paste/Move only when have a selection
        if (this.mGlobal) {
            if (selection == null || selection.size() == 0 ||
                    (this.mFso != null && !FileHelper.isDirectory(this.mFso))) {
                // Remove paste/move actions
                menu.removeItem(R.id.mnu_actions_paste_selection);
                menu.removeItem(R.id.mnu_actions_move_selection);
                menu.removeItem(R.id.mnu_actions_delete_selection);
            }
        }
        //- Create link
        if (this.mGlobal && (selection == null || selection.size() == 0
                || selection.size() > 1)) {
            // Only when one item is selected
            menu.removeItem(R.id.mnu_actions_create_link_global);
        } else if (this.mGlobal  && selection != null) {
            // Create link (not allow in sdcard, secure or remote storage volumes)
            FileSystemObject fso = selection.get(0);
            if (StorageHelper.isPathInStorageVolume(fso.getFullPath())
                    || fso.isSecure() || fso.isRemote()) {
                menu.removeItem(R.id.mnu_actions_create_link_global);
            }
        } else if (!this.mGlobal) {
            // Create link (not allow in sdcard, secure or remote storage volumes)
            if (StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())
                    || mFso.isSecure() || mFso.isRemote()) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }
        }

        //- Compress/Uncompress (only when selection is available)
        if (this.mOnSelectionListener != null) {
            //Compress
            if (this.mGlobal) {
                if (selection == null || selection.size() == 0) {
                    menu.removeItem(R.id.mnu_actions_compress_selection);
                } else {
                    for (FileSystemObject fso : selection) {
                        // Ignore for system, secure or remote files
                        if (fso instanceof SystemFile || fso.isSecure() || fso.isRemote()) {
                            menu.removeItem(R.id.mnu_actions_compress_selection);
                            break;
                        }
                    }
                }
            } else {
                // Ignore for system, secure or remote files
                if (this.mFso instanceof SystemFile || mFso.isSecure() || mFso.isRemote()) {
                    menu.removeItem(R.id.mnu_actions_compress);
                }
            }
            //Uncompress (Only supported files)
            if (!this.mGlobal && !FileHelper.isSupportedUncompressedFile(this.mFso)) {
                menu.removeItem(R.id.mnu_actions_extract);
            }

            // Send multiple (only regular files)
            if (this.mGlobal) {
                if (selection == null || selection.size() == 0) {
                    menu.removeItem(R.id.mnu_actions_send_selection);
                } else {
                    boolean areAllFiles = true;
                    int cc = selection.size();
                    for (int i = 0; i < cc; i++) {
                        FileSystemObject fso = selection.get(i);
                        if (FileHelper.isDirectory(fso)) {
                            areAllFiles = false;
                            break;
                        }
                    }
                    if (!areAllFiles) {
                        menu.removeItem(R.id.mnu_actions_send_selection);
                    }
                }
            }
        }

        // Shotcuts and Bookmarks (not available in virtual filesystems)
        if (!mGlobal && (mFso.isSecure() || mFso.isRemote())) {
            menu.removeItem(R.id.mnu_actions_add_shortcut);
        } else if (mGlobal) {
            if (selection != null && selection.size() > 0) {
                for (FileSystemObject fso : selection) {
                    if (fso.isSecure() || fso.isRemote()) {
                        menu.removeItem(R.id.mnu_actions_add_shortcut_current_folder);
                        break;
                    }
                }
            }
        }

        // Not allowed in search
        if (this.mSearch) {
            menu.removeItem(R.id.mnu_actions_extract);
            menu.removeItem(R.id.mnu_actions_compress);
            menu.removeItem(R.id.mnu_actions_create_link);
        } else {
            // Not allowed if not in search
            menu.removeItem(R.id.mnu_actions_open_parent_folder);
        }

        // Remove unsafe operations over virtual mountpoint directories
        List<Directory> virtualDirs = VirtualMountPointConsole.getVirtualMountableDirectories();
        if (!mGlobal && FileHelper.isDirectory(mFso) && virtualDirs.contains(mFso)) {
            menu.removeItem(R.id.mnu_actions_delete);
            menu.removeItem(R.id.mnu_actions_rename);
            menu.removeItem(R.id.mnu_actions_compress);
            menu.removeItem(R.id.mnu_actions_create_copy);
            menu.removeItem(R.id.mnu_actions_create_link);
            menu.removeItem(R.id.mnu_actions_add_shortcut);
        } else if (mGlobal) {
            if (selection != null && selection.size() > 0) {
                for (FileSystemObject fso : selection) {
                    if (FileHelper.isDirectory(fso) && virtualDirs.contains(fso)) {
                        menu.removeItem(R.id.mnu_actions_paste_selection);
                        menu.removeItem(R.id.mnu_actions_move_selection);
                        menu.removeItem(R.id.mnu_actions_delete_selection);
                        menu.removeItem(R.id.mnu_actions_rename);
                        menu.removeItem(R.id.mnu_actions_compress_selection);
                        menu.removeItem(R.id.mnu_actions_create_link_global);
                        menu.removeItem(R.id.mnu_actions_send_selection);
                        menu.removeItem(R.id.mnu_actions_create_link_global);
                        menu.removeItem(R.id.mnu_actions_create_link_global);
                        menu.removeItem(R.id.mnu_actions_create_link_global);
                        menu.removeItem(R.id.mnu_actions_add_shortcut_current_folder);
                        break;
                    }
                }
            }
        }

        // Remove not-ChRooted actions (actions that can't be present when running in
        // unprivileged mode)
        if (this.mChRooted) {
            menu.removeItem(R.id.mnu_actions_create_link);
            menu.removeItem(R.id.mnu_actions_create_link_global);
            menu.removeItem(R.id.mnu_actions_execute);

            // NOTE: This actions are not implemented in chrooted environments. The reason is
            // that the main target of this application is CyanogenMod (a rooted environment).
            // Adding this actions requires the use of commons-compress, an external Apache
            // library that will add more size to the ending apk.
            // For now, will maintain without implementation. Maybe, in the future.
            menu.removeItem(R.id.mnu_actions_compress);
            menu.removeItem(R.id.mnu_actions_compress_selection);
            menu.removeItem(R.id.mnu_actions_extract);
        }
    }
}
