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
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import android.text.TextUtils;
import android.util.Log;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo.ProviderInfoListResult;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.NavigationDrawerItem.NavigationDrawerItemType;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.preferences.AccessMode;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SDCARD;
import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USB;

/**
 * A helper class with useful methods for deal with storages.
 */
public final class StorageHelper {
    private static final String TAG = StorageHelper.class.getSimpleName();
    private static boolean DEBUG = false;
    private static final String STR_USB = "usb"; // $NON-NLS-1$

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
        if (StorageApiConsole.getStorageApiConsoleForPath(path) != null) {
            return true;
        }

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
     * Method that returns the volume storage path that the parameter belongs to
     *
     * @param path The path
     * @return volumePath, valid path if found, null if the path is not in a volume storage
     */
    public static String getStorageVolumeFromPath(String path) {
        String volumePath = null;
        if (StorageApiConsole.getStorageApiConsoleForPath(path) != null) {
            return String.valueOf(StorageApiConsole.getHashCodeFromStorageApiPath(path));
        }

        String fso = FileHelper.getAbsPath(path);

        // Check root
        volumePath = fileStartsWithPath(fso, FileHelper.ROOT_DIRECTORY, volumePath);

        // Check virtual mount points (secure storage)
        List<MountPoint> mps = VirtualMountPointConsole.getVirtualMountPoints();
        for (MountPoint mp : mps) {
            if (mp.isSecure()) {
                volumePath = fileStartsWithPath(fso, mp.getMountPoint(), volumePath);
            }
        }

        // Check storage volumes (sdcard, usb, etc)
        StorageVolume[] volumes =
                getStorageVolumes(FileManagerApplication.getInstance().getApplicationContext(),
                        false);
        int cc = volumes.length;
        for (int i = 0; i < cc; i++) {
            StorageVolume vol = volumes[i];
            volumePath = fileStartsWithPath(fso, vol.getPath(), volumePath);
        }
        return volumePath;
    }

    /**
     * Method returns volume root path closer related to file specified
     *
     * @param file the file to find closest mount point of
     * @param path the current path to check
     * @param volumePath the current mount path which is determined to be closest to the file path
     */
    private static String fileStartsWithPath(String file, String path, String volumePath) {
        if (file.startsWith(path)) {
            if (volumePath == null || path.length() > volumePath.length()) {
                volumePath = path;
            }
        }
        return volumePath;
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

    public static String getLocalStoragePath(Context ctx) {
        String path = null;
        StorageVolume[] volumes =
                StorageHelper.getStorageVolumes(ctx, false);
        if (volumes != null && volumes.length > 0) {
            //Ensure that initial directory is an absolute directory
            path = FileHelper.getAbsPath(volumes[0].getPath());
        }
        return path;
    }

    /**
     * Returns the list of mounted volumes (local, sdcard, usb, storage providers, protected)
     * NOTE: This is blocking and should be called asynchronously...
     *
     * @return files List of FileSystemObject representing each mounted volume
     */
    public static List<FileSystemObject> getStorageVolumesFileSystemObjectList(Context ctx) {
        List<FileSystemObject> files = loadStorageVolumeList(ctx);
        return files;
    }

    private static List<FileSystemObject> loadStorageVolumeList(Context ctx) {
        List<FileSystemObject> storageVolumesList = new ArrayList<FileSystemObject>();

        String title = null;
        String summary = null;
        int color;

        // Determine display mode
        boolean showRoot = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;
        NavigationDrawerItemType itemType = showRoot ?
                NavigationDrawerItemType.DOUBLE : NavigationDrawerItemType.SINGLE;

        // Load Local Storage
        title = ctx.getResources().getString(R.string.navigation_item_title_local);
        summary = StorageHelper.getLocalStoragePath(ctx);
        color = ctx.getResources().getColor(R.color.default_primary);
        storageVolumesList.add(new RootDirectory(title, summary, summary,
                R.drawable.ic_source_internal, color));

        // Show/hide root
        if (showRoot) {
            title = ctx.getString(R.string.navigation_item_title_root);
            summary = FileHelper.ROOT_DIRECTORY;
            color = ctx.getResources().getColor(R.color.root_primary);
            storageVolumesList.add(new RootDirectory(title, summary, summary,
                    R.drawable.ic_source_root_d, color));
        }

        loadExternalStorageItems(ctx, storageVolumesList, itemType);

        // Load storage Providers
        loadStorageProviders(ctx, storageVolumesList);

        loadSecureStorage(ctx, storageVolumesList);

        return storageVolumesList;
    }

    /**
     * Method that loads the secure digital card and usb storage menu items from the system.
     */
    private static void loadExternalStorageItems(Context ctx,
            List<FileSystemObject> storageVolumesList, NavigationDrawerItemType itemType) {
        List<Bookmark> sdBookmarks = new ArrayList<Bookmark>();
        List<Bookmark> usbBookmarks = new ArrayList<Bookmark>();

        try {
            // Recovery sdcards and usb from storage manager
            StorageVolume[] volumes =
                    StorageHelper.getStorageVolumes(ctx, true);
            for (StorageVolume volume: volumes) {
                if (volume != null) {
                    String mountedState = volume.getState();
                    String path = volume.getPath();
                    if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(mountedState) &&
                            !Environment.MEDIA_MOUNTED_READ_ONLY.equalsIgnoreCase(mountedState)) {
                        if (DEBUG) {
                            Log.w(TAG, "Ignoring '" + path + "' with state of '"
                                    + mountedState + "'");
                        }
                        continue;
                    }
                    if (!TextUtils.isEmpty(path)) {
                        String lowerPath = path.toLowerCase(Locale.ROOT);
                        if (lowerPath.contains(STR_USB)) {
                            usbBookmarks.add(new Bookmark(USB, StorageHelper
                                    .getStorageVolumeDescription(ctx,
                                            volume), path));
                        } else {
                            sdBookmarks.add(new Bookmark(SDCARD, StorageHelper
                                    .getStorageVolumeDescription(ctx,
                                            volume), path));
                        }
                    }
                }
            }

            String localStorage = ctx.getString(R.string.local_storage_path);

            // Load the bookmarks
            for (Bookmark b : sdBookmarks) {
                if (TextUtils.equals(b.getPath(), localStorage)) continue;
                int hash = b.hashCode();
                int color = ctx.getResources().getColor(R.color.sdcard_primary);
                storageVolumesList.add(new RootDirectory(b.getName(), b.getPath(), b.getPath(),
                        R.drawable.ic_source_sd_card, color));
            }
            for (Bookmark b : usbBookmarks) {
                int hash = b.hashCode();
                int color = ctx.getResources().getColor(R.color.usb_primary);
                storageVolumesList.add(new RootDirectory(b.getName(), b.getPath(), b.getPath(),
                        R.drawable.ic_source_usb, color));
            }
        }
        catch (Throwable ex) {
            Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }
    }

    /**
     * Method that loads the secure storage mount point.
     */
    private static void loadSecureStorage(Context ctx, List<FileSystemObject> storageVolumesList) {
        List<MountPoint> mps = VirtualMountPointConsole.getVirtualMountPoints();
        for (MountPoint mp : mps) {
            BOOKMARK_TYPE type = null;
            String name = null;
            if (mp.isSecure()) {
                type = BOOKMARK_TYPE.SECURE;
                name = ctx.getString(R.string.navigation_item_title_protected);
                Bookmark b = new Bookmark(type, name, mp.getMountPoint());

                int color = ctx.getResources().getColor(R.color.protected_primary);
                storageVolumesList.add(new RootDirectory(b.getName(), b.getPath(), b.getPath(),
                        R.drawable.ic_source_protected, color));
                break;
            } else {
                continue;
            }
        }
    }

    private static void loadStorageProviders(Context ctx,
            List<FileSystemObject> storageVolumesList) {
        StorageApi storageApi = StorageApi.getInstance();
        PendingResult<ProviderInfoListResult> pendingResult = storageApi.fetchProviders();
        ProviderInfoListResult providerInfoListResult = pendingResult.await();
        if (providerInfoListResult == null || !providerInfoListResult.getStatus().isSuccess()) {
            Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
        } else {
            List<StorageProviderInfo> providerInfoList =
                    providerInfoListResult.getProviderInfoList();
            if (providerInfoList == null) {
                Log.e(TAG, "no results returned");
                return;
            }
            if (DEBUG) Log.v(TAG, "got result(s)! " + providerInfoList.size());
            // TODO: Add to Navigation Drawer alphabetically
            for (StorageProviderInfo providerInfo : providerInfoList) {
                StorageApi sapi = StorageApi.getInstance();

                if (!providerInfo.needAuthentication() &&
                        !TextUtils.isEmpty(providerInfo.getPackage()) &&
                        !TextUtils.isEmpty(providerInfo.getTitle()) &&
                        !TextUtils.isEmpty(providerInfo.getSummary())) {
                    int providerHashCode = StorageApiConsole.getHashCodeFromProvider(providerInfo);

                    // Verify console exists, or create one
                    StorageApiConsole.registerStorageApiConsole(ctx, sapi, providerInfo);

                    // Add to storage volume list
                    addProviderInfoItem(ctx, storageVolumesList, providerHashCode, providerInfo);
                }
            }
        }
    }

    private static void addProviderInfoItem(Context ctx, List<FileSystemObject> storageVolumesList,
            int providerHashCode, StorageProviderInfo providerInfo) {
        if (providerInfo == null) {
            return;
        }

        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                providerInfo.getRootDocumentId(), providerHashCode);
        Drawable icon = StorageProviderUtils.loadPackageIcon(ctx,
                providerInfo.getAuthority(), providerInfo.getIcon());

        int color = StorageProviderUtils.loadProviderColor(ctx, providerInfo.getAuthority(),
                providerInfo.getPrimaryColor());

        if (icon != null) {
            FileSystemObject file = new RootDirectory(providerInfo.getTitle(),
                    providerInfo.getSummary(), path, -1/*icon*/, color);
            storageVolumesList.add(file);
        }
    }
}
