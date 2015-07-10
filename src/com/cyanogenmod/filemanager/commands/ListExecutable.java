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

package com.cyanogenmod.filemanager.commands;

import com.cyanogenmod.filemanager.model.FileSystemObject;

import java.util.List;

/**
 * An interface that represents an executable for list a directory.
 */
public interface ListExecutable extends SyncResultExecutable {

    /**
     * An enumeration of listing modes.
     */
    public enum LIST_MODE {
        /**
         * This list mode retrieves information of a list of file system
         * objects of a directory.
         */
        DIRECTORY,
        /**
         * This list mode retrieves information of a unique file system
         * objects.
         */
        FILEINFO,
        /**
         * This list mode retrieves information of a list of file system
         * objects of a directory, plus adds a link to the roots list when
         * the current directory has no parents.
         */
        PICKER
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<FileSystemObject> getResult();
}
