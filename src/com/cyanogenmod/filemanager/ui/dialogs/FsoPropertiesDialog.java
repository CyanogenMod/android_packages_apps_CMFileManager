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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.model.AID;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.FolderUsage;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.GroupPermission;
import com.cyanogenmod.filemanager.model.OthersPermission;
import com.cyanogenmod.filemanager.model.Permission;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.model.UserPermission;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.AIDHelper;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.ResourcesHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.io.File;
import java.io.IOException;

/**
 * A class that wraps a dialog for showing information about a {@link FileSystemObject}
 */
public class FsoPropertiesDialog
    implements OnClickListener, OnCheckedChangeListener, OnItemSelectedListener,
    DialogInterface.OnCancelListener, DialogInterface.OnDismissListener, AsyncResultListener {

    private static final String TAG = "FsoPropertiesDialog"; //$NON-NLS-1$

    private static final String OWNER_TYPE = "owner"; //$NON-NLS-1$
    private static final String GROUP_TYPE = "group"; //$NON-NLS-1$
    private static final String OTHERS_TYPE = "others"; //$NON-NLS-1$

    private static final String AID_FORMAT = "%05d - %s"; //$NON-NLS-1$
    private static final String AID_SEPARATOR = " - "; //$NON-NLS-1$

    /**
     * @hide
     */
    final FileSystemObject mFso;
    /**
     * @hide
     */
    boolean mHasChanged;

    /**
     * @hide
     */
    final Context mContext;
    private final AlertDialog mDialog;
    private final View mContentView;
    private View mInfoViewTab;
    private View mPermissionsViewTab;
    private View mInfoView;
    private View mPermissionsView;
    /**
     * @hide
     */
    CheckBox mChkNoMedia;
    /**
     * @hide
     */
    Spinner mSpnOwner;
    /**
     * @hide
     */
    Spinner mSpnGroup;
    /**
     * @hide
     */
    private CheckBox[] mChkUserPermission;
    private CheckBox[] mChkGroupPermission;
    private CheckBox[] mChkOthersPermission;
    private TextView mInfoMsgView;
    /**
     * @hide
     */
    TextView mTvSize;
    /**
     * @hide
     */
    TextView mTvContains;

    /**
     * @hide
     */
    boolean mIgnoreCheckEvents;
    private boolean mIsVirtual;
    private boolean mHasPrivileged;
    private final boolean mIsAdvancedMode;

    private final boolean mComputeFolderStatistics;
    private FolderUsageExecutable mFolderUsageExecutable;
    private FolderUsage mFolderUsage;
    /**
     * @hide
     */
    boolean mDrawingFolderUsage;

    private DialogInterface.OnDismissListener mOnDismissListener;

    /**
     * Constructor of <code>FsoPropertiesDialog</code>.
     *
     * @param context The current context
     * @param fso The file system object reference
     */
    public FsoPropertiesDialog(Context context, FileSystemObject fso) {
        super();

        //Save the context
        this.mContext = context;

        //Save data
        this.mFso = fso;
        this.mHasChanged = false;
        this.mIgnoreCheckEvents = true;
        this.mHasPrivileged = false;
        this.mIsAdvancedMode =
                FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;

        //Inflate the content
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContentView = li.inflate(R.layout.fso_properties_dialog, null);

        // Apply current theme
        applyTheme();

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        R.string.fso_properties_dialog_title,
                                        this.mContentView);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                this.mContext.getString(android.R.string.ok),
                (DialogInterface.OnClickListener)null);
        this.mDialog.setOnCancelListener(this);
        this.mDialog.setOnDismissListener(this);

        // Retrieve the user settings about computing folder statistics
        this.mComputeFolderStatistics =
                Preferences.getSharedPreferences().
                    getBoolean(
                        FileManagerSettings.SETTINGS_COMPUTE_FOLDER_STATISTICS.getId(),
                        ((Boolean)FileManagerSettings.SETTINGS_COMPUTE_FOLDER_STATISTICS.
                                getDefaultValue()).booleanValue());

        //Fill the dialog
        fillData(this.mContentView);
    }

    /**
     * Set a listener to be invoked when the dialog is dismissed.
     *
     * @param onDismissListener The {@link "OnDismissListener"} to use.
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.mOnDismissListener = onDismissListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
    }

    /**
     * Method that return the associated {@link FileSystemObject} reference
     *
     * @return FileSystemObject The fso
     */
    public FileSystemObject getFso() {
        return this.mFso;
    }

    /**
     * Method that returns if the properties of the file was changed
     *
     * @return boolean If the properties of the file was changed
     */
    public boolean isHasChanged() {
        return this.mHasChanged;
    }

    /**
     * Method that fill the dialog with the data of the mount point.
     *
     * @param contentView The content view
     */
    private void fillData(View contentView) {
        //Get the tab views
        this.mInfoViewTab = contentView.findViewById(R.id.fso_properties_dialog_tab_info);
        this.mPermissionsViewTab =
                contentView.findViewById(R.id.fso_properties_dialog_tab_permissions);
        this.mInfoView = contentView.findViewById(R.id.fso_tab_info);
        this.mPermissionsView = contentView.findViewById(R.id.fso_tab_permissions);

        //Register the listeners
        this.mInfoViewTab.setOnClickListener(this);
        this.mPermissionsViewTab.setOnClickListener(this);

        //Gets text views
        TextView tvName = (TextView)contentView.findViewById(R.id.fso_properties_name);
        TextView tvParent = (TextView)contentView.findViewById(R.id.fso_properties_parent);
        TextView tvType = (TextView)contentView.findViewById(R.id.fso_properties_type);
        View vCategoryRow = contentView.findViewById(R.id.fso_properties_category_row);
        TextView tvCategory = (TextView)contentView.findViewById(R.id.fso_properties_category);
        View vLinkRow = contentView.findViewById(R.id.fso_properties_link_row);
        TextView tvLink = (TextView)contentView.findViewById(R.id.fso_properties_link);
        this.mTvSize = (TextView)contentView.findViewById(R.id.fso_properties_size);
        View vContatinsRow = contentView.findViewById(R.id.fso_properties_contains_row);
        this.mTvContains = (TextView)contentView.findViewById(R.id.fso_properties_contains);
        TextView tvLastAccessedTime =
                (TextView)contentView.findViewById(R.id.fso_properties_last_accessed);
        TextView tvLastModifiedTime =
                (TextView)contentView.findViewById(R.id.fso_properties_last_modified);
        TextView tvLastChangedTime =
                (TextView)contentView.findViewById(R.id.fso_properties_last_changed);
        this.mChkNoMedia = (CheckBox)contentView.findViewById(R.id.fso_include_in_media_scan);
        this.mSpnOwner = (Spinner)contentView.findViewById(R.id.fso_properties_owner);
        this.mSpnGroup = (Spinner)contentView.findViewById(R.id.fso_properties_group);
        this.mInfoMsgView = (TextView)contentView.findViewById(R.id.fso_info_msg);

        //Fill the text views
        //- Info
        tvName.setText(this.mFso.getName());
        if (FileHelper.isRootDirectory(this.mFso)) {
            tvParent.setText("-"); //$NON-NLS-1$
        } else {
            tvParent.setText(this.mFso.getParent());
        }
        tvType.setText(MimeTypeHelper.getMimeTypeDescription(this.mContext, this.mFso));
        if (this.mFso instanceof Symlink) {
            Symlink link = (Symlink)this.mFso;
            if (link.getLinkRef() != null) {
                tvLink.setText(link.getLinkRef().getFullPath());
            } else {
                tvLink.setText("-"); //$NON-NLS-1$
            }
        }
        MimeTypeCategory category = MimeTypeHelper.getCategory(this.mContext, this.mFso);
        if (category.compareTo(MimeTypeCategory.NONE) == 0) {
            vCategoryRow.setVisibility(View.GONE);
        } else {
            tvCategory.setText(
                    MimeTypeHelper.getCategoryDescription(
                            this.mContext, category));
        }
        vLinkRow.setVisibility(this.mFso instanceof Symlink ? View.VISIBLE : View.GONE);
        String size = FileHelper.getHumanReadableSize(this.mFso);
        if (size.length() == 0) {
            size = "-"; //$NON-NLS-1$
        }
        this.mTvSize.setText(size);
        this.mTvContains.setText("-");  //$NON-NLS-1$
        tvLastAccessedTime.setText(
                FileHelper.formatFileTime(this.mContext, this.mFso.getLastAccessedTime()));
        tvLastModifiedTime.setText(
                FileHelper.formatFileTime(this.mContext, this.mFso.getLastModifiedTime()));
        tvLastChangedTime.setText(
                FileHelper.formatFileTime(this.mContext, this.mFso.getLastChangedTime()));

        //- Permissions
        String loadingMsg = this.mContext.getString(R.string.loading_message);
        setSpinnerMsg(this.mContext, FsoPropertiesDialog.this.mSpnOwner, loadingMsg);
        setSpinnerMsg(this.mContext, FsoPropertiesDialog.this.mSpnGroup, loadingMsg);
        updatePermissions();

        // Load owners and groups AIDs in background
        loadAIDs();

        // Load owners and groups AIDs in background
        if (FileHelper.isDirectory(this.mFso)) {
            vContatinsRow.setVisibility(View.VISIBLE);
            if (this.mComputeFolderStatistics) {
                computeFolderUsage();
            }
        }

        // Check if permissions operations are allowed
        mIsVirtual = VirtualMountPointConsole.isVirtualStorageResource(mFso.getFullPath());
        try {
            this.mHasPrivileged = ConsoleBuilder.getConsole(this.mContext).isPrivileged();
        } catch (Throwable ex) {/**NON BLOCK**/}
        this.mSpnOwner.setEnabled(this.mHasPrivileged);
        this.mSpnGroup.setEnabled(this.mHasPrivileged);
        // Not allowed for symlinks
        if (!mIsVirtual && !(this.mFso instanceof Symlink)) {
            setCheckBoxesPermissionsEnable(this.mChkUserPermission, this.mHasPrivileged);
            setCheckBoxesPermissionsEnable(this.mChkGroupPermission, this.mHasPrivileged);
            setCheckBoxesPermissionsEnable(this.mChkOthersPermission, this.mHasPrivileged);
        } else {
            setCheckBoxesPermissionsEnable(this.mChkUserPermission, false);
            setCheckBoxesPermissionsEnable(this.mChkGroupPermission, false);
            setCheckBoxesPermissionsEnable(this.mChkOthersPermission, false);
        }
        if (!mIsVirtual && !this.mHasPrivileged && this.mIsAdvancedMode) {
            this.mInfoMsgView.setVisibility(View.VISIBLE);
            this.mInfoMsgView.setOnClickListener(this);
        }

        // Add the listener after set the value to avoid raising triggers
        this.mSpnOwner.setOnItemSelectedListener(this);
        this.mSpnGroup.setOnItemSelectedListener(this);
        setPermissionCheckBoxesListener(this.mChkUserPermission);
        setPermissionCheckBoxesListener(this.mChkGroupPermission);
        setPermissionCheckBoxesListener(this.mChkOthersPermission);

        // Check if we should show "Skip media scan" toggle
        if (!FileHelper.isDirectory(this.mFso) ||
            !StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())) {
            LinearLayout fsoSkipMediaScanView =
                    (LinearLayout)contentView.findViewById(R.id.fso_skip_media_scan_view);
            fsoSkipMediaScanView.setVisibility(View.GONE);
        } else {
            //attach the click events
            this.mChkNoMedia.setChecked(isNoMediaFilePresent());
            this.mChkNoMedia.setOnCheckedChangeListener(this);
        }

        //Change the tab
        onClick(this.mInfoViewTab);
        this.mIgnoreCheckEvents = false;
    }

    /**
     * Method that loads the AIDs in background
     */
    private void loadAIDs() {
        // Load owners and groups AIDs in background
        AsyncTask<Void, Void, SparseArray<AID>> aidsTask =
                        new AsyncTask<Void, Void, SparseArray<AID>>() {
            @Override
            protected SparseArray<AID> doInBackground(Void...params) {
                return AIDHelper.getAIDs(FsoPropertiesDialog.this.mContext, true);
            }

            @Override
            protected void onPostExecute(SparseArray<AID> aids) {
                if (!isCancelled()) {
                    // Ensure that at least one AID was loaded
                    if (aids == null) {
                        String errorMsg =
                                FsoPropertiesDialog.this.mContext.getString(R.string.error_message);
                        setSpinnerMsg(
                                FsoPropertiesDialog.this.mContext,
                                FsoPropertiesDialog.this.mSpnOwner, errorMsg);
                        setSpinnerMsg(
                                FsoPropertiesDialog.this.mContext,
                                FsoPropertiesDialog.this.mSpnGroup, errorMsg);
                        return;
                    }

                    // Position of the owner and group
                    int owner = FsoPropertiesDialog.this.mFso.getUser().getId();
                    int group = FsoPropertiesDialog.this.mFso.getGroup().getId();
                    int ownerPosition = 0;
                    int groupPosition = 0;

                    // Convert the SparseArray in an array of string of "uid - name"
                    int len = aids.size();
                    final String[] data = new String[len];
                    for (int i = 0; i < len; i++) {
                        AID aid = aids.valueAt(i);
                        data[i] = String.format(AID_FORMAT,
                                        Integer.valueOf(aid.getId()), aid.getName());
                        if (owner == aid.getId()) ownerPosition = i;
                        if (group == aid.getId()) groupPosition = i;
                    }

                    // Change the adapter of the spinners
                    setSpinnerData(
                            FsoPropertiesDialog.this.mContext,
                            FsoPropertiesDialog.this.mSpnOwner, data, ownerPosition);
                    setSpinnerData(
                            FsoPropertiesDialog.this.mContext,
                            FsoPropertiesDialog.this.mSpnGroup, data, groupPosition);
                }
            }
        };
        aidsTask.execute();
    }

    /**
     * Method that computes the disk usage of the folder in background
     */
    private void computeFolderUsage() {
        try {
            if (this.mFso instanceof Symlink && ((Symlink) this.mFso).getLinkRef() != null) {
                this.mFolderUsageExecutable =
                    CommandHelper.getFolderUsage(
                            this.mContext,
                            ((Symlink) this.mFso).getLinkRef().getFullPath(), this, null);
            } else {
                this.mFolderUsageExecutable =
                    CommandHelper.getFolderUsage(
                        this.mContext, this.mFso.getFullPath(), this, null);
            }
        } catch (Exception cause) {
            //Capture the exception
            ExceptionUtil.translateException(this.mContext, cause, true, false);
            this.mTvSize.setText(R.string.error_message);
            this.mTvContains.setText(R.string.error_message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        cancelFolderUsageCommand();
        if (this.mOnDismissListener != null) {
            this.mOnDismissListener.onDismiss(dialog);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        cancelFolderUsageCommand();
        if (this.mOnDismissListener != null) {
            this.mOnDismissListener.onDismiss(dialog);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fso_properties_dialog_tab_info:
                if (!this.mInfoViewTab.isSelected()) {
                    this.mInfoViewTab.setSelected(true);
                    ((TextView)this.mInfoViewTab).setTextAppearance(
                            this.mContext, R.style.primary_text_appearance);
                    this.mPermissionsViewTab.setSelected(false);
                    ((TextView)this.mPermissionsViewTab).setTextAppearance(
                            this.mContext, R.style.secondary_text_appearance);
                    this.mInfoView.setVisibility(View.VISIBLE);
                    this.mPermissionsView.setVisibility(View.GONE);

                    // Apply the them
                    applyTabTheme();
                }
                break;

            case R.id.fso_properties_dialog_tab_permissions:
                if (!this.mPermissionsViewTab.isSelected()) {
                    this.mInfoViewTab.setSelected(false);
                    ((TextView)this.mInfoViewTab).setTextAppearance(
                            this.mContext, R.style.secondary_text_appearance);
                    this.mPermissionsViewTab.setSelected(true);
                    ((TextView)this.mPermissionsViewTab).setTextAppearance(
                            this.mContext, R.style.primary_text_appearance);
                    this.mInfoView.setVisibility(View.GONE);
                    this.mPermissionsView.setVisibility(View.VISIBLE);

                    // Apply the them
                    applyTabTheme();

                    // Adjust the size of the spinners
                    adjustSpinnerSize(this.mSpnOwner);
                    adjustSpinnerSize(this.mSpnGroup);
                }
                this.mInfoMsgView.setVisibility(
                        mIsVirtual || this.mHasPrivileged || !this.mIsAdvancedMode
                        ? View.GONE : View.VISIBLE);
                break;

            case R.id.fso_info_msg:
                //Change the console
                boolean superuser = ConsoleBuilder.changeToPrivilegedConsole(this.mContext);
                if (superuser) {
                    this.mInfoMsgView.setOnClickListener(null);
                    this.mInfoMsgView.setVisibility(View.GONE);
                    this.mInfoMsgView.setBackground(null);

                    // Enable controls
                    this.mSpnOwner.setEnabled(true);
                    this.mSpnGroup.setEnabled(true);
                    setCheckBoxesPermissionsEnable(this.mChkUserPermission, true);
                    setCheckBoxesPermissionsEnable(this.mChkGroupPermission, true);
                    setCheckBoxesPermissionsEnable(this.mChkOthersPermission, true);
                    // Not allowed for symlinks
                    if (!(this.mFso instanceof Symlink)) {
                        setCheckBoxesPermissionsEnable(this.mChkUserPermission, true);
                        setCheckBoxesPermissionsEnable(this.mChkGroupPermission, true);
                        setCheckBoxesPermissionsEnable(this.mChkOthersPermission, true);
                    }
                    this.mHasPrivileged = true;
                }
                break;

            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.fso_include_in_media_scan:
                onNoMediaCheckedChanged(buttonView, isChecked);
                break;

            default:
                onPermissionsCheckedChanged(buttonView, isChecked);
                break;
        }
    }

    /**
     * Method that manage a check changed event
     *
     * @param buttonView The checkbox
     * @param isChecked If the checkbox is checked
     */
    private void onNoMediaCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mIgnoreCheckEvents) {
            this.mIgnoreCheckEvents = false;
            return;
        }
        // Checked means "skip media scan"
        final File nomedia = FileHelper.getNoMediaFile(this.mFso);
        if (isChecked) {
            preventMediaScan(nomedia);
        } else {
            allowMediaScan(nomedia);
        }
    }

    /**
     * Method that manage a check changed event
     *
     * @param buttonView The checkbox
     * @param isChecked If the checkbox is checked
     */
    private void onPermissionsCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mIgnoreCheckEvents) return;
        try {
            // Cancel the folder usage command
            cancelFolderUsageCommand();

            // Retrieve the permissions and send to operating system
            Permissions permissions = getPermissions();
            if (!CommandHelper.changePermissions(
                    this.mContext, this.mFso.getFullPath(), permissions, null)) {
                // Show the warning message
                setMsg(this.mContext.getString(
                        R.string.fso_properties_failed_to_change_permission_msg));

                // Update the permissions with the previous information
                updatePermissions();
                return;
            }

            // Some filesystem, like sdcards, doesn't allow to change the permissions.
            // But the system doesn't return the fail. Read again the fso and compare to
            // ensure that the permission was changed
            try {
                FileSystemObject systemFso  =
                        CommandHelper.getFileInfo(
                                this.mContext, this.mFso.getFullPath(), false, null);
                if (systemFso == null || systemFso.getPermissions().compareTo(permissions) != 0) {
                    // Show the warning message
                    setMsg(FsoPropertiesDialog.this.mContext.getString(
                            R.string.fso_properties_failed_to_change_permission_msg));

                    // Update the permissions with the previous information
                    updatePermissions();
                    return;
                }
            } catch (Exception e) {
                // Show the warning message
                setMsg(FsoPropertiesDialog.this.mContext.getString(
                        R.string.fso_properties_failed_to_change_permission_msg));

                // Update the permissions with the previous information
                updatePermissions();
                return;
            }

            // The permission was changed. Refresh the information
            this.mFso.setPermissions(permissions);
            this.mHasChanged = true;
            setMsg(null);

        } catch (Exception ex) {
            // Capture the exception and show warning message
            ExceptionUtil.translateException(
                    this.mContext, ex, true, true, new ExceptionUtil.OnRelaunchCommandResult() {
                @Override
                public void onSuccess() {
                    // Hide the message
                    setMsg(null);
                }

                @Override
                public void onCancelled() {
                    // Update the permissions with the previous information
                    updatePermissions();
                    setMsg(null);
                }

                @Override
                public void onFailed(Throwable cause) {
                    // Show the warning message
                    setMsg(FsoPropertiesDialog.this.mContext.getString(
                            R.string.fso_properties_failed_to_change_permission_msg));

                    // Update the permissions with the previous information
                    updatePermissions();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        User user = null;
        Group group = null;
        String msg = null;

        try {
            // Apply theme
            Theme theme = ThemeManager.getCurrentTheme(this.mContext);
            theme.setTextColor(
                    this.mContext, ((TextView) parent.getChildAt(0)), "text_color"); //$NON-NLS-1$

            String row = parent.getItemAtPosition(position).toString();
            int uid = Integer.parseInt(row.substring(0, row.indexOf(AID_SEPARATOR)));
            String name = row.substring(row.indexOf(AID_SEPARATOR) + 3);

            // Check which spinner was changed
            switch (parent.getId()) {
                case R.id.fso_properties_owner:
                    //Owner
                    user = new User(uid, name);
                    group = this.mFso.getGroup();
                    msg = this.mContext.getString(
                            R.string.fso_properties_failed_to_change_owner_msg);
                    break;
                case R.id.fso_properties_group:
                    //Group
                    user = this.mFso.getUser();
                    group = new Group(uid, name);
                    msg = this.mContext.getString(
                            R.string.fso_properties_failed_to_change_group_msg);
                    break;

                default:
                    break;
            }
        } catch (Exception ex) {
            // Capture the exception
            ExceptionUtil.translateException(this.mContext, ex);

            // Exit from dialog. The dialog may have inconsistency
            this.mDialog.dismiss();
            return;
        }

        // Has changed?
        if (this.mFso.getUser().compareTo(user) == 0 &&
             this.mFso.getGroup().compareTo(group) == 0) {
            return;
        }

        // Cancel the folder usage command
        cancelFolderUsageCommand();

        // Change the owner and group of the fso
        try {
            if (!CommandHelper.changeOwner(
                    this.mContext, this.mFso.getFullPath(), user, group, null)) {
                // Show the warning message
                setMsg(msg);

                // Update the information of owner and group
                updateSpinnerFromAid(this.mSpnOwner, this.mFso.getUser());
                updateSpinnerFromAid(this.mSpnGroup, this.mFso.getGroup());
                return;
            }

            //Change the fso reference
            this.mFso.setUser(user);
            this.mFso.setGroup(group);
            this.mHasChanged = true;
            setMsg(null);

        } catch (Exception ex) {
            // Capture the exception and show warning message
            final String txtMsg = msg;
            ExceptionUtil.translateException(
                    this.mContext, ex, true, true, new ExceptionUtil.OnRelaunchCommandResult() {
                @Override
                public void onSuccess() {
                    // Hide the message
                    setMsg(null);
                }

                @Override
                public void onCancelled() {
                    // Update the information of owner and group
                    updateSpinnerFromAid(
                            FsoPropertiesDialog.this.mSpnOwner,
                            FsoPropertiesDialog.this.mFso.getUser());
                    updateSpinnerFromAid(
                            FsoPropertiesDialog.this.mSpnGroup,
                            FsoPropertiesDialog.this.mFso.getGroup());
                    setMsg(null);
                }

                @Override
                public void onFailed(Throwable cause) {
                    setMsg(txtMsg);

                    // Update the information of owner and group
                    updateSpinnerFromAid(
                            FsoPropertiesDialog.this.mSpnOwner,
                            FsoPropertiesDialog.this.mFso.getUser());
                    updateSpinnerFromAid(
                            FsoPropertiesDialog.this.mSpnGroup,
                            FsoPropertiesDialog.this.mFso.getGroup());
                    return;
                }
            });

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {/**NON BLOCK**/}

    /**
     * Method that shows a simple message on the spinner (loading, error, ...)
     *
     * @param ctx The current context
     * @param spinner The spinner
     * @param msg The message
     * @hide
     */
    static void setSpinnerMsg(Context ctx, Spinner spinner, String msg) {
        ArrayAdapter<String> loadingAdapter =
                new ArrayAdapter<String>(
                        ctx, R.layout.spinner_item, new String[]{msg});
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(loadingAdapter);
        spinner.setEnabled(false);
    }

    /**
     * Method that fills the spinner with the data
     *
     * @param ctx The current context
     * @param spinner The spinner
     * @param data The data
     * @param selection The object to select
     * @hide
     */
    void setSpinnerData(
            Context ctx, Spinner spinner, String[] data, int selection) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        ctx, R.layout.spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selection);
        spinner.setEnabled(this.mHasPrivileged);
    }

    /**
     * Method that update a spinner from an {@link AID} reference
     *
     * @param spinner The spinner to update
     * @param aid The {@link AID} reference
     */
    @SuppressWarnings({"unchecked", "boxing"})
    public static void updateSpinnerFromAid(Spinner spinner, AID aid) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>)spinner.getAdapter();
        int position = adapter.getPosition(String.format(AID_FORMAT, aid.getId(), aid.getName()));
        if (position != -1) {
            spinner.setSelection(position);
        }
    }

    /**
     * Method that refresh the information of permissions
     * @hide
     */
    void updatePermissions() {
        // Update the permissions with the previous information
        FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
        try {
            Permissions permissions = this.mFso.getPermissions();
            this.mChkUserPermission =
                    loadCheckBoxUserPermission(
                            this.mContext, this.mContentView, permissions.getUser());
            this.mChkGroupPermission =
                    loadCheckBoxGroupPermission(
                            this.mContext, this.mContentView, permissions.getGroup());
            this.mChkOthersPermission =
                    loadCheckBoxOthersPermission(
                            this.mContext, this.mContentView, permissions.getOthers());
        } finally {
            FsoPropertiesDialog.this.mIgnoreCheckEvents = false;
        }
    }

    /**
     * Method that load the checkboxes for a user permission
     *
     * @param ctx The current context
     * @param rootView The root view
     * @return UserPermission The user permission
     */
    private static CheckBox[] loadCheckBoxUserPermission (
            Context ctx, View rootView, UserPermission permission) {
        CheckBox[] chkPermissions = loadPermissionCheckBoxes(ctx, rootView, OWNER_TYPE);
        chkPermissions[0].setChecked(permission.isSetUID());
        setCheckBoxesPermissions(chkPermissions, permission);
        return chkPermissions;
    }

    /**
     * Method that load the checkboxes for a group permission
     *
     * @param ctx The current context
     * @param rootView The root view
     * @return UserPermission The user permission
     */
    private static CheckBox[] loadCheckBoxGroupPermission (
            Context ctx, View rootView, GroupPermission permission) {
        CheckBox[] chkPermissions = loadPermissionCheckBoxes(ctx, rootView, GROUP_TYPE);
        chkPermissions[0].setChecked(permission.isSetGID());
        setCheckBoxesPermissions(chkPermissions, permission);
        return chkPermissions;
    }

    /**
     * Method that load the checkboxes for a group permission
     *
     * @param ctx The current context
     * @param rootView The root view
     * @return UserPermission The user permission
     */
    private static CheckBox[] loadCheckBoxOthersPermission (
            Context ctx, View rootView, OthersPermission permission) {
        CheckBox[] chkPermissions = loadPermissionCheckBoxes(ctx, rootView, OTHERS_TYPE);
        chkPermissions[0].setChecked(permission.isStickybit());
        setCheckBoxesPermissions(chkPermissions, permission);
        return chkPermissions;
    }

    /**
     * Method that check/uncheck the common permission for a permission checkboxes
     *
     * @param chkPermissions The checkboxes
     * @param permission The permission
     */
    private static void setCheckBoxesPermissions (
            CheckBox[] chkPermissions, Permission permission) {
        chkPermissions[1].setChecked(permission.isRead());
        chkPermissions[2].setChecked(permission.isWrite());
        chkPermissions[3].setChecked(permission.isExecute());
    }

    /**
     * Method that check/uncheck the common permission for a permission checkboxes
     *
     * @param chkPermissions The checkboxes
     * @param enabled If the checkbox should be enabled
     */
    private static void setCheckBoxesPermissionsEnable (
            CheckBox[] chkPermissions, boolean enabled) {
        int cc = chkPermissions.length;
        for (int i = 0; i < cc; i++) {
            chkPermissions[i].setEnabled(enabled);
        }
    }

    /**
     * Method that load the checkboxes associated with a type of permission
     *
     * @param ctx The current context
     * @param rootView The root view
     * @param type The type of permission [owner, group, others]
     * @return CheckBox[] The checkboxes associated
     */
    private static CheckBox[] loadPermissionCheckBoxes(Context ctx, View rootView, String type) {
        Resources res = ctx.getResources();
        CheckBox[] chkPermissions = new CheckBox[4];
        chkPermissions[0] = (CheckBox)rootView.findViewById(
                ResourcesHelper.getIdentifier(
                        res, "id",  //$NON-NLS-1$
                        String.format("fso_permissions_%s_special", type))); //$NON-NLS-1$
        chkPermissions[1] = (CheckBox)rootView.findViewById(
                ResourcesHelper.getIdentifier(
                        res, "id",  //$NON-NLS-1$
                        String.format("fso_permissions_%s_read", type))); //$NON-NLS-1$
        chkPermissions[2] = (CheckBox)rootView.findViewById(
                ResourcesHelper.getIdentifier(
                        res, "id",  //$NON-NLS-1$
                        String.format("fso_permissions_%s_write", type))); //$NON-NLS-1$
        chkPermissions[3] = (CheckBox)rootView.findViewById(
                ResourcesHelper.getIdentifier(
                        res, "id",  //$NON-NLS-1$
                        String.format("fso_permissions_%s_execute", type))); //$NON-NLS-1$
        return chkPermissions;
    }

    /**
     * Method that sets the listener for the permission checkboxes
     *
     * @param chkPermissions The checkboxes
     */
    private void setPermissionCheckBoxesListener(CheckBox[] chkPermissions) {
        int cc = chkPermissions.length;
        for (int i = 0; i < cc; i++) {
            chkPermissions[i].setOnCheckedChangeListener(this);
        }
    }

    /**
     * Method that retrieves the current permissions selected by the user
     *
     * @return Permissions The permissions selected by the user
     */
    private Permissions getPermissions() {
        UserPermission userPermission =
                new UserPermission(
                        this.mChkUserPermission[1].isChecked(),
                        this.mChkUserPermission[2].isChecked(),
                        this.mChkUserPermission[3].isChecked(),
                        this.mChkUserPermission[0].isChecked());
        GroupPermission groupPermission =
                new GroupPermission(
                        this.mChkGroupPermission[1].isChecked(),
                        this.mChkGroupPermission[2].isChecked(),
                        this.mChkGroupPermission[3].isChecked(),
                        this.mChkGroupPermission[0].isChecked());
        OthersPermission othersPermission =
                new OthersPermission(
                        this.mChkOthersPermission[1].isChecked(),
                        this.mChkOthersPermission[2].isChecked(),
                        this.mChkOthersPermission[3].isChecked(),
                        this.mChkOthersPermission[0].isChecked());
        Permissions permissions =
                new Permissions(userPermission, groupPermission, othersPermission);
        return permissions;
    }

    /**
     * Method that set a message in the dialog. If the message is {@link null} then
     * the view is hidden
     *
     * @param msg The message to show. {@link null} to hide the dialog
     * @hide
     */
    void setMsg(String msg) {
        this.mInfoMsgView.setText(msg);
        this.mInfoMsgView.setVisibility(
                mIsVirtual || !this.mIsAdvancedMode || (this.mHasPrivileged && msg == null) ?
                        View.GONE :
                        View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncStart() {
        this.mDrawingFolderUsage = false;
        this.mFolderUsage = new FolderUsage(this.mFso.getFullPath());
        printFolderUsage(true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncEnd(final boolean cancelled) {
        try {
            // Clone the reference
            FsoPropertiesDialog.this.mFolderUsage =
                    (FolderUsage)this.mFolderUsageExecutable.getFolderUsage().clone();
            printFolderUsage(true, cancelled);
        } catch (Exception ex) {/**NON BLOCK**/}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPartialResult(final Object partialResults) {
        try {
            // Do not saturate ui thread
            if (this.mDrawingFolderUsage) {
                return;
            }

            // Clone the reference
            FsoPropertiesDialog.this.mFolderUsage =
                    (FolderUsage)(((FolderUsage)partialResults).clone());
            printFolderUsage(true, false);
        } catch (Exception ex) {/**NON BLOCK**/}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncExitCode(int exitCode) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(Exception cause) {
        //Capture the exception
        ExceptionUtil.translateException(this.mContext, cause);
    }

    /**
     * Method that cancels the folder usage command execution
     */
    private void cancelFolderUsageCommand() {
        if (this.mComputeFolderStatistics) {
            // Cancel the folder usage command
            try {
                if (this.mFolderUsageExecutable != null &&
                    this.mFolderUsageExecutable.isCancellable() &&
                    !this.mFolderUsageExecutable.isCancelled()) {
                    this.mFolderUsageExecutable.cancel();
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to cancel the folder usage command", ex); //$NON-NLS-1$
            }
        }
    }

    /**
     * Method that redraws the information about folder usage
     *
     * @param computing If the process if computing the data
     * @param cancelled If the process was cancelled
     */
    private void printFolderUsage(final boolean computing, final boolean cancelled) {
        // Mark that a drawing is in progress
        this.mDrawingFolderUsage = true;

        final Resources res = this.mContext.getResources();
        if (cancelled) {
            try {
                FsoPropertiesDialog.this.mTvSize.setText(R.string.cancelled_message);
                FsoPropertiesDialog.this.mTvContains.setText(R.string.cancelled_message);
            } catch (Throwable e) {/**NON BLOCK**/}

            // End of drawing
            this.mDrawingFolderUsage = false;
        } else {
            // Calculate size prior to use ui thread
            final String size = FileHelper.getHumanReadableSize(this.mFolderUsage.getTotalSize());

            // Compute folders and files string
            String folders = res.getQuantityString(
                                        R.plurals.n_folders,
                                        this.mFolderUsage.getNumberOfFolders(),
                                        Integer.valueOf(this.mFolderUsage.getNumberOfFolders()));
            String files = res.getQuantityString(
                                        R.plurals.n_files,
                                        this.mFolderUsage.getNumberOfFiles(),
                                        Integer.valueOf(this.mFolderUsage.getNumberOfFiles()));
            final String contains = res.getString(
                                        R.string.fso_properties_dialog_folder_items,
                                        folders, files);

            // Update the dialog
            ((Activity)this.mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (computing) {
                        FsoPropertiesDialog.this.mTvSize.setText(
                                res.getString(R.string.computing_message, size));
                        FsoPropertiesDialog.this.mTvContains.setText(
                                res.getString(R.string.computing_message_ln, contains));
                    } else {
                        FsoPropertiesDialog.this.mTvSize.setText(size);
                        FsoPropertiesDialog.this.mTvContains.setText(contains);
                    }

                    // End of drawing
                    FsoPropertiesDialog.this.mDrawingFolderUsage = false;
                }
            });
        }
    }

    /**
     * Method that adjust the size of the spinner to fit the window
     *
     * @param spinner The spinner
     */
    private void adjustSpinnerSize(final Spinner spinner) {
        final View v = this.mContentView.findViewById(R.id.fso_properties_dialog_tabhost);
        spinner.post(new Runnable() {
            @Override
            public void run() {
                // Align with the last checkbox of the column
                int vW = v.getMeasuredWidth();
                int[] cbSpn = new int[2];
                spinner.getLocationInWindow(cbSpn);

                // Set the width
                spinner.getLayoutParams().width = vW - cbSpn[0];
            }
        });
    }

    /**
     * Method that applies the current theme to the activity
     */
    private void applyTheme() {
        Theme theme = ThemeManager.getCurrentTheme(this.mContext);
        theme.setBackgroundDrawable(
                this.mContext, this.mContentView, "background_drawable"); //$NON-NLS-1$
        applyTabTheme();
        View v = this.mContentView.findViewById(R.id.fso_properties_dialog_tab_divider1);
        theme.setBackgroundColor(this.mContext, v, "horizontal_divider_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_dialog_tab_divider2);
        theme.setBackgroundColor(this.mContext, v, "vertical_divider_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_dialog_tab_divider3);
        theme.setBackgroundColor(this.mContext, v, "vertical_divider_color"); //$NON-NLS-1$

        v = this.mContentView.findViewById(R.id.fso_properties_name_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_name);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_parent_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_parent);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_type_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_type);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_category_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_category);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_link_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_link);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_size_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_size);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_contains_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_contains);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_accessed_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_accessed);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_modified_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_modified);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_changed_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_last_changed);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_include_in_media_scan_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$

        v = this.mContentView.findViewById(R.id.fso_properties_owner_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_group_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_special_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_read_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_write_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_execute_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_owner_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_group_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_permissions_others_label);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_info_msg);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        ((TextView)v).setCompoundDrawablesWithIntrinsicBounds(
                theme.getDrawable(this.mContext, "filesystem_warning_drawable"), //$NON-NLS-1$
                null, null, null);
    }

    /**
     * Method that applies the current theme to the tab host
     */
    private void applyTabTheme() {
        // Apply the theme
        Theme theme = ThemeManager.getCurrentTheme(this.mContext);
        View v = this.mContentView.findViewById(R.id.fso_properties_dialog_tab_info);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
        v = this.mContentView.findViewById(R.id.fso_properties_dialog_tab_permissions);
        theme.setTextColor(this.mContext, (TextView)v, "text_color"); //$NON-NLS-1$
    }

    /**
     * Method that prevents media scan in the directory (creates a new .nomedia file)
     *
     * @param nomedia The .nomedia file
     */
    private void preventMediaScan(final File nomedia) {
        // Create .nomedia file. The file should not exist here
        try {
            if (!nomedia.createNewFile()) {
                // failed to create .nomedia file
                DialogHelper.showToast(
                    this.mContext,
                    this.mContext.getString(
                            R.string.fso_failed_to_prevent_media_scan),
                    Toast.LENGTH_SHORT);
                this.mIgnoreCheckEvents = true;
                this.mChkNoMedia.setChecked(false);
                return;
            }

            // Refresh the listview
            this.mHasChanged = true;

        } catch (IOException ex) {
            // failed to create .nomedia file
            ExceptionUtil.translateException(this.mContext, ex, true, false, null);
            DialogHelper.showToast(
                this.mContext,
                this.mContext.getString(
                        R.string.fso_failed_to_prevent_media_scan),
                Toast.LENGTH_SHORT);
            this.mIgnoreCheckEvents = true;
            this.mChkNoMedia.setChecked(false);
        }
    }

    /**
     * Method that allows media scan in the directory (removes the .nomedia file)
     *
     * @param nomedia The .nomedia file
     */
    private void allowMediaScan(final File nomedia) {
        // Delete .nomedia file. The file should exist here

        // .nomedia is a directory? Then ask the user prior to remove completely the folder
        if (nomedia.isDirectory()) {
            // confirm removing the dir
            AlertDialog alert = DialogHelper.createYesNoDialog(
                this.mContext,
                R.string.fso_delete_nomedia_dir_title,
                R.string.fso_delete_nomedia_dir_body,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            boolean ret = FileHelper.deleteFolder(nomedia);
                            if (!ret) {
                                DialogHelper.showToast(
                                    FsoPropertiesDialog.this.mContext,
                                    FsoPropertiesDialog.this.mContext.getString(
                                            R.string.fso_failed_to_allow_media_scan),
                                    Toast.LENGTH_SHORT);
                                FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
                                FsoPropertiesDialog.this.mChkNoMedia.setChecked(true);
                                return;
                            }

                            // Refresh the listview
                            FsoPropertiesDialog.this.mHasChanged = true;

                        } else {
                            FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
                            FsoPropertiesDialog.this.mChkNoMedia.setChecked(true);
                        }
                    }
                });
            DialogHelper.delegateDialogShow(this.mContext, alert);

        // .nomedia file is not empty?  Then ask the user prior to remove the file
        } else if (nomedia.length() != 0) {
            // confirm removing non empty file
            AlertDialog alert = DialogHelper.createYesNoDialog(
                this.mContext,
                R.string.fso_delete_nomedia_non_empty_title,
                R.string.fso_delete_nomedia_non_empty_body,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (!nomedia.delete()) {
                                DialogHelper.showToast(
                                    FsoPropertiesDialog.this.mContext,
                                    FsoPropertiesDialog.this.mContext.getString(
                                            R.string.fso_failed_to_allow_media_scan),
                                    Toast.LENGTH_SHORT);
                                FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
                                FsoPropertiesDialog.this.mChkNoMedia.setChecked(true);
                                return;
                            }

                            // Refresh the listview
                            FsoPropertiesDialog.this.mHasChanged = true;

                        } else {
                            FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
                            FsoPropertiesDialog.this.mChkNoMedia.setChecked(true);
                        }
                    }
                });
            DialogHelper.delegateDialogShow(this.mContext, alert);

        // Normal .nomedia file
        } else {
            if (!nomedia.delete()) {
                //failed to delete .nomedia file
                DialogHelper.showToast(
                    this.mContext,
                    this.mContext.getString(
                            R.string.fso_failed_to_allow_media_scan),
                    Toast.LENGTH_SHORT);
                FsoPropertiesDialog.this.mIgnoreCheckEvents = true;
                FsoPropertiesDialog.this.mChkNoMedia.setChecked(true);
                return;
            }

            // Refresh the listview
            FsoPropertiesDialog.this.mHasChanged = true;
        }
    }

    /**
     * Method that checks if the .nomedia file is present
     *
     * @return boolean If the .nomedia file is present
     */
    private boolean isNoMediaFilePresent() {
        final File nomedia = FileHelper.getNoMediaFile(this.mFso);
        return nomedia.exists();
    }

}
