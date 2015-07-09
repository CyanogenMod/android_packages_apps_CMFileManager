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

import android.text.TextUtils;
import android.util.Log;

import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogen.ambient.storage.StorageApi.StatusResult;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.commands.MoveExecutable;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A class for move a file or directory.
 */
public class MoveCommand extends Program implements MoveExecutable {

    private static final String TAG = MoveCommand.class.getSimpleName();

    private final StorageApiConsole mConsole;
    private final String mSrc;
    private final String mDst;
    private String mName;
    private Boolean mResult = false;


    /**
     * Constructor of <code>MoveCommand</code>.
     *
     * @param console The console that will be used for this action
     * @param src The name of the file or directory to be moved
     * @param dst The name of the file or directory in which move the source file or directory
     * @param name The destination file name
     */
    public MoveCommand(StorageApiConsole console, String src, String dst, String name) {
        super();
        this.mConsole = console;
        this.mSrc = src;
        this.mDst = dst;
        mName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResult() {
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
                    String.format("Moving %s to %s", this.mSrc, this.mDst)); //$NON-NLS-1$
        }
        Console cSrc = StorageApiConsole.getStorageApiConsoleForPath(mSrc);
        Console cDst = StorageApiConsole.getStorageApiConsoleForPath(mDst);

        try {
            if ((cSrc != null && cDst != null) &&
                    (cSrc instanceof StorageApiConsole && cDst instanceof StorageApiConsole) &&
                    (mConsole.equals(cSrc) && cSrc.equals(cDst))) {
                moveWithinSingleProvider();
            } else if (cSrc != null && cSrc instanceof StorageApiConsole && mConsole.equals(cSrc)) {
                moveFromProviderToLocal();
            } else if (cDst != null && cDst instanceof StorageApiConsole && mConsole.equals(cDst)) {
                moveFromLocalToProvider();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to move file. ", e); //$NON-NLS-1$
        }


        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

    private void moveWithinSingleProvider() {
        final String srcPath = StorageApiConsole.getProviderPathFromFullPath(mSrc);
        final String dstPath = StorageApiConsole.getProviderPathFromFullPath(mDst);

        PendingResult<DocumentResult> pendingResult =
                mConsole.getStorageApi().move(mConsole.getStorageProviderInfo(), srcPath, dstPath);

        DocumentResult result = pendingResult.await();

        if (result == null || !result.getStatus().isSuccess()) {
            Log.e(TAG, String.format("Failed to move file %s to %s",
                    srcPath, dstPath)); //$NON-NLS-1$
        }

        mResult = result.getStatus().isSuccess();
    }

    private void moveFromProviderToLocal() throws NoSuchFileOrDirectory, FileNotFoundException,
            ExecutionException, CancelledOperationException, InsufficientPermissionsException {
        StorageApi storageApi = mConsole.getStorageApi();
        StorageProviderInfo storageProviderInfo = mConsole.getStorageProviderInfo();
        if (storageApi == null) {
            return;
        } else if (storageProviderInfo == null || TextUtils.isEmpty(mSrc)) {
            return;
        }
        PendingResult<DocumentResult> pendingResult =
                storageApi.getMetadata(storageProviderInfo,
                        StorageApiConsole.getProviderPathFromFullPath(mSrc), true);
        DocumentResult documentResult = pendingResult.await();
        if (documentResult == null || !documentResult.getStatus().isSuccess()) {
            Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
            throw new NoSuchFileOrDirectory(mSrc);
        }
        Document document = documentResult.getDocument();

        //Move or copy recursively
        File d = new File(mDst);
        if (!StorageProviderUtils.copyFromProviderRecursive(mConsole, document, d, this)) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. InsufficientPermissions"); //$NON-NLS-1$
            }
            throw new InsufficientPermissionsException();
        }

        PendingResult<StatusResult> pendingStatusResult =
                storageApi.delete(storageProviderInfo,
                        StorageApiConsole.getProviderPathFromFullPath(mSrc));
        StatusResult statusResult = pendingStatusResult.await();
        if (statusResult == null || !statusResult.getStatus().isSuccess()) {
            if (isTrace()) {
                Log.v(TAG, "Result: OK. WARNING. Source not deleted."); //$NON-NLS-1$
            }
        }
        mResult = statusResult.getStatus().isSuccess();
    }

    private void moveFromLocalToProvider() throws NoSuchFileOrDirectory, FileNotFoundException,
            ExecutionException, CancelledOperationException, InsufficientPermissionsException {
        StorageApi storageApi = mConsole.getStorageApi();
        StorageProviderInfo storageProviderInfo = mConsole.getStorageProviderInfo();
        if (storageApi == null) {
            return;
        } else if (storageProviderInfo == null || TextUtils.isEmpty(mDst)) {
            return;
        }
        PendingResult<DocumentResult> pendingResult =
                storageApi.getMetadata(storageProviderInfo,
                        StorageApiConsole.getProviderPathFromFullPath(mDst), true);
        DocumentResult documentResult = pendingResult.await();
        if (documentResult == null || !documentResult.getStatus().isSuccess()) {
            Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
            throw new NoSuchFileOrDirectory(mDst);
        }
        Document document = documentResult.getDocument();

        //Move or copy recursively
        File s = new File(mSrc);
        if (!s.exists()) {
            throw new NoSuchFileOrDirectory(mSrc);
        }
        if (!StorageProviderUtils.copyToProviderRecursive(mConsole, s, document, mName, this)) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. InsufficientPermissions"); //$NON-NLS-1$
            }
            throw new InsufficientPermissionsException();
        }

        if (!FileHelper.deleteFolder(s)) {
            if (isTrace()) {
                Log.v(TAG, "Result: OK. WARNING. Source not deleted."); //$NON-NLS-1$
            }
        }
        mResult = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getSrcWritableMountPoint() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getDstWritableMountPoint() {
        return null;
    }

}
