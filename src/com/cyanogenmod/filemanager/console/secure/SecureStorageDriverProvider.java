/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.console.secure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.file.FileDriver;

/**
 * The SecureStorage driver provider which handles {@code "secure"} data schemes
 */
public class SecureStorageDriverProvider implements FsDriverProvider {

    /** File scheme **/
    public static final String FILE_SCHEME = "file";

    /** SecureStorage scheme **/
    public static final String SECURE_STORAGE_SCHEME = "secure";

    /** The singleton instance of this class. */
    static final SecureStorageDriverProvider SINGLETON = new SecureStorageDriverProvider();

    /** You cannot instantiate this class. */
    private SecureStorageDriverProvider() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<FsScheme, FsDriver> get() {
        return Boot.DRIVERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<FsScheme, FsDriver> DRIVERS;
        static {
            final Map<FsScheme, FsDriver> fast = new LinkedHashMap<FsScheme, FsDriver>();
            fast.put(FsScheme.create(FILE_SCHEME), new FileDriver());
            fast.put(FsScheme.create(SECURE_STORAGE_SCHEME), SecureStorageDriver.SINGLETON);
            DRIVERS = Collections.unmodifiableMap(fast);
        }
    } // Boot
}
