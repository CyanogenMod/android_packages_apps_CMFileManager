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
 * A class that represents a symbolic link.
 *
 * {@link "http://en.wikipedia.org/wiki/Symbolic_link"}
 */
public class Symlink extends FileSystemObject {

    private static final long serialVersionUID = -6411787401264288389L;

    /**
     * The unix identifier of the object.
     * @hide
     */
    public static final char UNIX_ID = 'l';

    private String mLink;
    private FileSystemObject mLinkRef;

    /**
     * Constructor of <code>Symlink</code>.
     *
     * @param name The name of the object
     * @param link The real file that this symlink is point to
     * @param parent The parent folder of the object
     * @param user The user proprietary of the object
     * @param group The group proprietary of the object
     * @param permissions The permissions of the object
     * @param lastAccessedTime The last time that the object was accessed
     * @param lastModifiedTime The last time that the object was modified
     * @param lastChangedTime The last time that the object was changed
     */
    public Symlink(String name, String link, String parent, User user,
            Group group, Permissions permissions,
            Date lastAccessedTime, Date lastModifiedTime, Date lastChangedTime) {
        super(name, parent, null, null, user, group, permissions, 0L,
                lastAccessedTime, lastModifiedTime, lastChangedTime);
        this.mLink = link;
    }

    /**
     * Method that returns the real file that this symlink is point to.
     *
     * @return String The real file that this symlink is point to.
     */
    public String getLink() {
        return this.mLink;
    }

    /**
     * Method that sets the real file that this symlink is point to.
     *
     * @param link the real file that this symlink is point to
     */
    public void setLink(String link) {
        this.mLink = link;
    }

    /**
     * Method that returns the {@link FileSystemObject} reference of the symlink.
     *
     * @return FileSystemObject The {@link FileSystemObject} reference of the symlink
     */
    public FileSystemObject getLinkRef() {
        return this.mLinkRef;
    }

    /**
     * Method that sets the {@link FileSystemObject} reference of the symlink.
     *
     * @param linkRef The {@link FileSystemObject} reference of the symlink
     */
    public void setLinkRef(FileSystemObject linkRef) {
        this.mLinkRef = linkRef;
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
        return "Symlink [type=" + super.toString() + ", link=" //$NON-NLS-1$//$NON-NLS-2$
                + this.mLink + "]";  //$NON-NLS-1$
    }

}
