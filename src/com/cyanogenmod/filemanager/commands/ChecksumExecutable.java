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

/**
 * An interface that represents an executable for calculate checksum of file system objects.
 */
public interface ChecksumExecutable extends AsyncResultExecutable {

    /**
     * Checksum enumerations
     */
    public enum CHECKSUMS {
        /**
         * MD5 digest algorithm
         */
        MD5,
        /**
         * SHA-1 digest algorithm
         */
        SHA1
    }

    /**
     * Method that returns the calculated MD5 [0] and SHA-1 [1] digests
     *
     * @return String[] The calculated MD5 [0] and SHA-1 [1] digests
     */
    String[] getResult();

    /**
     * Method that returns a calculated digest checksum
     *
     * @param checksum The checksum to return
     * @return String The calculated digest to return
     */
    String getChecksum(CHECKSUMS checksum);
}
