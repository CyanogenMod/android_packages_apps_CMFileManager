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
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

/**
 * A class that represents an abstract file system object.
 *
 * @see RegularFile
 * @see Directory
 * @see Symlink
 * @see SystemFile
 */
public abstract class FileSystemObject implements Serializable, Comparable<FileSystemObject> {

    private static final long serialVersionUID = -571144166609728391L;

    //Resource identifier for default icon
    private static final int RESOURCE_ICON_DEFAULT = R.drawable.ic_fso_default;

    private int mResourceIconId;
    private String mName;
    private String mParent;
    private User mUser;
    private Group mGroup;
    private Permissions mPermissions;
    private long mSize;
    private Date mLastAccessedTime;
    private Date mLastModifiedTime;
    private Date mLastChangedTime;
    private boolean mIsSecure;
    private boolean mIsRemote;

    /**
     * Constructor of <code>FileSystemObject</code>.
     *
     * @param name The name of the object
     * @param parent The parent folder of the object
     * @param user The user proprietary of the object
     * @param group The group proprietary of the object
     * @param permissions The permissions of the object
     * @param size The size in bytes of the object
     * @param lastAccessedTime The last time that the object was accessed
     * @param lastModifiedTime The last time that the object was modified
     * @param lastChangedTime The last time that the object was changed
     */
    public FileSystemObject(String name, String parent, User user, Group group,
            Permissions permissions, long size,
            Date lastAccessedTime, Date lastModifiedTime, Date lastChangedTime) {
        super();
        this.mName = name;
        this.mParent = parent;
        this.mUser = user;
        this.mGroup = group;
        this.mPermissions = permissions;
        this.mSize = size;
        this.mLastAccessedTime = lastAccessedTime;
        this.mLastModifiedTime = lastModifiedTime;
        this.mLastChangedTime = lastChangedTime;
        this.mResourceIconId = RESOURCE_ICON_DEFAULT;
        this.mIsSecure = false;
        this.mIsRemote = false;
    }

    /**
     * Method that returns the character that identifies the object in unix.
     *
     * @return char The character that identifies the object in unix
     */
    public abstract char getUnixIdentifier();

    /**
     * Method that returns the name of the object.
     *
     * @return String The name of the object
     */
    public String getName() {
        return this.mName;
    }

    /**
     * Method that sets the name of the object.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Method that returns the parent folder of the object.
     *
     * @return String The parent folder of the object
     */
    public String getParent() {
        return this.mParent;
    }

    /**
     * Method that sets the parent folder of the object.
     *
     * @param parent The parent folder of the object
     */
    public void setParent(String parent) {
        this.mParent = parent;
    }

    /**
     * Method that returns the user proprietary of the object.
     *
     * @return User The user proprietary of the object
     */
    public User getUser() {
        return this.mUser;
    }

    /**
     * Method that sets the user proprietary of the object.
     *
     * @param user The user proprietary of the object
     */
    public void setUser(User user) {
        this.mUser = user;
    }

    /**
     * Method that returns the group proprietary of the object.
     *
     * @return Group The group proprietary of the object
     */
    public Group getGroup() {
        return this.mGroup;
    }

    /**
     * Method that sets the group proprietary of the object.
     *
     * @param group The group proprietary of the object
     */
    public void setGroup(Group group) {
        this.mGroup = group;
    }

    /**
     * Method that returns the permissions of the object.
     *
     * @return Permissions The permissions of the object
     */
    public Permissions getPermissions() {
        return this.mPermissions;
    }

    /**
     * Method that sets the permissions of the object.
     *
     * @param permissions The permissions of the object
     */
    public void setPermissions(Permissions permissions) {
        this.mPermissions = permissions;
    }

    /**
     * Method that returns the size in bytes of the object.
     *
     * @return long The size in bytes of the object
     */
    public long getSize() {
        return this.mSize;
    }

    /**
     * Method that sets the size in bytes of the object.
     *
     * @param size The size in bytes of the object
     */
    public void setSize(long size) {
        this.mSize = size;
    }

    /**
     * Method that returns the last time that the object was accessed.
     *
     * @return Date The last time that the object was accessed
     */
    public Date getLastAccessedTime() {
        return this.mLastAccessedTime;
    }

    /**
     * Method that sets the last time that the object was accessed.
     *
     * @param lastAccessedTime The last time that the object was accessed
     */
    public void setLastAccessedTime(Date lastAccessedTime) {
        this.mLastAccessedTime = lastAccessedTime;
    }

    /**
     * Method that returns the last time that the object was modified.
     *
     * @return Date The last time that the object was modified
     */
    public Date getLastModifiedTime() {
        return this.mLastModifiedTime;
    }

    /**
     * Method that sets the last time that the object was modified.
     *
     * @param lastModifiedTime The last time that the object was modified
     */
    public void setLastModifiedTime(Date lastModifiedTime) {
        this.mLastModifiedTime = lastModifiedTime;
    }

    /**
     * Method that returns the last time that the object was changed.
     *
     * @return Date The last time that the object was changed
     */
    public Date getLastChangedTime() {
        return this.mLastChangedTime;
    }

    /**
     * Method that sets the last time that the object was changed.
     *
     * @param lastChangedTime The last time that the object was changed
     */
    public void setLastChangedTime(Date lastChangedTime) {
        this.mLastChangedTime = lastChangedTime;
    }

    /**
     * Method that returns if the object is hidden object.
     *
     * @return boolean If the object is hidden object
     */
    public boolean isHidden() {
        return this.mName.startsWith("."); //$NON-NLS-1$
    }

    /**
     * Method that returns if the object is secure
     *
     * @return boolean If the object is secure
     */
    public boolean isSecure() {
        return mIsSecure;
    }

    /**
     * Mehtod that sets if the object is secure
     *
     * @param secure if the object is secure
     */
    public void setSecure(boolean secure) {
        mIsSecure = secure;
    }

    /**
     * Method that returns if the object is remote
     *
     * @return boolean If the object is remote
     */
    public boolean isRemote() {
        return mIsRemote;
    }

    /**
     * Mehtod that sets if the object is remote
     *
     * @param remote if the object is remote
     */
    public void setRemote(boolean remote) {
        mIsRemote = remote;
    }

    /**
     * Method that returns the identifier of the drawable icon associated
     * to the object.
     *
     * @return int The identifier of the drawable icon
     * @hide
     */
    public int getResourceIconId() {
        return this.mResourceIconId;
    }

    /**
     * Method that sets the identifier of the drawable icon associated
     * to the object.
     *
     * @param resourceIconId The identifier of the drawable icon
     * @hide
     */
    protected void setResourceIconId(int resourceIconId) {
        this.mResourceIconId = resourceIconId;
    }

    /**
     * Method that returns the full path of the file system object.
     *
     * @return String The full path of the file system object
     */
    public String getFullPath() {
        if (FileHelper.isRootDirectory(this)) {
            return FileHelper.ROOT_DIRECTORY;
        } else if (FileHelper.isParentRootDirectory(this)) {
            if (this.mParent == null) {
                return FileHelper.ROOT_DIRECTORY + this.mName;
            }
            return this.mParent + this.mName;
        }
        return this.mParent + File.separator + this.mName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FileSystemObject another) {
        String o1 = this.getFullPath();
        String o2 = another.getFullPath();
        return o1.compareTo(o2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.mName == null) ? 0 : this.mName.hashCode());
        result = prime * result + ((this.mParent == null) ? 0 : this.mParent.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        FileSystemObject other = (FileSystemObject) obj;
        if (this.mName == null) {
            if (other.mName != null)
                return false;
        } else if (!this.mName.equals(other.mName))
            return false;
        if (this.mParent == null) {
            if (other.mParent != null)
                return false;
        } else if (!this.mParent.equals(other.mParent))
            return false;
        return true;
    }

    /**
     * Method that returns the unix string representation of the type and permissions of the
     * file system object.
     *
     * @return String The string representation
     */
    public String toRawPermissionString() {
        return Character.toString(getUnixIdentifier())
                + getPermissions().toRawString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "FileSystemObject [mResourceIconId=" + this.mResourceIconId //$NON-NLS-1$
                + ", mName=" + this.mName + ", mParent=" + this.mParent //$NON-NLS-1$ //$NON-NLS-2$
                + ", mUser=" + this.mUser + ", mGroup=" + this.mGroup //$NON-NLS-1$ //$NON-NLS-2$
                + ", mPermissions=" + this.mPermissions //$NON-NLS-1$
                + ", mSize=" + this.mSize //$NON-NLS-1$
                + ", mLastAccessedTime=" + this.mLastAccessedTime //$NON-NLS-1$
                + ", mLastModifiedTime=" + this.mLastModifiedTime //$NON-NLS-1$
                + ", mLastChangedTime=" + this.mLastChangedTime //$NON-NLS-1$
                + ", mIsSecure=" + mIsSecure //$NON-NLS-1$
                + ", mIsRemote=" + mIsRemote //$NON-NLS-1$
                + "]"; //$NON-NLS-1$
    }

}
