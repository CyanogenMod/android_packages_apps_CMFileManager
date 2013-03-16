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
 * An enumeration of all implemented compression modes.
 */
public enum CompressionMode {
    /**
     * Archive using Tar algorithm
     */
    A_TAR("tar", true, null), //$NON-NLS-1$
    /**
     * Archive and compress using Gzip algorithm
     */
    AC_GZIP("tar.gz", true, null), //$NON-NLS-1$
    /**
     * Archive and compress using Gzip algorithm
     */
    AC_GZIP2("tgz", true, null), //$NON-NLS-1$
    /**
     * Archive and compress using Bzip algorithm
     */
    AC_BZIP("tar.bz2", true, null), //$NON-NLS-1$
    /**
     * Compress using Gzip algorithm
     */
    C_GZIP("gz", false, null), //$NON-NLS-1$
    /**
     * Compress using Bzip algorithm
     */
    C_BZIP("bz2", false, null), //$NON-NLS-1$
    /**
     * Archive using Zip algorithm
     */
    A_ZIP("zip", true, "zip"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * The file extension
     */
    public final String mExtension;
    /**
     * If the file is an archive or archive-compressed (true) or a compressed file (false)
     */
    public final boolean mArchive;
    /**
     * If the compress mode requires the present of an optional file (null == required)
     */
    public final String mCommandId;

    /**
     * Constructor of <code>CompressionMode</code>
     *
     * @param extension The output extension
     * @param archive If the output is an archive or archive-compressed
     */
    private CompressionMode(String extension, boolean archive, String commandId) {
        this.mExtension = extension;
        this.mArchive = archive;
        this.mCommandId = commandId;
    }
}
