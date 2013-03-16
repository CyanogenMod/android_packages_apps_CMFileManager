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

package com.cyanogenmod.filemanager.ui.policy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.CompressExecutable;
import com.cyanogenmod.filemanager.commands.UncompressExecutable;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.RelaunchableException;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.listeners.OnSelectionListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.CompressionMode;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.FixedQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class with the convenience methods for resolve compress/uncompress related actions
 */
public final class CompressActionPolicy extends ActionsPolicy {

    /**
     * A class that holds a listener for compression/uncompression operations
     */
    private static class CompressListener implements AsyncResultListener {

        final FixedQueue<String> mQueue;
        boolean mEnd;
        Throwable mCause;

        /**
         * Constructor of <code>CompressListener</code>
         */
        public CompressListener() {
            super();
            this.mEnd = false;
            this.mQueue = new FixedQueue<String>(2); //Holds only one item
            this.mCause = null;
        }

        @Override
        public void onPartialResult(Object result) {
            this.mQueue.insert((String)result);
        }

        @Override
        public void onException(Exception cause) {
            this.mCause = cause;
        }

        @Override
        public void onAsyncStart() {/**NON BLOCK**/}

        @Override
        public void onAsyncEnd(boolean cancelled) {/**NON BLOCK**/}

        @Override
        public void onAsyncExitCode(int exitCode) {
            this.mEnd = true;
        }
    }

    /**
     * Method that compresses the list of files of the selection.
     *
     * @param ctx The current context
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     * @hide
     */
    public static void compress(
            final Context ctx,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // Retrieve the current selection
        final List<FileSystemObject> selection = onSelectionListener.onRequestSelectedFiles();
        if (selection != null && selection.size() > 0) {
            // Show a dialog to allow the user make the compression mode choice
            final String[] labels = getSupportedCompressionModesLabels(ctx, selection);
            AlertDialog dialog = DialogHelper.createSingleChoiceDialog(
                    ctx, R.string.compression_mode_title,
                    labels,
                    CompressionMode.AC_GZIP.ordinal(),
                    new DialogHelper.OnSelectChoiceListener() {
                        @Override
                        public void onSelectChoice(int choice) {
                            // Do the compression
                            compress(
                                    ctx,
                                    getCompressionModeFromUserChoice(ctx, labels, choice),
                                    selection,
                                    onSelectionListener,
                                    onRequestRefreshListener);
                        }

                        @Override
                        public void onNoSelectChoice() {/**NON BLOCK**/}
                    });
            DialogHelper.delegateDialogShow(ctx, dialog);
        }
    }

    /**
     * Method that compresses an uncompressed file.
     *
     * @param ctx The current context
     * @param fso The compressed file
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     * @hide
     */
    public static void compress(
            final Context ctx, final FileSystemObject fso,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // Create a list with the item
        final List<FileSystemObject> items = new ArrayList<FileSystemObject>();
        items.add(fso);

        // Show a dialog to allow the user make the compression mode choice
        final String[] labels = getSupportedCompressionModesLabels(ctx, items);
        AlertDialog dialog = DialogHelper.createSingleChoiceDialog(
                ctx, R.string.compression_mode_title,
                getSupportedCompressionModesLabels(ctx, items),
                CompressionMode.AC_GZIP.ordinal(),
                new DialogHelper.OnSelectChoiceListener() {
                    @Override
                    public void onSelectChoice(int choice) {
                        // Do the compression
                        compress(
                                ctx,
                                getCompressionModeFromUserChoice(ctx, labels, choice),
                                items,
                                onSelectionListener,
                                onRequestRefreshListener);
                    }

                    @Override
                    public void onNoSelectChoice() {/**NON BLOCK**/}
                });
        DialogHelper.delegateDialogShow(ctx, dialog);
    }


    /**
     * Method that compresses some uncompressed files or folders
     *
     * @param ctx The current context
     * @param mode The compression mode
     * @param fsos The list of files to compress
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     * @hide
     */
    static void compress(
            final Context ctx, final CompressionMode mode, final List<FileSystemObject> fsos,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // The callable interface
        final BackgroundCallable callable = new BackgroundCallable() {
            // The current items
            final Context mCtx = ctx;
            final CompressionMode mMode = mode;
            final List<FileSystemObject> mFsos = fsos;
            final OnRequestRefreshListener mOnRequestRefreshListener = onRequestRefreshListener;

            final Object mSync = new Object();
            Throwable mCause;
            CompressExecutable cmd = null;

            final CompressListener mListener =
                                new CompressListener();
            private String mMsg;
            private boolean mStarted = false;

            @Override
            public int getDialogTitle() {
                return R.string.waiting_dialog_compressing_title;
            }
            @Override
            public int getDialogIcon() {
                return 0;
            }
            @Override
            public boolean isDialogCancellable() {
                return true;
            }

            @Override
            public Spanned requestProgress() {
                // Initializing the dialog
                if (!this.mStarted) {
                    String progress =
                            this.mCtx.getResources().
                                getString(
                                    R.string.waiting_dialog_analizing_msg);
                    return Html.fromHtml(progress);
                }

                // Return the current operation
                String msg = (this.mMsg == null) ? "" : this.mMsg; //$NON-NLS-1$
                String progress =
                      this.mCtx.getResources().
                          getString(
                              R.string.waiting_dialog_compressing_msg,
                              msg);
                return Html.fromHtml(progress);
            }

            @Override
            public void onSuccess() {
                try {
                    if (this.cmd != null && this.cmd.isCancellable() && !this.cmd.isCancelled()) {
                        this.cmd.cancel();
                    }
                } catch (Exception e) {/**NON BLOCK**/}

                //Operation complete. Refresh
                if (this.mOnRequestRefreshListener != null) {
                  // The reference is not the same, so refresh the complete navigation view
                  this.mOnRequestRefreshListener.onRequestRefresh(null, true);
                }
                if (this.cmd != null) {
                    showOperationSuccessMsg(
                            ctx,
                            R.string.msgs_compressing_success,
                            this.cmd.getOutCompressedFile());
                } else {
                    ActionsPolicy.showOperationSuccessMsg(ctx);
                }
            }

            @Override
            public void doInBackground(Object... params) throws Throwable {
                this.mCause = null;
                this.mStarted = true;

                // This method expect to receive
                // 1.- BackgroundAsyncTask
                BackgroundAsyncTask task = (BackgroundAsyncTask)params[0];
                String out = null;
                try {
                    // Archive or Archive-Compression
                    if (this.mMode.mArchive) {
                        // Convert the list to an array of full paths
                        String[] src = new String[this.mFsos.size()];
                        int cc = this.mFsos.size();
                        for (int i = 0; i < cc; i++) {
                            src[i] = this.mFsos.get(i).getFullPath();
                        }

                        // Use the current directory name for create the compressed file
                        String curDirName =
                                new File(onSelectionListener.onRequestCurrentDir()).getName();
                        if (src.length == 1) {
                            // But only one file is passed, then used the name of unique file
                            curDirName = FileHelper.getName(this.mFsos.get(0).getName());
                        }
                        String name =
                                String.format(
                                        "%s.%s", curDirName, this.mMode.mExtension); //$NON-NLS-1$
                        String newName =
                                FileHelper.createNonExistingName(
                                        ctx,
                                        onSelectionListener.onRequestCurrentItems(),
                                        name,
                                        R.string.create_new_compress_file_regexp);
                        String newNameAbs =
                                new File(
                                        onSelectionListener.onRequestCurrentDir(),
                                        newName).getAbsolutePath();

                        // Do the compression
                        this.cmd =
                           CommandHelper.compress(
                                ctx,
                                this.mMode,
                                newNameAbs,
                                src,
                                this.mListener, null);

                    // Compression
                    } else {
                        // Only the first item from the list is valid. If there are more in the
                        // list, then discard them
                        String src = this.mFsos.get(0).getFullPath();

                        // Do the compression
                        this.cmd =
                           CommandHelper.compress(
                                ctx,
                                this.mMode,
                                src,
                                this.mListener, null);
                    }
                    out = this.cmd.getOutCompressedFile();

                    // Request paint the
                    this.mListener.mQueue.insert(out);
                    task.onRequestProgress();

                    // Don't use an active blocking because this suppose that all message
                    // will be processed by the UI. Instead, refresh with a delay and
                    // display the active file
                    while (!this.mListener.mEnd) {
                        // Sleep to don't saturate the UI thread
                        Thread.sleep(50L);

                        List<String> msgs = this.mListener.mQueue.peekAll();
                        if (msgs.size() > 0) {
                            this.mMsg = msgs.get(msgs.size()-1);
                            task.onRequestProgress();
                        }
                    }

                    // Dialog is ended. Force the last redraw
                    List<String> msgs = this.mListener.mQueue.peekAll();
                    if (msgs.size() > 0) {
                        this.mMsg = msgs.get(msgs.size()-1);
                        task.onRequestProgress();
                    }

                } catch (Exception e) {
                    // Need to be relaunched?
                    if (e instanceof RelaunchableException) {
                        OnRelaunchCommandResult rl = new OnRelaunchCommandResult() {
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onSuccess() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }

                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onFailed(Throwable cause) {
                                mCause = cause;
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onCancelled() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                        };

                        // Translate the exception (and wait for the result)
                        ExceptionUtil.translateException(ctx, e, false, true, rl);
                        synchronized (this.mSync) {
                            this.mSync.wait();
                        }

                        // Persist the exception?
                        if (this.mCause != null) {
                            // The exception must be elevated
                            throw this.mCause;
                        }

                    } else {
                        // The exception must be elevated
                        throw e;
                    }
                }


                // Any exception?
                Thread.sleep(100L);
                if (this.mListener.mCause != null) {
                    throw this.mListener.mCause;
                }

                // Check that the operation was completed retrieving the compressed file or folder
                boolean failed = false;
                try {
                    FileSystemObject fso = CommandHelper.getFileInfo(ctx, out, false, null);
                    if (fso == null) {
                        // Failed. The file or folder not exists
                        failed = true;
                    }

                    // Operation complete successfully

                } catch (Throwable e) {
                    // Failed. The file or folder not exists
                    failed = true;
                }
                if (failed) {
                    throw new ExecutionException(
                            String.format(
                                    "Failed to compress file(s) to: %s", out)); //$NON-NLS-1$
                }
            }
        };
        final BackgroundAsyncTask task = new BackgroundAsyncTask(ctx, callable);

        // Check if the output exists. When the mode is archive, this method generate a new
        // name based in the current directory. When the mode is compressed then the name
        // is the name of the file to compress without extension. In this case the name should
        // be validate prior to compress
        boolean askUser = false;
        try {
            if (!mode.mArchive) {
                // Only the first item from the list is valid. If there are more in the
                // list, then discard them
                String src = fsos.get(0).getFullPath();
                CompressExecutable ucmd =
                        FileManagerApplication.getBackgroundConsole().
                            getExecutableFactory().newCreator().
                                createCompressExecutable(mode, src, null);
                String dst = ucmd.getOutCompressedFile();
                FileSystemObject info = CommandHelper.getFileInfo(ctx, dst, null);
                if (info != null) {
                    askUser = true;
                }
            }
        } catch (Exception e) {/**NON BLOCK**/}

        // Ask the user because the destination file or folder exists
        if (askUser) {
            //Show a dialog asking the user for overwrite the files
            AlertDialog dialog =
                    DialogHelper.createTwoButtonsQuestionDialog(
                            ctx,
                            android.R.string.cancel,
                            R.string.overwrite,
                            R.string.confirm_overwrite,
                            ctx.getString(R.string.msgs_overwrite_files),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface alertDialog, int which) {
                                    // NEGATIVE (overwrite)  POSITIVE (cancel)
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        // Execute background task
                                        task.execute(task);
                                    }
                                }
                           });
            DialogHelper.delegateDialogShow(ctx, dialog);
        } else {
            // Execute background task
            task.execute(task);
        }
    }

    /**
     * Method that uncompress a compressed file.
     *
     * @param ctx The current context
     * @param fso The compressed file
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     * @hide
     */
    public static void uncompress(
            final Context ctx, final FileSystemObject fso,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // The callable interface
        final BackgroundCallable callable = new BackgroundCallable() {
            // The current items
            final Context mCtx = ctx;
            final FileSystemObject mFso = fso;
            final OnRequestRefreshListener mOnRequestRefreshListener = onRequestRefreshListener;

            final Object mSync = new Object();
            Throwable mCause;
            UncompressExecutable cmd;

            final CompressListener mListener =
                                new CompressListener();
            private String mMsg;
            private boolean mStarted = false;

            @Override
            public int getDialogTitle() {
                return R.string.waiting_dialog_extracting_title;
            }
            @Override
            public int getDialogIcon() {
                return 0;
            }
            @Override
            public boolean isDialogCancellable() {
                return true;
            }

            @Override
            public Spanned requestProgress() {
                // Initializing the dialog
                if (!this.mStarted) {
                    String progress =
                            this.mCtx.getResources().
                                getString(
                                    R.string.waiting_dialog_analizing_msg);
                    return Html.fromHtml(progress);
                }

                // Return the current operation
                String msg = (this.mMsg == null) ? "" : this.mMsg; //$NON-NLS-1$
                String progress =
                      this.mCtx.getResources().
                          getString(
                              R.string.waiting_dialog_extracting_msg,
                              msg);
                return Html.fromHtml(progress);
            }

            @Override
            public void onSuccess() {
                try {
                    if (this.cmd != null && this.cmd.isCancellable() && !this.cmd.isCancelled()) {
                        this.cmd.cancel();
                    }
                } catch (Exception e) {/**NON BLOCK**/}

                //Operation complete. Refresh
                if (this.mOnRequestRefreshListener != null) {
                  // The reference is not the same, so refresh the complete navigation view
                  this.mOnRequestRefreshListener.onRequestRefresh(null, true);
                }
                if (this.cmd != null) {
                    showOperationSuccessMsg(
                            ctx,
                            R.string.msgs_extracting_success,
                            this.cmd.getOutUncompressedFile());
                } else {
                    ActionsPolicy.showOperationSuccessMsg(ctx);
                }
            }

            @Override
            public void doInBackground(Object... params) throws Throwable {
                this.mCause = null;
                this.mStarted = true;

                // This method expect to receive
                // 1.- BackgroundAsyncTask
                BackgroundAsyncTask task = (BackgroundAsyncTask)params[0];
                String out = null;
                try {
                    this.cmd =
                        CommandHelper.uncompress(
                                ctx,
                                this.mFso.getFullPath(),
                                null,
                                this.mListener, null);
                    out = this.cmd.getOutUncompressedFile();

                    // Request paint the
                    this.mListener.mQueue.insert(out);
                    task.onRequestProgress();

                    // Don't use an active blocking because this suppose that all message
                    // will be processed by the UI. Instead, refresh with a delay and
                    // display the active file
                    while (!this.mListener.mEnd) {
                        // Sleep to don't saturate the UI thread
                        Thread.sleep(50L);

                        List<String> msgs = this.mListener.mQueue.peekAll();
                        if (msgs.size() > 0) {
                            this.mMsg = msgs.get(msgs.size()-1);
                            task.onRequestProgress();
                        }
                    }

                    // Dialog is ended. Force the last redraw
                    List<String> msgs = this.mListener.mQueue.peekAll();
                    if (msgs.size() > 0) {
                        this.mMsg = msgs.get(msgs.size()-1);
                        task.onRequestProgress();
                    }

                } catch (Exception e) {
                    // Need to be relaunched?
                    if (e instanceof RelaunchableException) {
                        OnRelaunchCommandResult rl = new OnRelaunchCommandResult() {
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onSuccess() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }

                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onFailed(Throwable cause) {
                                mCause = cause;
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onCancelled() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                        };

                        // Translate the exception (and wait for the result)
                        ExceptionUtil.translateException(ctx, e, false, true, rl);
                        synchronized (this.mSync) {
                            this.mSync.wait();
                        }

                        // Persist the exception?
                        if (this.mCause != null) {
                            // The exception must be elevated
                            throw this.mCause;
                        }

                    } else {
                        // The exception must be elevated
                        throw e;
                    }
                }


                // Any exception?
                Thread.sleep(100L);
                if (this.mListener.mCause != null) {
                    throw this.mListener.mCause;
                }

                // Check that the operation was completed retrieving the uncompressed
                // file or folder
                boolean failed = false;
                try {
                    FileSystemObject fso2 = CommandHelper.getFileInfo(ctx, out, false, null);
                    if (fso2 == null) {
                        // Failed. The file or folder not exists
                        failed = true;
                    }

                    // Operation complete successfully

                } catch (Throwable e) {
                    // Failed. The file or folder not exists
                    failed = true;
                }
                if (failed) {
                    throw new ExecutionException(
                            String.format(
                                    "Failed to extract file: %s", //$NON-NLS-1$
                                    this.mFso.getFullPath()));
                }
            }
        };
        final BackgroundAsyncTask task = new BackgroundAsyncTask(ctx, callable);

        // Check if the output exists
        boolean askUser = false;
        try {
            UncompressExecutable ucmd =
                    FileManagerApplication.getBackgroundConsole().
                        getExecutableFactory().newCreator().
                            createUncompressExecutable(fso.getFullPath(), null, null);
            String dst = ucmd.getOutUncompressedFile();
            FileSystemObject info = CommandHelper.getFileInfo(ctx, dst, null);
            if (info != null) {
                askUser = true;
            }
        } catch (Exception e) {/**NON BLOCK**/}

        // Ask the user because the destination file or folder exists
        if (askUser) {
            //Show a dialog asking the user for overwrite the files
            AlertDialog dialog =
                    DialogHelper.createTwoButtonsQuestionDialog(
                            ctx,
                            android.R.string.cancel,
                            R.string.overwrite,
                            R.string.confirm_overwrite,
                            ctx.getString(R.string.msgs_overwrite_files),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface alertDialog, int which) {
                                    // NEGATIVE (overwrite)  POSITIVE (cancel)
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        // Check if the necessary to display a warning because
                                        // security issues
                                        checkZipSecurityWarning(ctx, task, fso);
                                    }
                                }
                           });
            DialogHelper.delegateDialogShow(ctx, dialog);
        } else {
            // Execute background task
            task.execute(task);
        }
    }

    /**
     * Method that checks if it is necessary to display a warning dialog because
     * the privileged extraction of a zip file.
     *
     * @param ctx The current context
     * @param task The task
     * @param fso The zip file
     * @hide
     */
    static void checkZipSecurityWarning(
            final Context ctx, final BackgroundAsyncTask task, FileSystemObject fso) {
        // WARNING! Extracting a ZIP file with relatives or absolutes path could break
        // the system and is need a security alert that the user can confirm prior to
        // make the extraction
        String ext = FileHelper.getExtension(fso);
        if (ConsoleBuilder.isPrivileged() && ext.compareToIgnoreCase("zip") == 0) { //$NON-NLS-1$
            AlertDialog dialog = DialogHelper.createYesNoDialog(
                ctx,
                R.string.confirm_overwrite,
                R.string.security_warning_extract,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface alertDialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Execute background task
                            task.execute(task);
                        }
                    }
               });
            dialog.show();
        } else {
            // Execute background task
            task.execute(task);
        }
    }

    /**
     * Method that returns the supported compression modes
     *
     * @param ctx The current context
     * @param fsos The list of file system objects to compress
     * @return String[] An array with the compression mode labels
     */
    private static String[] getSupportedCompressionModesLabels(
                                Context ctx, List<FileSystemObject> fsos) {
        String[] labels = ctx.getResources().getStringArray(R.array.compression_modes_labels);
        // If more than a file are requested, compression is not available
        // The same applies if the unique item is a folder
        if (fsos.size() > 1 || (fsos.size() == 1 && FileHelper.isDirectory(fsos.get(0)))) {
            ArrayList<String> validLabels = new ArrayList<String>();
            CompressionMode[] values = CompressionMode.values();
            int cc = values.length;
            for (int i = 0; i < cc; i++) {
                if (values[i].mArchive) {
                    if (values[i].mCommandId == null ||
                        FileManagerApplication.hasOptionalCommand(values[i].mCommandId)) {
                        validLabels.add(labels[i]);
                    }
                }
            }
            labels = validLabels.toArray(new String[]{});
        } else {
            // Remove optional commands
            ArrayList<String> validLabels = new ArrayList<String>();
            CompressionMode[] values = CompressionMode.values();
            int cc = values.length;
            for (int i = 0; i < cc; i++) {
                if (values[i].mCommandId == null ||
                    FileManagerApplication.hasOptionalCommand(values[i].mCommandId)) {
                    validLabels.add(labels[i]);
                }
            }
            labels = validLabels.toArray(new String[]{});
        }
        return labels;
    }

    /**
     * Method that returns the compression mode from the user choice
     *
     * @param ctx The current context
     * @param labels The dialog labels
     * @param choice The choice of the user
     * @return CompressionMode The compression mode
     */
    static CompressionMode getCompressionModeFromUserChoice(
            Context ctx, String[] labels, int choice) {
        String label = labels[choice];
        String[] allLabels = ctx.getResources().getStringArray(R.array.compression_modes_labels);
        CompressionMode[] values = CompressionMode.values();
        int cc = allLabels.length;
        for (int i = 0; i < cc; i++) {
            if (allLabels[i].compareTo(label) == 0) {
                return values[i];
            }
        }
        return null;
    }

    /**
     * Method that shows a message when the operation is complete successfully
     *
     * @param ctx The current context
     * @param res The resource identifier
     * @param dst The destination output
     * @hide
     */
    protected static void showOperationSuccessMsg(Context ctx, int res, String dst) {
        DialogHelper.showToast(ctx, ctx.getString(res, dst), Toast.LENGTH_SHORT);
    }
}