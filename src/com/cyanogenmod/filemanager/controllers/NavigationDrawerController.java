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

package com.cyanogenmod.filemanager.controllers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.support.design.widget.NavigationView;
import android.text.TextUtils;
import android.util.Log;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo.ProviderInfoListResult;
import com.cyanogenmod.filemanager.FileManagerApplication;

import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SDCARD;
import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SECURE;
import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USB;

/**
 * NavigationDrawerController. This class is contains logic to add/remove and manage items in
 * the NavigationDrawer which uses android support libraries NavigationView.
 */
public class NavigationDrawerController
        implements ResultCallback<ProviderInfoListResult> {
    private static final String TAG = NavigationDrawerController.class.getSimpleName();
    private static boolean DEBUG = false;
    public List<StorageProviderInfo> mProviderInfoList;
    private static final String STR_USB = "usb"; // $NON-NLS-1$

    int[][] color_states = new int[][] {
            new int[] {android.R.attr.state_checked}, // checked
            new int[0] // default
    };

    // TODO: Replace with legitimate colors per item.
    int[] colors = new int[] {
            R.color.favorites_primary,
            Color.BLACK
    };

    private Context mCtx;
    private NavigationView mNavigationDrawer;
    private Map<Integer, StorageProviderInfo> mProvidersMap;
    private Map<Integer, Bookmark> mStorageBookmarks;

    public NavigationDrawerController(Context ctx, NavigationView navigationView) {
        mCtx = ctx;
        mNavigationDrawer = navigationView;
        mProvidersMap = new HashMap<Integer, StorageProviderInfo>();
        mStorageBookmarks = new HashMap<Integer, Bookmark>();

        ColorStateList colorStateList = new ColorStateList(color_states, colors);
        // TODO: Figure out why the following doesn't work correctly...
        mNavigationDrawer.setItemTextColor(colorStateList);
        mNavigationDrawer.setItemIconTintList(colorStateList);
    }

    @Override
    public void onResult(StorageProviderInfo.ProviderInfoListResult providerInfoListResult) {
        mProviderInfoList =
                providerInfoListResult.getProviderInfoList();
        if (mProviderInfoList == null) {
            Log.e(TAG, "no results returned");
            return;
        }
        if (DEBUG) Log.v(TAG, "got result(s)! " + mProviderInfoList.size());
        // TODO: Add to Navigation Drawer alphabetically
        for (StorageProviderInfo providerInfo : mProviderInfoList) {
            StorageApi sapi = StorageApi.getInstance();

            if (!providerInfo.needAuthentication()) {
                int providerHashCode = StorageApiConsole.getHashCodeFromProvider(providerInfo);

                // Verify console exists, or create one
                StorageApiConsole.registerStorageApiConsole(mCtx, sapi, providerInfo);

                // Add to navigation drawer controller
                addProviderInfoItem(providerHashCode, providerInfo);
            }
        }
    }


    public List<StorageProviderInfo> getProviderList() {
        return mProviderInfoList;
    }

    public void loadNavigationDrawerItems() {
        // clear current special nav drawer items
        removeAllDynamicMenuItemsFromDrawer();

        // Show/hide root
        boolean showRoot = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;
        mNavigationDrawer.getMenu().findItem(R.id.navigation_item_root_d).setVisible(showRoot);

        loadExternalStorageItems();
        StorageApi storageApi = StorageApi.getInstance();
        PendingResult<StorageProviderInfo.ProviderInfoListResult> pendingResult =
                storageApi.fetchProviders(this);
    }

    /**
     * Method that loads the secure digital card and usb storage menu items from the system.
     *
     * @return List<MenuItem> The storage items to be displayed
     */
    private void loadExternalStorageItems() {
        List<Bookmark> sdBookmarks = new ArrayList<Bookmark>();
        List<Bookmark> usbBookmarks = new ArrayList<Bookmark>();

        try {
            // Recovery sdcards and usb from storage manager
            StorageVolume[] volumes =
                    StorageHelper.getStorageVolumes(mCtx, true);
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
                        Bookmark bookmark;
                        if (lowerPath.contains(STR_USB)) {
                            usbBookmarks.add(new Bookmark(USB, StorageHelper
                                    .getStorageVolumeDescription(mCtx,
                                            volume), path));
                        } else {
                            sdBookmarks.add(new Bookmark(SDCARD, StorageHelper
                                    .getStorageVolumeDescription(mCtx,
                                            volume), path));
                        }
                    }
                }
            }

            String localStorage = mCtx.getResources().getString(R.string.local_storage_path);

            // Load the bookmarks
            for (Bookmark b : sdBookmarks) {
                if (TextUtils.equals(b.getPath(), localStorage)) continue;
                int hash = b.hashCode();
                addMenuItemToDrawer(hash, b.getName(), R.drawable.ic_sdcard_drawable);
                mStorageBookmarks.put(hash, b);
            }
            for (Bookmark b : usbBookmarks) {
                int hash = b.hashCode();
                addMenuItemToDrawer(hash, b.getName(), R.drawable.ic_usb_drawable);
                mStorageBookmarks.put(hash, b);
            }
        }
        catch (Throwable ex) {
            Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }
    }

    private void addMenuItemToDrawer(int hash, String title, int iconDrawable) {
        if (mNavigationDrawer.getMenu().findItem(hash) == null) {
            mNavigationDrawer.getMenu()
                    .add(R.id.navigation_group_roots, hash, 0, title)
                    .setIcon(iconDrawable);
        }
    }

    public void removeMenuItemFromDrawer(int hash) {
        mNavigationDrawer.getMenu().removeItem(hash);
    }

    public void removeAllDynamicMenuItemsFromDrawer() {
        for (int key : mStorageBookmarks.keySet()) {
            removeMenuItemFromDrawer(key);
            mStorageBookmarks.remove(key);
        }
        for (int key : mProvidersMap.keySet()) {
            removeMenuItemFromDrawer(key);
            mProvidersMap.remove(key);
        }
    }

    public void addProviderInfoItem(int providerHashCode, StorageProviderInfo providerInfo) {
        // Concatenate title and summary
        // TODO: Change to two line menu items
        String title = providerInfo.getTitle() + " " + providerInfo.getSummary();

        mProvidersMap.put(providerHashCode, providerInfo);
        addMenuItemToDrawer(providerHashCode, title, R.drawable.ic_remote_drawable);
    }

    public StorageProviderInfo getProviderInfoFromMenuItem(int key) {
        return mProvidersMap.get(key);
    }

    public Bookmark getBookmarkFromMenuItem(int key) {
        return mStorageBookmarks.get(key);
    }
}
