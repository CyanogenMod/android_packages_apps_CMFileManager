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

import com.cyanogenmod.filemanager.commands.MoveExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.IOException;

import de.schlichtherle.truezip.file.TFile;



/**
 * A class for move a file or directory.
 */
public class MoveCommand extends Program implements MoveExecutable {

    private static final String TAG = "MoveCommand"; //$NON-NLS-1$

    private final String mSrc;
    private final String mDst;

    /**
     * Constructor of <code>MoveCommand</code>.
     *
     * @param console The current console
     * @param src The name of the file or directory to be moved
     * @param dst The name of the file or directory in which move the source file or directory
     */
    public MoveCommand(SecureConsole console, String src, String dst) {
        super(console);
        this.mSrc = src;
        this.mDst = dst;
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
                    String.format("Creating from %s to %s", this.mSrc, this.mDst)); //$NON-NLS-1$
        }

        TFile s = getConsole().buildRealFile(this.mSrc);
        TFile d = getConsole().buildRealFile(this.mDst);
        if (!s.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mSrc);
        }

        //Move or copy recursively
        if (d.exists()) {
            try {
                TFile.cp_r(s, d, SecureConsole.DETECTOR, SecureConsole.DETECTOR);
            } catch (IOException ex) {
                throw new ExecutionException("Failed to move file or directory", ex);
            }
            if (!FileHelper.deleteFolder(s)) {
                if (isTrace()) {
                    Log.v(TAG, "Result: OK. WARNING. Source not deleted."); //$NON-NLS-1$
                }
            }
        } else {
            // Use rename. We are not cross filesystem with this console, so this operation
            // should be safe
            try {
                TFile.mv(s, d, SecureConsole.DETECTOR);
            } catch (IOException ex) {
                throw new ExecutionException("Failed to rename file or directory", ex);
            }
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
