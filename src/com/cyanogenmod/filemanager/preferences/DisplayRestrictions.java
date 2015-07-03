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

package com.cyanogenmod.filemanager.preferences;

/**
 * An enumeration of the restrictions that can be applied when displaying list of files.
 */
public enum DisplayRestrictions {
    /**
     * Restriction for display only files with the category.
     */
    CATEGORY_TYPE_RESTRICTION,
    /**
     * Restriction for display only files with these mime/types (this restriction
     * accepts a String or String[] as parameter).
     */
    MIME_TYPE_RESTRICTION,
    /**
     * Restriction for display only files with a size lower than the specified
     */
    SIZE_RESTRICTION,
    /**
     * Restriction for display only directories
     */
    DIRECTORY_ONLY_RESTRICTION,
    /**
     * Restriction for display only files from the local file system. Avoid remote files.
     */
    LOCAL_FILESYSTEM_ONLY_RESTRICTION
}
