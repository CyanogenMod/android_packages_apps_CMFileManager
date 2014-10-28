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

import com.cyanogenmod.filemanager.commands.DeleteFileExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.MountPoint;

import de.schlichtherle.truezip.file.TFile;

import java.io.IOException;


/**
 * A class for delete a file.
 */
public class DeleteFileCommand extends Program implements DeleteFileExecutable {

    private static final String TAG = "DeleteFileCommand"; //$NON-NLS-1$

    private final String mPath;

    /**
     * Constructor of <code>DeleteFileCommand</code>.
     *
     * @param console The current console
     * @param path The name of the new file
     */
    public DeleteFileCommand(SecureConsole console, String path) {
        super(console);
        this.mPath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresSync() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResult() {
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Deleting file: %s", this.mPath)); //$NON-NLS-1$
        }

        TFile f = getConsole().buildRealFile(this.mPath);
        if (!f.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mPath);
        }

        // Check that if the path exist, it need to be a file. Otherwise something is
        // wrong
        if (f.exists() && !f.isFile()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. ExecutionException"); //$NON-NLS-1$
            }
            throw new ExecutionException("the path exists but is not a file"); //$NON-NLS-1$
        }

        // Delete the file
        try {
            TFile.rm(f);
        } catch (IOException ex) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. IOException"); //$NON-NLS-1$
            }
            throw new ExecutionException("Failed to delete file", ex);
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
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
