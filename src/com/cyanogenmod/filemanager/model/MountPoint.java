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

import java.io.Serializable;

/**
 * A class that holds information about a mount point.
 */
public class MountPoint implements Serializable, Comparable<MountPoint> {

    private static final long serialVersionUID = -2598921174356702897L;

    private final String mMountPoint;
    private final String mDevice;
    private final String mType;
    private final String mOptions;
    private final int mDump;
    private final int mPass;
    private boolean mSecure;
    private boolean mRemote;

    /**
     * Constructor of <code>MountPoint</code>.
     *
     * @param mountPoint The mount point of the file system device
     * @param device The file system device
     * @param type The type of file system
     * @param options The mount options
     * @param dump The frequency to determine if the filesystem need to be dumped
     * @param pass The order in which filesystem checks are done at reboot time
     * @param secure If the device is a secure virtual filesystem
     * @param remote If the device is a remote virtual filesystem
     */
    public MountPoint(
            String mountPoint, String device, String type, String options, int dump,
            int pass, boolean secure, boolean remote) {
        super();
        this.mMountPoint = mountPoint;
        this.mDevice = device;
        this.mType = type;
        this.mOptions = options;
        this.mDump = dump;
        this.mPass = pass;
        this.mSecure = secure;
        this.mRemote = remote;
    }

    /**
     * Method that returns the mount point of the file system device.
     *
     * @return String The mount point of the file system device
     */
    public String getMountPoint() {
        return this.mMountPoint;
    }

    /**
     * Method that returns the file system device.
     *
     * @return String The file system device
     */
    public String getDevice() {
        return this.mDevice;
    }

    /**
     * Method that returns the type of file system.
     *
     * @return String The type of file system
     */
    public String getType() {
        return this.mType;
    }

    /**
     * Method that returns the mount options.
     *
     * @return String The mount options
     */
    public String getOptions() {
        return this.mOptions;
    }

    /**
     * Method that returns the frequency to determine if the filesystem need to be dumped.
     *
     * @return long The frequency to determine if the filesystem need to be dumped
     */
    public int getDump() {
        return this.mDump;
    }

    /**
     * Method that returns the frequency to determine if the order in which filesystem
     * checks are done at reboot time.
     *
     * @return long The order in which filesystem checks are done at reboot time
     */
    public int getPass() {
        return this.mPass;
    }

    /**
     * Method that returns if the mountpoint belongs to a virtual filesystem.
     *
     * @return boolean If the mountpoint belongs to a virtual filesystem.
     */
    public boolean isVirtual() {
        return mSecure || mRemote;
    }

    /**
     * Method that returns if the mountpoint belongs to a secure virtual filesystem.
     *
     * @return boolean If the mountpoint belongs to a secure virtual filesystem.
     */
    public boolean isSecure() {
        return mSecure;
    }

    /**
     * Method that returns if the mountpoint belongs to a remote virtual filesystem.
     *
     * @return boolean If the mountpoint belongs to a remote virtual filesystem.
     */
    public boolean isRemote() {
        return mRemote;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDevice == null) ? 0 : mDevice.hashCode());
        result = prime * result + mDump;
        result = prime * result
                + ((mMountPoint == null) ? 0 : mMountPoint.hashCode());
        result = prime * result
                + ((mOptions == null) ? 0 : mOptions.hashCode());
        result = prime * result + mPass;
        result = prime * result + (mRemote ? 1231 : 1237);
        result = prime * result + (mSecure ? 1231 : 1237);
        result = prime * result + ((mType == null) ? 0 : mType.hashCode());
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
        if (getClass() != obj.getClass())
            return false;
        MountPoint other = (MountPoint) obj;
        if (mDevice == null) {
            if (other.mDevice != null)
                return false;
        } else if (!mDevice.equals(other.mDevice))
            return false;
        if (mDump != other.mDump)
            return false;
        if (mMountPoint == null) {
            if (other.mMountPoint != null)
                return false;
        } else if (!mMountPoint.equals(other.mMountPoint))
            return false;
        if (mOptions == null) {
            if (other.mOptions != null)
                return false;
        } else if (!mOptions.equals(other.mOptions))
            return false;
        if (mPass != other.mPass)
            return false;
        if (mRemote != other.mRemote)
            return false;
        if (mSecure != other.mSecure)
            return false;
        if (mType == null) {
            if (other.mType != null)
                return false;
        } else if (!mType.equals(other.mType))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MountPoint [mMountPoint=" + mMountPoint + ", mDevice="
                + mDevice + ", mType=" + mType + ", mOptions=" + mOptions
                + ", mDump=" + mDump + ", mPass=" + mPass + ", mSecure="
                + mSecure + ", mRemote=" + mRemote + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(MountPoint another) {
        return this.mMountPoint.compareTo(another.mMountPoint);
    }


}
