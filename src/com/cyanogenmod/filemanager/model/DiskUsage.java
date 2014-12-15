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
import java.util.ArrayList;
import java.util.List;

/**
 * A class that holds information about the usage of a disk (total, used and free space).
 */
public class DiskUsage implements Serializable {

    private static final long serialVersionUID = -4540446701543226294L;

    private final List<DiskUsageCategory> mDiskUsageCategoryList =
            new ArrayList<DiskUsageCategory>();
    private final String mMountPoint;
    private final long mTotal;
    private final long mUsed;
    private final long mFree;

    /**
     * Constructor of <code>DiskUsage</code>.
     *
     * @param mountPoint The mount point
     * @param total The total amount of space
     * @param used The used amount of space
     * @param free The free amount of space
     */
    public DiskUsage(String mountPoint, long total, long used, long free) {
        super();
        this.mMountPoint = mountPoint;
        this.mTotal = total;
        this.mUsed = used;
        this.mFree = free;
    }

    /**
     * Method that returns the mount point.
     *
     * @return String The mount point
     */
    public String getMountPoint() {
        return this.mMountPoint;
    }

    /**
     * Method that returns the total amount of space.
     *
     * @return long The total amount of space
     */
    public long getTotal() {
        return this.mTotal;
    }

    /**
     * Method that returns the used amount of space.
     *
     * @return long The used amount of space
     */
    public long getUsed() {
        return this.mUsed;
    }

    /**
     * Method that returns the free amount of space.
     *
     * @return long The free amount of space
     */
    public long getFree() {
        return this.mFree;
    }

    /**
     * Method that returns the total sum of all categories
     *
     * @return {@link java.lang.Long}
     */
    public long getCategorySum() {
        long bytes = 0;
        for (DiskUsageCategory category : getUsageCategoryList()) {
            bytes += category.getSizeBytes();
        }
        return bytes;
    }

    /**
     * Add a usage category
     *
     * @param category {@link com.cyanogenmod.filemanager.model.DiskUsageCategory} not null
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public void addUsageCategory(DiskUsageCategory category) throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException("'category' cannot be null!");
        }
        mDiskUsageCategoryList.add(category);
    }

    public List<DiskUsageCategory> getUsageCategoryList() {
        return mDiskUsageCategoryList;
    }

    /**
     * Clears the list of usage categories
     */
    public void clearUsageCategories() {
        mDiskUsageCategoryList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.mFree ^ (this.mFree >>> 32));
        result = prime * result + ((this.mMountPoint == null) ? 0 : this.mMountPoint.hashCode());
        result = prime * result + (int) (this.mTotal ^ (this.mTotal >>> 32));
        result = prime * result + (int) (this.mUsed ^ (this.mUsed >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DiskUsage other = (DiskUsage) obj;
        if (this.mFree != other.mFree) {
            return false;
        }
        if (this.mMountPoint == null) {
            if (other.mMountPoint != null) {
                return false;
            }
        } else if (!this.mMountPoint.equals(other.mMountPoint)) {
            return false;
        }
        if (this.mTotal != other.mTotal) {
            return false;
        }
        if (this.mUsed != other.mUsed) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DiskUsage [mMountPoint=" + this.mMountPoint //$NON-NLS-1$
                + ", mTotal=" + this.mTotal + //$NON-NLS-1$
                ", mUsed=" + this.mUsed + ", mFree=" //$NON-NLS-1$ //$NON-NLS-2$
                + this.mFree + "]";  //$NON-NLS-1$
    }

}
