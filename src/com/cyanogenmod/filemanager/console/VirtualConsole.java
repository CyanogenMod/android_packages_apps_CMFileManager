/*
 * Copyright (C) 20124 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.console;

import android.content.Context;
import android.util.Log;

import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.model.Identity;
import com.cyanogenmod.filemanager.util.AIDHelper;

/**
 * An abstract base class for all the virtual {@link Console}.
 */
public abstract class VirtualConsole extends Console {

    public static final String TAG = "VirtualConsole";

    private boolean mActive;
    private final Context mCtx;
    private final Identity mIdentity;

    /**
     * Constructor of <code>VirtualConsole</code>
     *
     * @param ctx The current context
     */
    public VirtualConsole(Context ctx) {
        super();
        mCtx = ctx;
        mIdentity = AIDHelper.createVirtualIdentity();
    }

    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    @Override
    public void alloc() throws ConsoleAllocException {
        try {
            if (isTrace()) {
                Log.v(TAG, "Allocating " + getName() + " console");
            }
            mActive = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to allocate " + getName() + " console", e);
            throw new ConsoleAllocException("failed to build console", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dealloc() {
        if (isTrace()) {
            Log.v(TAG, "Deallocating Java console");
        }
        mActive = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void realloc() throws ConsoleAllocException {
        dealloc();
        alloc();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity getIdentity() {
        return mIdentity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrivileged() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return mActive;
    }

    /**
     * Method that returns the current context
     *
     * @return Context The current context
     */
    public Context getCtx() {
        return mCtx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCancel() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onSendSignal(SIGNAL signal) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onEnd() {
        return false;
    }
}
