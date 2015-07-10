/*
 * Copyright (C) 2015 The CyanogenMod Project
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
public class RootDirectory extends FileSystemObject {

    /**
     * Constructor of <code>FileSystemObject</code>.
     *
     * @param name             The name of the object
     * @param providerPrefix   The prefix to file path that represents a specific provider
     * @param id               The id of the object
     * @param summary          The summary for the root directory
     * @param icon             The root's icon
     * @param primaryColor     The roots primary color
     */
    public RootDirectory(String name, String providerPrefix, String id,
                         String summary, int icon, int primaryColor) {
        super(name, null, providerPrefix, id, null, null, null, 0L, null, null, null);
        if (icon != -1) {
            setResourceIconId(icon);
        }
    }

    @Override
    public char getUnixIdentifier() {
        return 0;
    }
}