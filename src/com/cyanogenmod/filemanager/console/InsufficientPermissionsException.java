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

package com.cyanogenmod.filemanager.console;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.commands.SyncResultExecutable;

/**
 * An exception thrown when an operation required elevated permissions.
 */
public class InsufficientPermissionsException extends RelaunchableException {

    private static final long serialVersionUID = -5350536343872073589L;

    /**
     * Constructor of <code>InsufficientPermissionsException</code>.
     */
    public InsufficientPermissionsException() {
        super(null);
    }

    /**
     * Constructor of <code>InsufficientPermissionsException</code>.
     *
     * @param executable The executable that should be re-executed
     */
    public InsufficientPermissionsException(SyncResultExecutable executable) {
        super(executable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQuestionResourceId() {
        return R.string.advise_insufficient_permissions;
    }

}
