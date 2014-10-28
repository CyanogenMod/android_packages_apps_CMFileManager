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

import com.cyanogenmod.filemanager.commands.ParentDirExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;

import de.schlichtherle.truezip.file.TFile;


/**
 * A class for returns the parent directory.
 */
public class ParentDirCommand extends Program implements ParentDirExecutable {

    private static final String TAG = "ParentDirCommand"; //$NON-NLS-1$

    private final String mSrc;
    private String mParentDir;

    /**
     * Constructor of <code>ParentDirCommand</code>.
     *
     * @param console The current console
     * @param src The source file
     */
    public ParentDirCommand(SecureConsole console, String src) {
        super(console);
        this.mSrc = src;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResult() {
        return this.mParentDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Getting parent directory of %s", //$NON-NLS-1$
                            this.mSrc));
        }

        // Build the source file information
        TFile f = getConsole().buildRealFile(mSrc).getParentFile();
        boolean isSecureStorage = SecureConsole.isSecureStorageDir(f);
        if (isSecureStorage) {
            this.mParentDir = getConsole().getVirtualMountPoint().getAbsolutePath();
        } else {
            this.mParentDir = getConsole().buildVirtualPath(f);
        }

        if (isTrace()) {
            Log.v(TAG,
                    String.format("Parent directory: %S", //$NON-NLS-1$
                            this.mParentDir));
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
        }
    }

}
