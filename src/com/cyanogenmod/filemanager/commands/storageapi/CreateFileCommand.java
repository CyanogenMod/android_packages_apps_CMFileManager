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

package com.cyanogenmod.filemanager.commands.storageapi;

import android.util.Log;

import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogenmod.filemanager.commands.CreateFileExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.util.MountPointHelper;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.io.File;
import java.io.IOException;


/**
 * A class for create a file.
 */
public class CreateFileCommand extends Program implements CreateFileExecutable {

    private static final String TAG = "CreateFileCommand"; //$NON-NLS-1$


    private final StorageApiConsole mConsole;
    private final String mParent;
    private final String mName;
    private String mResult;

    /**
     * Constructor of <code>CreateFileCommand</code>.
     *
     * @param dir The id of the parent directory
     * @param name The name of the new file
     */
    public CreateFileCommand(StorageApiConsole console, String dir, String name) {
        super();
        this.mConsole = console;
        this.mParent = dir;
        this.mName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResult() {
        return mResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Creating file: %s", this.mParent)); //$NON-NLS-1$
        }

        String parentId = StorageApiConsole.getProviderPathFromFullPath(mParent);
        PendingResult<StorageApi.Document.DocumentResult> pendingResult = mConsole.getStorageApi()
                .createDocument(mConsole.getStorageProviderInfo(), parentId, mName, null);
        StorageApi.Document.DocumentResult statusResult = pendingResult.await();

        if (statusResult.getStatus().isSuccess()) {
            mResult = StorageApiConsole.getFullPathForConsoleDocument(mConsole,
                    statusResult.getDocument().getId());
        }

        if (isTrace()) {
            Log.v(TAG, "Result: " + mResult); //$NON-NLS-1$
        }
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
