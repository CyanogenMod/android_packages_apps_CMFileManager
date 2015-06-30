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
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.support.design.widget.NavigationView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo.ProviderInfoListResult;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.MainActivity;
import com.cyanogenmod.filemanager.adapters.NavigationDrawerAdapter;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.NavigationDrawerItem;
import com.cyanogenmod.filemanager.model.NavigationDrawerItem.NavigationDrawerItemType;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.util.StorageHelper;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.SDCARD;
import static com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE.USB;

/**
 * NavigationDrawerController. This class is contains logic to add/remove and manage items in
 * the NavigationDrawer which uses android support libraries NavigationView.
 */
public class NavigationDrawerController implements ResultCallback<ProviderInfoListResult> {
    private static final String TAG = NavigationDrawerController.class.getSimpleName();
    private static boolean DEBUG = false;
    private static final String STR_USB = "usb"; // $NON-NLS-1$

    private Context mCtx;
    private NavigationView mNavigationDrawer;
    private NavigationDrawerAdapter mAdapter;
    private List<NavigationDrawerItem> mNavigationDrawerItemList;
    private int mLastRoot;
    private Map<Integer, StorageProviderInfo> mProvidersMap;
    private Map<Integer, Bookmark> mStorageBookmarks;
    public List<StorageProviderInfo> mProviderInfoList;

    public NavigationDrawerController(Context ctx, NavigationView navigationView) {
        mCtx = ctx;
        mNavigationDrawer = navigationView;

        mProvidersMap = new HashMap<Integer, StorageProviderInfo>();
        mStorageBookmarks = new HashMap<Integer, Bookmark>();
        mNavigationDrawerItemList = new ArrayList<NavigationDrawerItem>();
        mLastRoot = 0;
        ListView listView = (ListView)mNavigationDrawer.findViewById(R.id.navigation_view_listview);
        listView.setOnItemClickListener(((MainActivity)mCtx));
        mAdapter = new NavigationDrawerAdapter(mCtx, mNavigationDrawerItemList);
        listView.setAdapter(mAdapter);
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
        mAdapter.notifyDataSetChanged();
    }


    public List<StorageProviderInfo> getProviderList() {
        return mProviderInfoList;
    }

    public void loadNavigationDrawerItems() {
        // clear current special nav drawer items
        removeAllItemsFromDrawer();

        mLastRoot = 0;
        String title = null;
        String summary = null;

        // Determine display mode
        boolean showRoot = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;
        NavigationDrawerItemType itemType = showRoot ?
                NavigationDrawerItemType.DOUBLE : NavigationDrawerItemType.SINGLE;

        // Load Header
        mNavigationDrawerItemList.add(new NavigationDrawerItem(0, NavigationDrawerItemType.HEADER,
                null, null, 0, 0));

        // Load Home and Favorites
        title = mCtx.getResources().getString(R.string.navigation_item_title_home);
        summary = null;
        mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_home,
                NavigationDrawerItemType.SINGLE, title, summary, R.drawable.ic_home,
                R.color.default_primary));
        title = mCtx.getResources().getString(R.string.navigation_item_title_favorites);
        summary = null;
        mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_favorites,
                NavigationDrawerItemType.SINGLE, title, summary, R.drawable.ic_favorite_on,
                R.color.favorites_primary));

        // Divider
        mNavigationDrawerItemList.add(new NavigationDrawerItem(0, NavigationDrawerItemType.DIVIDER,
                null, null, 0, 0));

        // Load Local Storage
        title = mCtx.getResources().getString(R.string.navigation_item_title_local);
        summary = null;
        mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_internal,
                itemType, title, summary, R.drawable.ic_source_internal, R.color.default_primary));

        // Show/hide root
        if (showRoot) {
            title = mCtx.getResources().getString(R.string.navigation_item_title_root);
            summary = null;
            mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_root_d,
                    itemType, title, null, R.drawable.ic_source_root_d, R.color.root_primary));
        }

        //loadExternalStorageItems();

        //loadSecureStorage();
        //R.id.navigation_item_protected

        // Grab storageapi providers insertion spot in list.
        mLastRoot = mNavigationDrawerItemList.size();

        // Divider
        mNavigationDrawerItemList.add(new NavigationDrawerItem(0, NavigationDrawerItemType.DIVIDER,
                null, null, 0, 0));

        // Load manage storage and settings
        title = mCtx.getResources().getString(R.string.navigation_item_title_manage);
        summary = null;
        mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_manage,
                NavigationDrawerItemType.SINGLE, title, summary, R.drawable.ic_storage_sources,
                R.color.misc_primary));
        title = mCtx.getResources().getString(R.string.navigation_item_title_settings);
        summary = null;
        mNavigationDrawerItemList.add(new NavigationDrawerItem(R.id.navigation_item_settings,
                NavigationDrawerItemType.SINGLE, title, summary, R.drawable.ic_settings,
                R.color.misc_primary));

        // Notify dataset changed here because we aren't sure when/if storage providers will return.
        mAdapter.notifyDataSetChanged();

        // Load storage providers, This is done last because and is asynchronous, and it has its own
        // call to notifiyDataSetChanged.
        StorageApi storageApi = StorageApi.getInstance();
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
        /*if (mNavigationDrawer.getMenu().findItem(hash) == null) {
            mNavigationDrawer.getMenu()
                    .add(R.id.navigation_group_roots, hash, 0, title)
                    .setIcon(iconDrawable);
        }*/
    }

    private void addMenuItemToDrawer(int hash, String title, Drawable iconDrawable) {
        /*if (mNavigationDrawer.getMenu().findItem(hash) == null) {
            mNavigationDrawer.getMenu()
                    .add(R.id.navigation_group_roots, hash, 0, title)
                    .setIcon(iconDrawable);
        }*/
    }

    public void removeAllItemsFromDrawer() {
        // reset menu list
        mNavigationDrawerItemList.clear();
        mLastRoot = 0;

        // reset hashmaps
        mStorageBookmarks.clear();
        mProvidersMap.clear();
    }

    public void addProviderInfoItem(int providerHashCode, StorageProviderInfo providerInfo) {
        Drawable icon = StorageProviderUtils.loadPackageIcon(mCtx, providerInfo.getAuthority(),
                providerInfo.getIcon());
        mProvidersMap.put(providerHashCode, providerInfo);
        mNavigationDrawerItemList.add(mLastRoot++, new NavigationDrawerItem(providerHashCode,
                NavigationDrawerItemType.DOUBLE, providerInfo.getTitle(), providerInfo.getSummary(),
                icon, R.color.misc_primary));
    }

    public StorageProviderInfo getProviderInfoFromMenuItem(int key) {
        return mProvidersMap.get(key);
    }

    public Bookmark getBookmarkFromMenuItem(int key) {
        return mStorageBookmarks.get(key);
    }
}
