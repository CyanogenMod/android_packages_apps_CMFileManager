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

package com.cyanogenmod.filemanager.tasks;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.app.Activity;

import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.MountPointHelper;

/**
 * A class for recovery information about filesystem status (mount point, disk usage, ...).
 */
public class FilesystemAsyncTask extends AsyncTask<String, Integer, Boolean> {

    private static final String TAG = "FilesystemAsyncTask"; //$NON-NLS-1$

    /**
     * @hide
     */
    final Context mContext;
    /**
     * @hide
     */
    final ImageView mMountPointInfo;
    /**
     * @hide
     */
    final ProgressBar mDiskUsageInfo;
    /**
     * @hide
     */
    final int mFreeDiskSpaceWarningLevel;
    private boolean mRunning;

    /**
     * @hide
     */
    final boolean mIsDialog;

    /**
     * @hide
     */
    static int sColorFilterNormal;

    /**
     * Constructor of <code>FilesystemAsyncTask</code>.
     *
     * @param context The current context
     * @param mountPointInfo The mount point info view
     * @param diskUsageInfo The mount point info view
     * @param freeDiskSpaceWarningLevel The free disk space warning level
     */
    public FilesystemAsyncTask(
            Context context, ImageView mountPointInfo,
            ProgressBar diskUsageInfo, int freeDiskSpaceWarningLevel) {
        this(context, mountPointInfo, diskUsageInfo, freeDiskSpaceWarningLevel,
                false);
    }

    /**
     * Constructor of <code>FilesystemAsyncTask</code>.
     *
     * @param context The current context
     * @param mountPointInfo The mount point info view
     * @param diskUsageInfo The mount point info view
     * @param freeDiskSpaceWarningLevel The free disk space warning level
     * @param isDialog Whether or not to use dialog theme resources
     */
    public FilesystemAsyncTask(
            Context context, ImageView mountPointInfo,
            ProgressBar diskUsageInfo, int freeDiskSpaceWarningLevel, boolean isDialog) {
        super();
        this.mContext = context;
        this.mMountPointInfo = mountPointInfo;
        this.mDiskUsageInfo = diskUsageInfo;
        this.mFreeDiskSpaceWarningLevel = freeDiskSpaceWarningLevel;
        this.mRunning = false;
        this.mIsDialog = isDialog;
    }

    /**
     * Method that returns if there is a task running.
     *
     * @return boolean If there is a task running
     */
    public boolean isRunning() {
        return this.mRunning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean doInBackground(String... params) {
        //Running
        this.mRunning = true;

        //Extract the directory from arguments
        String dir = params[0];

        //Extract filesystem mount point from directory
        if (isCancelled()) {
            return Boolean.TRUE;
        }
        final MountPoint mp = MountPointHelper.getMountPointFromDirectory(dir);
        if (mp == null) {
            //There is no information about
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Theme theme = ThemeManager.getCurrentTheme(FilesystemAsyncTask.this.mContext);
                    theme.setImageDrawable(
                            FilesystemAsyncTask.this.mContext,
                            FilesystemAsyncTask.this.mMountPointInfo,
                            FilesystemAsyncTask.this.mIsDialog ?
                                    "filesystem_dialog_warning_drawable" //$NON-NLS-1$
                                    : "filesystem_warning_drawable"); //$NON-NLS-1$
                    FilesystemAsyncTask.this.mMountPointInfo.setTag(null);
                }
            });
        } else {
            //Set image icon an save the mount point info
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   String resource =
                            MountPointHelper.isReadOnly(mp)
                            ? FilesystemAsyncTask.this.mIsDialog ?
                                    "filesystem_dialog_locked_drawable" //$NON-NLS-1$
                                    : "filesystem_locked_drawable" //$NON-NLS-1$
                            : FilesystemAsyncTask.this.mIsDialog ?
                                    "filesystem_dialog_unlocked_drawable" //$NON-NLS-1$
                                    : "filesystem_unlocked_drawable"; //$NON-NLS-1$
                    Theme theme = ThemeManager.getCurrentTheme(FilesystemAsyncTask.this.mContext);
                    theme.setImageDrawable(
                            FilesystemAsyncTask.this.mContext,
                            FilesystemAsyncTask.this.mMountPointInfo,
                            resource);
                    FilesystemAsyncTask.this.mMountPointInfo.setTag(mp);
                }
            });

            //Load information about disk usage
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DiskUsage du = null;
                    try {
                         du = MountPointHelper.getMountPointDiskUsage(mp);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to retrieve disk usage information", e); //$NON-NLS-1$
                        du = new DiskUsage(
                                mp.getMountPoint(), 0, 0, 0);
                    }
                    int usage = 0;
                    if (du != null && du.getTotal() != 0) {
                        usage = (int)(du.getUsed() * 100 / du.getTotal());
                        FilesystemAsyncTask.this.mDiskUsageInfo.setProgress(usage);
                        FilesystemAsyncTask.this.mDiskUsageInfo.setTag(du);
                    } else {
                        usage = du == null ? 0 : 100;
                        FilesystemAsyncTask.this.mDiskUsageInfo.setProgress(usage);
                        FilesystemAsyncTask.this.mDiskUsageInfo.setTag(null);
                    }

                    // Advise about diskusage (>=mFreeDiskSpaceWarningLevel) with other color
                    Theme theme = ThemeManager.getCurrentTheme(FilesystemAsyncTask.this.mContext);
                    int filter =
                            usage >= FilesystemAsyncTask.this.mFreeDiskSpaceWarningLevel ?
                               theme.getColor(
                                       FilesystemAsyncTask.this.mContext,
                                       "disk_usage_filter_warning_color") : //$NON-NLS-1$
                               theme.getColor(
                                       FilesystemAsyncTask.this.mContext,
                                       "disk_usage_filter_normal_color"); //$NON-NLS-1$
                    FilesystemAsyncTask.this.mDiskUsageInfo.
                                getProgressDrawable().setColorFilter(
                                        new PorterDuffColorFilter(filter, Mode.MULTIPLY));
                }
            });
        }
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Boolean result) {
        this.mRunning = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled(Boolean result) {
        this.mRunning = false;
        super.onCancelled(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled() {
        this.mRunning = false;
        super.onCancelled();
    }

}
