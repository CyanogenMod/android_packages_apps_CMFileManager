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

import com.cyanogenmod.filemanager.R;

import java.util.Date;

/**
 * A class that represents a directory.
 */
public class Directory extends FileSystemObject {

    private static final long serialVersionUID = -3975569940766905884L;

    //Resource identifier for default icon
    private static final int RESOURCE_FOLDER_DEFAULT = R.drawable.ic_fso_folder;

    /**
     * The unix identifier of the object.
     * @hide
     */
    public static final char UNIX_ID = 'd';

    /**
     * Constructor of <code>Directory</code>.
     *
     * @param name The name of the object
     * @param parent The parent folder of the object
     * @param providerPrefix The prefix to file path that represents a specific provider
     * @param user The user proprietary of the object
     * @param group The group proprietary of the object
     * @param permissions The permissions of the object
     * @param lastAccessedTime The last time that the object was accessed
     * @param lastModifiedTime The last time that the object was modified
     * @param lastChangedTime The last time that the object was changed
     */
    public Directory(String name, String parent, String providerPrefix, User user, Group group,
                     Permissions permissions, Date lastAccessedTime, Date lastModifiedTime,
                     Date lastChangedTime) {
        super(name, parent, providerPrefix, user, group, permissions, 0L,
                lastAccessedTime, lastModifiedTime, lastChangedTime);
        setResourceIconId(RESOURCE_FOLDER_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getUnixIdentifier() {
        return UNIX_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Directory [type=" + super.toString() + "]";  //$NON-NLS-1$//$NON-NLS-2$
    }

}
