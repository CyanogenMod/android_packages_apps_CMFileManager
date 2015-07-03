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
package com.cyanogenmod.filemanager.util;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * A helper class with useful methods for deal with storages.
 */
public final class StorageHelper {

    private static StorageVolume[] sStorageVolumes;

    /**
     * Method that returns the storage volumes defined in the system.  This method uses
     * reflection to retrieve the method because CM10 has a {@link Context}
     * as first parameter, that AOSP hasn't.
     *
     * @param ctx The current context
     * @param reload If true, re-query the volumes and do not return the already cached list.
     * @return StorageVolume[] The storage volumes defined in the system
     */
    @SuppressWarnings("boxing")
    public static synchronized StorageVolume[] getStorageVolumes(Context ctx, boolean reload) {
        if (sStorageVolumes == null || reload) {
            //IMP!! Android SDK doesn't have a "getVolumeList" but is supported by CM10.
            //Use reflect to get this value (if possible)
            try {
                StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
                Method method = sm.getClass().getMethod("getVolumeList"); //$NON-NLS-1$
                sStorageVolumes = (StorageVolume[])method.invoke(sm);

            } catch (Exception ex) {
                //Ignore. Android SDK StorageManager class doesn't have this method
                //Use default android information from environment
                try {
                    File externalStorage = Environment.getExternalStorageDirectory();
                    String path = externalStorage.getCanonicalPath();
                    String description = null;
                    if (path.toLowerCase(Locale.ROOT).indexOf("usb") != -1) { //$NON-NLS-1$
                        description = ctx.getString(R.string.usb_storage);
                    } else {
                        description = ctx.getString(R.string.external_storage);
                    }
                    // Android SDK has a different constructor for StorageVolume. In CM10 the
                    // description is a resource id. Create the object by reflection
                    Constructor<StorageVolume> constructor =
                            StorageVolume.class.
                                getConstructor(
                                        String.class,
                                        String.class,
                                        boolean.class,
                                        boolean.class,
                                        int.class,
                                        boolean.class,
                                        long.class);
                    StorageVolume sv =
                            constructor.newInstance(path, description, false, false, 0, false, 0);
                    sStorageVolumes = new StorageVolume[]{sv};
                } catch (Exception ex2) {
                    /**NON BLOCK**/
                }
            }
            if (sStorageVolumes == null) {
                sStorageVolumes = new StorageVolume[]{};
            }
        }
        return sStorageVolumes;
    }

    /**
     * Method that returns the storage volume description. This method uses
     * reflection to retrieve the description because CM10 has a {@link Context}
     * as first parameter, that AOSP hasn't.
     *
     * @param ctx The current context
     * @param volume The storage volume
     * @return String The description of the storage volume
     */
    public static String getStorageVolumeDescription(Context ctx, StorageVolume volume) {
        try {
            Method method = volume.getClass().getMethod(
                                            "getDescription", //$NON-NLS-1$
                                            new Class[]{Context.class});
            if (method == null) {
                // AOSP
                method = volume.getClass().getMethod("getDescription"); //$NON-NLS-1$
                return (String)method.invoke(volume);
            }

            // CM10
            return (String)method.invoke(volume, ctx);

        } catch (Throwable _throw) {
            // Returns the volume storage path
            return volume.getPath();
        }
    }

    /**
     * Method that returns if the path is in a volume storage
     *
     * @param path The path
     * @return boolean If the path is in a volume storage
     */
    public static boolean isPathInStorageVolume(String path) {
        String fso = FileHelper.getAbsPath(path);
        StorageVolume[] volumes =
                getStorageVolumes(FileManagerApplication.getInstance().getApplicationContext(),
                                  false);
        int cc = volumes.length;
        for (int i = 0; i < cc; i++) {
            StorageVolume vol = volumes[i];
            if (fso.startsWith(vol.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that returns if the path is a storage volume
     *
     * @param path The path
     * @return boolean If the path is a storage volume
     */
    public static boolean isStorageVolume(String path) {
        StorageVolume[] volumes =
                getStorageVolumes(FileManagerApplication.getInstance().getApplicationContext(),
                                  false);
        int cc = volumes.length;
        for (int i = 0; i < cc; i++) {
            StorageVolume vol = volumes[i];
            String p = new File(path).getAbsolutePath();
            String v = new File(vol.getPath()).getAbsolutePath();
            if (p.compareTo(v) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that return the chrooted path of an absolute path. xe: /storage/sdcard0 --> sdcard0.
     *
     * @param path The path
     * @return String The chrooted path
     */
    public static String getChrootedPath(String path) {
        StorageVolume[] volumes =
                getStorageVolumes(FileManagerApplication.getInstance().getApplicationContext(),
                                  false);
        int cc = volumes.length;
        for (int i = 0; i < cc; i++) {
            StorageVolume vol = volumes[i];
            File p = new File(path);
            File v = new File(vol.getPath());
            if (p.getAbsolutePath().startsWith(v.getAbsolutePath())) {
                return v.getName() + path.substring(v.getAbsolutePath().length());
            }
        }
        return null;
    }

}
