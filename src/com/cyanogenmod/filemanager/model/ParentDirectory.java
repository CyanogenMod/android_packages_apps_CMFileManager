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

package com.cyanogenmod.filemanager.model;

import com.cyanogenmod.filemanager.util.FileHelper;


/**
 * A class that represents a link to the parent directory.
 */
public class ParentDirectory extends Directory {

    private static final long serialVersionUID = -3818276335217197479L;

    /**
     * Constructor of <code>ParentDirectory</code>.
     *
     * @param parent The parent folder of the object
     */
    public ParentDirectory(String parent) {
        super(FileHelper.PARENT_DIRECTORY, parent, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor of <code>ParentDirectory</code>.
     *
     * @param parent The parent folder of the object
     * @param providerPrefix The prefix to file path that represents a specific provider
     */
    public ParentDirectory(String parent, String providerPrefix) {
        super(FileHelper.PARENT_DIRECTORY, parent, providerPrefix, parent, null, null, null, null,
                null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden() {
        return false;
    }

}
