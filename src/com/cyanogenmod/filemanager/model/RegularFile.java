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

import java.util.Date;

/**
 * A class that represents a regular file.
 */
public class RegularFile extends FileSystemObject {

    private static final long serialVersionUID = 7113562456595400525L;

    /**
     * The unix identifier of the object.
     * @hide
     */
    public static final char UNIX_ID = '-';

    /**
     * Constructor of <code>RegularFile</code>.
     *
     * @param name The name of the object
     * @param parent The parent folder of the object
     * @param providerPrefix The prefix to file path that represents a specific provider
     * @param id The id of the object
     * @param user The user proprietary of the object
     * @param group The group proprietary of the object
     * @param permissions The permissions of the object
     * @param size The size in bytes of the object
     * @param lastAccessedTime The last time that the object was accessed
     * @param lastModifiedTime The last time that the object was modified
     * @param lastChangedTime The last time that the object was changed
     */
    public RegularFile(String name, String parent, String providerPrefix, String id, User user,
            Group group, Permissions permissions, long size, Date lastAccessedTime,
            Date lastModifiedTime, Date lastChangedTime) {
        super(name, parent, providerPrefix, id, user, group, permissions, size,
                lastAccessedTime, lastModifiedTime, lastChangedTime);
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
        return "RegularFile [type=" + super.toString() + "]";  //$NON-NLS-1$//$NON-NLS-2$
    }

}
