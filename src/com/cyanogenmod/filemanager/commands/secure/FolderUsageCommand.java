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

package com.cyanogenmod.filemanager.commands.secure;

import android.util.Log;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.FolderUsage;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import de.schlichtherle.truezip.file.TFile;

/**
 * A class for retrieve the disk usage of a folder.
 */
public class FolderUsageCommand extends Program implements FolderUsageExecutable {

    private static final String TAG = "FolderUsage"; //$NON-NLS-1$

    private final String mDirectory;
    private final AsyncResultListener mAsyncResultListener;
    private final FolderUsage mFolderUsage;

    private boolean mCancelled;
    private boolean mEnded;
    private final Object mSync = new Object();

    /**
     * Constructor of <code>FolderUsageCommand</code>.
     *
     * @param console The secure console
     * @param directory The absolute directory to compute
     * @param asyncResultListener The partial result listener
     */
    public FolderUsageCommand(SecureConsole console, String directory,
            AsyncResultListener asyncResultListener) {
        super(console);
        this.mDirectory = directory;
        this.mAsyncResultListener = asyncResultListener;
        this.mFolderUsage = new FolderUsage(directory);
        this.mCancelled = false;
        this.mEnded = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsynchronous() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FolderUsage getFolderUsage() {
        return this.mFolderUsage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Computing folder usage for folder %s", //$NON-NLS-1$
                            this.mDirectory));
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncStart();
        }

        TFile f = getConsole().buildRealFile(mDirectory);
        if (!f.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(new NoSuchFileOrDirectory(this.mDirectory));
            }
        }
        if (!f.isDirectory()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(
                        new ExecutionException("path exists but it's not a folder")); //$NON-NLS-1$
            }
        }

        // Compute data recursively
        computeRecursive(f);

        synchronized (this.mSync) {
            this.mEnded = true;
            this.mSync.notify();
        }

        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncEnd(this.mCancelled);
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncExitCode(0);
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

    /**
     * Method that computes the folder usage recursively
     *
     * @param folder The folder where to start the computation
     */
    private void computeRecursive(TFile folder) {
        // Obtains the files and folders of the folders
        try {
            TFile[] files = folder.listFiles();
            int c = 0;
            if (files != null) {
                int cc = files.length;
                for (int i = 0; i < cc; i++) {
                    if (files[i].isDirectory()) {
                        this.mFolderUsage.addFolder();
                        computeRecursive(files[i]);
                    } else {
                        this.mFolderUsage.addFile();
                        // Compute statistics and size
                        MimeTypeCategory category =
                                MimeTypeHelper.getCategory(null, files[i]);
                        this.mFolderUsage.addFileToCategory(category);
                        this.mFolderUsage.addSize(files[i].length());
                    }

                    // Partial notification
                    if (c % 5 == 0) {
                        //If a listener is defined, then send the partial result
                        if (getAsyncResultListener() != null) {
                            getAsyncResultListener().onPartialResult(this.mFolderUsage);
                        }
                    }

                    // Check if the process was cancelled
                    try {
                        synchronized (this.mSync) {
                            if (this.mCancelled  || this.mEnded) {
                                this.mSync.notify();
                                break;
                            }
                        }
                    } catch (Exception e) {/**NON BLOCK**/}
                }
            }
        } finally {
            //If a listener is defined, then send the partial result
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(this.mFolderUsage);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        synchronized (this.mSync) {
            return this.mCancelled;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel() {
        try {
            synchronized (this.mSync) {
                if (this.mEnded || this.mCancelled) {
                    this.mCancelled = true;
                    return true;
                }
                this.mCancelled = true;
                this.mSync.wait(5000L);
            }
        } catch (Exception e) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean end() {
        try {
            synchronized (this.mSync) {
                this.mEnded = true;
                this.mSync.wait(5000L);
            }
        } catch (Exception e) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnEndListener(OnEndListener onEndListener) {
        //Ignore. secure console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        //Ignore. secure console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncResultListener getAsyncResultListener() {
        return this.mAsyncResultListener;
    }
}
