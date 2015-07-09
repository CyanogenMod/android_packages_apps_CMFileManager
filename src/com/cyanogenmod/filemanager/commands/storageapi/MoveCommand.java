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

package com.cyanogenmod.filemanager.commands.storageapi;

import android.util.Log;

import com.android.internal.http.multipart.StringPart;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.MoveExecutable;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MountPointHelper;

import java.io.File;


/**
 * A class for move a file or directory.
 */
public class MoveCommand extends Program implements MoveExecutable {

    private static final String TAG = MoveCommand.class.getSimpleName();

    private final StorageApiConsole mConsole;
    private final String mSrc;
    private final String mDst;
    private final Object mSync = new Object();
    private boolean mFinished = false;
    private Boolean mResult = false;


    /**
     * Constructor of <code>MoveCommand</code>.
     *
     * @param src The name of the file or directory to be moved
     * @param dst The name of the file or directory in which move the source file or directory
     */
    public MoveCommand(StorageApiConsole console, String src, String dst) {
        super();
        this.mConsole = console;
        this.mSrc = src;
        this.mDst = dst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResult() {
        if (!mFinished) {
            synchronized (mSync) {
                try {
                    mSync.wait(R.integer.storageapi_timeout);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Result timeout."); //$NON-NLS-1$
                }
            }
        }
        return mResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException,
                   CancelledOperationException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Creating from %s to %s", this.mSrc, this.mDst)); //$NON-NLS-1$
        }

        moveFromProviderToProvider();
        /*File s = new File(this.mSrc);
        File d = new File(this.mDst);
        if (!s.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mSrc);
        }

        //Move or copy recursively
        if (d.exists()) {
            if (!FileHelper.copyRecursive(s, d, getBufferSize(), this)) {
                if (isTrace()) {
                    Log.v(TAG, "Result: FAIL. InsufficientPermissionsException"); //$NON-NLS-1$
                }
                throw new InsufficientPermissionsException();
            }
            if (!FileHelper.deleteFolder(s)) {
                if (isTrace()) {
                    Log.v(TAG, "Result: OK. WARNING. Source not deleted."); //$NON-NLS-1$
                }
            }
        } else {
            // Move between filesystem is not allow. If rename fails then use copy operation
            if (!s.renameTo(d)) {
                if (!FileHelper.copyRecursive(s, d, getBufferSize(), this)) {
                    if (isTrace()) {
                        Log.v(TAG, "Result: FAIL. InsufficientPermissionsException"); //$NON-NLS-1$
                    }
                    throw new InsufficientPermissionsException();
                }
                if (!FileHelper.deleteFolder(s)) {
                    if (isTrace()) {
                        Log.v(TAG, "Result: OK. WARNING. Source not deleted."); //$NON-NLS-1$
                    }
                }
            }
        }*/

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

    private void moveFromProviderToProvider() {
        final String srcPath = StorageApiConsole.getProviderPathFromFullPath(mSrc);
        final String dstPath = StorageApiConsole.getProviderPathFromFullPath(mDst);

        mConsole.getStorageApi().move(mConsole.getStorageProviderInfo(), srcPath, dstPath,
                new ResultCallback<DocumentResult>() {
                    @Override
                    public void onResult(DocumentResult documentResult) {
                        if (documentResult == null) {
                            Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
                            return;
                        }
                        if (documentResult.getStatus().isSuccess()) {
                            mResult = true;
                        }
                        synchronized (mSync) {
                            mSync.notify();
                        }
                        mFinished = true;

                        if (isTrace()) {
                            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
                        }
                    }
                });
    }

    private void moveFromLocalToProvider() {
        // TODO: Implement this
        File srcFile = new File(mSrc);
        final String dstPath = StorageApiConsole.getProviderPathFromFullPath(mDst);
    }

    private void moveFromProviderToLocal() {
        // TODO: Implement this
        final String srcPath = StorageApiConsole.getProviderPathFromFullPath(mSrc);
        File dstFile = new File(mDst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getSrcWritableMountPoint() {
        //return MountPointHelper.getMountPointFromDirectory(this.mSrc);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getDstWritableMountPoint() {
        //return MountPointHelper.getMountPointFromDirectory(this.mDst);
        return null;
    }

}
