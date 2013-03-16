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
 * An enumeration of all implemented uncompression modes.
 */
public enum UncompressionMode {
    /**
     * Uncompress using Tar algorithm
     */
    A_UNTAR("tar", true), //$NON-NLS-1$
    /**
     * Uncompress using Tar algorithm
     */
    A_UNZIP("zip", true), //$NON-NLS-1$
    /**
     * Uncompress using Gzip algorithm
     */
    AC_GUNZIP("tar.gz", true), //$NON-NLS-1$
    /**
     * Uncompress using Gzip algorithm
     */
    AC_GUNZIP2("tgz", true), //$NON-NLS-1$
    /**
     * Uncompress using Bzip algorithm
     */
    AC_BUNZIP("tar.bz2", true), //$NON-NLS-1$
    /**
     * Uncompress using Lzma algorithm
     */
    AC_UNLZMA("tar.lzma", true), //$NON-NLS-1$
    /**
     * Uncompress using Gzip algorithm
     */
    C_GUNZIP("gz", false), //$NON-NLS-1$
    /**
     * Uncompress using Bzip algorithm
     */
    C_BUNZIP("bz2", false), //$NON-NLS-1$
    /**
     * Uncompress using Lzma algorithm
     */
    C_UNLZMA("lzma", false), //$NON-NLS-1$
    /**
     * Uncompress using Unix compress algorithm
     */
    C_UNCOMPRESS("Z", false), //$NON-NLS-1$
    /**
     * Uncompress using Unix compress algorithm
     */
    C_UNXZ("xz", false), //$NON-NLS-1$
    /**
     * Uncompress using Rar algorithm
     */
    C_UNRAR("rar", true); //$NON-NLS-1$

    /**
     * The file extension
     */
    public final String mExtension;
    /**
     * If the file is an archive or archive-compressed (true) or a compressed file (false)
     */
    public final boolean mArchive;

    /**
     * Constructor of <code>UncompressionMode</code>
     *
     * @param extension The file extension
     * @param archive If the file is an archive or archive-compressed
     */
    private UncompressionMode(String extension, boolean archive) {
        this.mExtension = extension;
        this.mArchive = archive;
    }
}
