/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.cyanogenmod.filemanager.model;

import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

/**
 * DiskUsageCategory
 * <pre>
 *     Category by mime type and the amount of bytes it is using
 * </pre>
 */
public class DiskUsageCategory {

    // Members
    private MimeTypeCategory mCategory;
    private long mSizeBytes = 0l;

    /**
     * Simple constructor
     */
    public DiskUsageCategory() {
    }

    /**
     * Constructor
     *
     * @param category  {@link com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory}
     * @param sizeBytes {@link java.lang.Long}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public DiskUsageCategory(MimeTypeCategory category, long sizeBytes)
            throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException("'category' may not be null!");
        }
        mCategory = category;
        mSizeBytes = sizeBytes;
    }

    public MimeTypeCategory getCategory() {
        return mCategory;
    }

    public void setCategory(MimeTypeCategory category) {
        mCategory = category;
    }

    public long getSizeBytes() {
        return mSizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        mSizeBytes = sizeBytes;
    }

}
