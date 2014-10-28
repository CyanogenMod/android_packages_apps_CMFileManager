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

import com.cyanogenmod.filemanager.commands.ListExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.util.FileHelper;

import de.schlichtherle.truezip.file.TFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * A class for list information about files and directories.
 */
public class ListCommand extends Program implements ListExecutable {

    private static final String TAG = "ListCommand"; //$NON-NLS-1$

    private final String mSrc;
    private final LIST_MODE mMode;
    private final List<FileSystemObject> mFiles;

    /**
     * Constructor of <code>ListCommand</code>. List mode.
     *
     * @param src The file system object to be listed
     * @param mode The mode of listing
     */
    public ListCommand(SecureConsole console, String src, LIST_MODE mode) {
        super(console);
        this.mSrc = src;
        this.mMode = mode;
        this.mFiles = new ArrayList<FileSystemObject>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresOpen() {
        if (this.mMode.compareTo(LIST_MODE.FILEINFO) == 0) {
            return !getConsole().getVirtualMountPoint().equals(new File(mSrc));
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> getResult() {
        return this.mFiles;
    }

    /**
     * Method that returns a single result of the program invocation.
     * Only must be called within a <code>FILEINFO</code> mode listing.
     *
     * @return FileSystemObject The file system object reference
     */
    public FileSystemObject getSingleResult() {
        return this.mFiles.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Listing %s. Mode: %s", //$NON-NLS-1$
                            this.mSrc, this.mMode));
        }

        TFile f = getConsole().buildRealFile(mSrc);
        boolean isSecureStorage = SecureConsole.isSecureStorageDir(f);
        File javaFile = f.getFile();
        if (!isSecureStorage && !f.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mSrc);
        }
        if (this.mMode.compareTo(LIST_MODE.DIRECTORY) == 0) {
            // List files in directory
            TFile[] files = f.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    FileSystemObject fso = FileHelper.createFileSystemObject(files[i]);
                    if (fso != null) {
                        // Convert to virtual
                        fso.setParent(getConsole().buildVirtualPath(files[i].getParentFile()));
                        fso.setSecure(true);

                        if (isTrace()) {
                            Log.v(TAG, String.valueOf(fso));
                        }
                        this.mFiles.add(fso);
                    }
                }
            }

            //Now if not is the root directory, add the parent directory
            if (this.mSrc.compareTo(FileHelper.ROOT_DIRECTORY) != 0 &&
                    this.mMode.compareTo(LIST_MODE.DIRECTORY) == 0) {
                this.mFiles.add(0, new ParentDirectory(new File(this.mSrc).getParent()));
            }
        } else {
            // Build the source file information
            FileSystemObject fso = FileHelper.createFileSystemObject(
                    isSecureStorage ? javaFile : f);
            if (fso != null) {
                // Convert to virtual
                if (isSecureStorage) {
                    File virtualMountPoint = getConsole().getVirtualMountPoint();
                    fso = new Directory(
                            virtualMountPoint.getName(),
                            getConsole().getVirtualMountPoint().getParent(),
                            fso.getUser(), fso.getGroup(), fso.getPermissions(),
                            fso.getLastAccessedTime(),
                            fso.getLastModifiedTime(),
                            fso.getLastChangedTime());
                    fso.setSecure(true);
                } else {
                    fso.setParent(getConsole().buildVirtualPath(f.getParentFile()));
                }
                fso.setSecure(true);
                if (isTrace()) {
                    Log.v(TAG, String.valueOf(fso));
                }
                this.mFiles.add(fso);
            }
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

}
