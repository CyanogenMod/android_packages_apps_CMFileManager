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

import android.webkit.MimeTypeMap;
import com.android.internal.http.multipart.StringPart;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogen.ambient.storage.StorageApi.DocumentInfo.DocumentInfoResult;
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
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * A class for move a file or directory.
 */
public class MoveCommand extends Program implements MoveExecutable {

    private static final String TAG = MoveCommand.class.getSimpleName();

    private final StorageApiConsole mConsole;
    private final String mSrc;
    private final String mDst;
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

        if ((cSrc != null && cDst != null) &&
                (cSrc instanceof StorageApiConsole && cDst instanceof StorageApiConsole) &&
                (mConsole.equals(cSrc) && cSrc.equals(cDst))) {
            moveWithinSingleProvider();
        } else if (cSrc != null && cSrc instanceof StorageApiConsole && mConsole.equals(cSrc)) {
            try {
                moveFromProviderToLocal();
            } catch (Exception e) {
                Log.e(TAG, "Failed to move file. ", e); //$NON-NLS-1$
            }
        } else if (cDst != null && cDst instanceof StorageApiConsole && mConsole.equals(cDst)) {
            moveFromLocalToProvider();
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

    private void moveFromProviderToLocal() throws NoSuchFileOrDirectory, FileNotFoundException {
        final String srcPath = StorageApiConsole.getProviderPathFromFullPath(mSrc);
        File dstFile = new File(mDst);

        OutputStream outputStream = null;
        try {
            if (!dstFile.exists() && !dstFile.createNewFile()) {
                throw new NoSuchFileOrDirectory(this.mSrc);
            }
            outputStream = new FileOutputStream(dstFile);
        } catch (Exception e) {
            Log.v(TAG, "Result: Failed creating temp file. " + e); //$NON-NLS-1$
        }

        PendingResult<DocumentInfoResult> pendingResult =
                mConsole.getStorageApi().getFile(mConsole.getStorageProviderInfo(),
                        srcPath, outputStream, null);

        DocumentInfoResult result = pendingResult.await();

        if (result == null || !result.getStatus().isSuccess()) {
            Log.e(TAG, String.format("Failed to move file %s to %s",
                    srcPath, mDst)); //$NON-NLS-1$
        }

        mResult = result.getStatus().isSuccess();
    }

    private void moveFromLocalToProvider() {
        File srcFile = new File(mSrc);
        final String dstPath = StorageApiConsole.getProviderPathFromFullPath(mDst);

        InputStream inputStream = null;
        try {
            if (!srcFile.exists()) {
                throw new NoSuchFileOrDirectory(this.mSrc);
            }
            inputStream = new FileInputStream(srcFile);
        } catch (Exception e) {
            Log.v(TAG, "Result: Failed creating temp file. " + e); //$NON-NLS-1$
        }

        // Get mime type
        String extension = MimeTypeMap.getFileExtensionFromUrl(srcFile.getName());
        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        PendingResult<DocumentResult> pendingResult =
                mConsole.getStorageApi().putFile(mConsole.getStorageProviderInfo(),
                        dstPath, srcFile.getName(), inputStream, mimetype, false, null);

        DocumentResult result = pendingResult.await();

        if (result == null || !result.getStatus().isSuccess()) {
            Log.e(TAG, String.format("Failed to move file %s to %s",
                    mSrc, dstPath)); //$NON-NLS-1$
        }

        mResult = result.getStatus().isSuccess();
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
