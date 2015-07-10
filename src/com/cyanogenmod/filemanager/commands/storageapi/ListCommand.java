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

import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.commands.ListExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * A class for list information about files and directories.
 */
public class ListCommand extends Program implements ListExecutable {
    private static final String TAG = ListCommand.class.getSimpleName();
    private static boolean DEBUG = false;

    private final StorageApiConsole mConsole;
    private final String mSrc;
    private final LIST_MODE mMode;
    private final List<FileSystemObject> mFiles;
    private final Object mSync = new Object();
    private boolean mFinished = false;

    /**
     * Constructor of <code>ListCommand</code>. List mode.
     *
     * @param src The file system object to be listed
     * @param mode The mode of listing
     */
    public ListCommand(StorageApiConsole console, String src, LIST_MODE mode) {
        super();
        this.mConsole = console;
        this.mSrc = StorageApiConsole.getProviderPathFromFullPath(src);
        this.mMode = mode;
        this.mFiles = new ArrayList<FileSystemObject>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> getResult() {
        if (!mFinished) {
            synchronized (mSync) {
                try {
                    mSync.wait(R.integer.storageapi_timeout);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Result timeout. No valid results returned."); //$NON-NLS-1$
                }
            }
        }
        return this.mFiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Listing %s. Mode: %s", //$NON-NLS-1$
                            this.mSrc, this.mMode));
        }

        StorageApi storageApi = mConsole.getStorageApi();
        StorageProviderInfo storageProviderInfo = mConsole.getStorageProviderInfo();
        if (storageApi == null) {
            return;
        } else if (storageProviderInfo == null || TextUtils.isEmpty(mSrc)) {
            return;
        }

        storageApi.getMetadata(storageProviderInfo, mSrc, true,
                new ResultCallback<DocumentResult>() {
                    @Override
                    public void onResult(DocumentResult documentResult) {
                        if (documentResult == null) {
                            Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
                            return;
                        }
                        try {
                            processDocumentResult(documentResult);
                        } catch (Exception e) {
                            Log.e(TAG, "Result: Error parsing results. e=" + e); //$NON-NLS-1$
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

    private void processDocumentResult(DocumentResult result)
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {
        Document current = result.getDocument();
        if (current == null) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mSrc);
        }
        int hash = StorageApiConsole.getHashCodeFromProvider(mConsole.getStorageProviderInfo());
        String providerPrefix = StorageApiConsole.constructStorageApiPrefixFromHash(hash);
        if (mMode.compareTo(LIST_MODE.DIRECTORY) == 0) {
            List<Document> documents = current.getContents();
            if (documents != null && !documents.isEmpty()) {
                for (Document document : documents) {
                    FileSystemObject fso =
                            FileHelper.createFileSystemObject(document, providerPrefix);
                    if (fso != null) {
                        if (isTrace()) {
                            Log.v(TAG, String.valueOf(fso));
                        }
                        this.mFiles.add(fso);
                    }
                }
            }

            // If current is not root, add parent directory to file list (..)
            if (!TextUtils.equals(current.getId(), current.getParentId())) {
                if (mMode.compareTo(LIST_MODE.DIRECTORY) == 0) {
                    if (DEBUG) Log.d(TAG, "Adding parentId=" + current.getParentId());
                    mFiles.add(0, new ParentDirectory(current.getParentId(), providerPrefix));
                }
            }

        } else {
            // Build the parent information
            FileSystemObject fso = FileHelper.createFileSystemObject(current, providerPrefix);
            if (fso != null) {
                if (isTrace()) {
                    Log.v(TAG, String.valueOf(fso));
                }
                this.mFiles.add(fso);
            }
        }
    }

}
