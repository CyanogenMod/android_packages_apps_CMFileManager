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

package com.cyanogenmod.filemanager.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.cyanogenmod.filemanager.compat.CompatUtils;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.KnownMimeTypeResolver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private static final int MAX_CACHE = 500;

    private static final int MSG_LOAD = 1;
    private static final int MSG_LOADED = 2;
    private static final int MSG_DESTROY = 3;

    private final Map<String, Drawable> mIcons;     // Themes based
    private final Map<String, Drawable> mAppIcons;  // App based

    private Map<String, Long> mAlbums;      // Media albums

    private Map<ImageView, FileSystemObject> mRequests;

    private final Context mContext;
    private final boolean mUseThumbs;
    private boolean mNeedAlbumUpdate = true;

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private static class LoadResult {
        FileSystemObject fso;
        Drawable result;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADED:
                    processResult((LoadResult) msg.obj);
                    sendEmptyMessageDelayed(MSG_DESTROY, 3000);
                    break;
                case MSG_DESTROY:
                    shutdownWorker();
                    break;
            }
        }

        private void processResult(LoadResult result) {
            // Cache the new drawable
            final String filePath = MediaHelper.normalizeMediaPath(result.fso.getFullPath());
            mAppIcons.put(filePath, result.result);

            // find the request for it
            for (Map.Entry<ImageView, FileSystemObject> entry : mRequests.entrySet()) {
                final ImageView imageView = entry.getKey();
                final FileSystemObject fso = entry.getValue();
                if (fso == result.fso) {
                    imageView.setImageDrawable(result.result);
                    mRequests.remove(imageView);
                    break;
                }
            }
        }
    };

    private ContentObserver mMediaObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            synchronized (this) {
                mNeedAlbumUpdate = true;
            }
        }
    };

    /**
     * Constructor of <code>IconHolder</code>.
     *
     * @param useThumbs If thumbs of images, videos, apps, ... should be returned
     * instead of the default icon.
     */
    public IconHolder(Context context, boolean useThumbs) {
        super();
        this.mContext = context;
        this.mUseThumbs = useThumbs;
        this.mRequests = new HashMap<ImageView, FileSystemObject>();
        this.mIcons = new HashMap<String, Drawable>();
        this.mAppIcons = new LinkedHashMap<String, Drawable>(MAX_CACHE, .75F, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Entry<String, Drawable> eldest) {
                return size() > MAX_CACHE;
            }
        };
        this.mAlbums = new HashMap<String, Long>();
        if (useThumbs) {
            final ContentResolver cr = mContext.getContentResolver();
            for (Uri uri : MediaHelper.RELEVANT_URIS) {
                cr.registerContentObserver(uri, true, mMediaObserver);
            }
        }
    }

    /**
     * Method that returns a drawable reference of a icon.
     *
     * @param resid The resource identifier
     * @return Drawable The drawable icon reference
     */
    public Drawable getDrawable(final String resid) {
        //Check if the icon exists in the cache
        if (this.mIcons.containsKey(resid)) {
            return this.mIcons.get(resid);
        }

        //Load the drawable, cache and returns reference
        Theme theme = ThemeManager.getCurrentTheme(mContext);
        Drawable dw = theme.getDrawable(mContext, resid);
        this.mIcons.put(resid, dw);
        return dw;
    }

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param iconView View to load the drawable into
     * @param fso The FileSystemObject reference
     * @param defaultIcon Drawable to be used in case no specific one could be found
     * @return Drawable The drawable reference
     */
    public void loadDrawable(ImageView iconView, FileSystemObject fso, Drawable defaultIcon) {
        if (!mUseThumbs) {
            iconView.setImageDrawable(defaultIcon);
            return;
        }

        // Is cached?
        final String filePath = MediaHelper.normalizeMediaPath(fso.getFullPath());
        if (this.mAppIcons.containsKey(filePath)) {
            iconView.setImageDrawable(this.mAppIcons.get(filePath));
            return;
        }

        mRequests.put(iconView, fso);
        iconView.setImageDrawable(defaultIcon);

        mHandler.removeMessages(MSG_DESTROY);
        if (mWorkerThread == null) {
            mWorkerThread = new HandlerThread("IconHolderLoader");
            mWorkerThread.start();
            mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());
        }
        Message msg = mWorkerHandler.obtainMessage(MSG_LOAD, fso);
        msg.sendToTarget();
    }

    /**
     * Cancel loading of a drawable for a certain ImageView.
     */
    public void cancelLoad(ImageView view) {
        FileSystemObject fso = mRequests.get(view);
        if (fso != null && mWorkerHandler != null) {
            mWorkerHandler.removeMessages(MSG_LOAD, fso);
        }
        mRequests.remove(view);
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD:
                    FileSystemObject fso = (FileSystemObject) msg.obj;
                    Drawable d = loadDrawable(fso);
                    if (d != null) {
                        LoadResult result = new LoadResult();
                        result.fso = fso;
                        result.result = d;
                        mHandler.obtainMessage(MSG_LOADED, result).sendToTarget();
                    }
                    break;
            }
        }

        private Drawable loadDrawable(FileSystemObject fso) {
            final String filePath = MediaHelper.normalizeMediaPath(fso.getFullPath());

            if (KnownMimeTypeResolver.isAndroidApp(mContext, fso)) {
                return getAppDrawable(fso);
            } else if (KnownMimeTypeResolver.isImage(mContext, fso)) {
                return getImageDrawable(filePath);
            } else if (KnownMimeTypeResolver.isVideo(mContext, fso)) {
                return getVideoDrawable(filePath);
            } else if (FileHelper.isDirectory(fso)) {
                synchronized (mMediaObserver) {
                    if (mNeedAlbumUpdate) {
                        mNeedAlbumUpdate = false;
                        mAlbums = MediaHelper.getAllAlbums(mContext.getContentResolver());
                    }
                }
                if (mAlbums.containsKey(filePath)) {
                    return getAlbumDrawable(mAlbums.get(filePath));
                }
            }

            return null;
        }

        /**
         * Method that returns the main icon of the app
         *
         * @param fso The FileSystemObject
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getAppDrawable(FileSystemObject fso) {
            final String filepath = fso.getFullPath();
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
                    PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                // Read http://code.google.com/p/android/issues/detail?id=9151, CM fixed this
                // issue. We retain it for compatibility with older versions and roms without
                // this fix. Required to access apk which are not installed.
                final ApplicationInfo appInfo = packageInfo.applicationInfo;
                appInfo.sourceDir = filepath;
                appInfo.publicSourceDir = filepath;
                return pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo);
            }
            return null;
        }

        /**
         * Method that returns a thumbnail of the picture
         *
         * @param file The path to the file
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getImageDrawable(String file) {
            Bitmap thumb = (Bitmap) CompatUtils.getClsHideObjectStaticMethod(ThumbnailUtils.class, "createImageThumbnail",
                    MediaHelper.normalizeMediaPath(file),
                    CompatUtils.getClsHideIntStaticField(ThumbnailUtils.class, "TARGET_SIZE_MICRO_THUMBNAIL", 96));
            if (thumb == null) {
                return null;
            }
            return new BitmapDrawable(mContext.getResources(), thumb);
        }

        /**
         * Method that returns a thumbnail of the video
         *
         * @param file The path to the file
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getVideoDrawable(String file) {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(
                    MediaHelper.normalizeMediaPath(file),
                    CompatUtils.getClsHideIntStaticField(ThumbnailUtils.class, "TARGET_SIZE_MICRO_THUMBNAIL", 96));
            if (thumb == null) {
                return null;
            }
            return new BitmapDrawable(mContext.getResources(), thumb);
        }

        /**
         * Method that returns a thumbnail of the album folder
         *
         * @param albumId The album identifier
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getAlbumDrawable(long albumId) {
            String path = MediaHelper.getAlbumThumbnailPath(mContext.getContentResolver(), albumId);
            if (path == null) {
                return null;
            }
            Bitmap thumb = (Bitmap) CompatUtils.getClsHideObjectStaticMethod(ThumbnailUtils.class, "createImageThumbnail", path,
                    CompatUtils.getClsHideIntStaticField(ThumbnailUtils.class, "TARGET_SIZE_MICRO_THUMBNAIL", 96));
//            Bitmap thumb = ThumbnailUtils.createImageThumbnail(path,
//            		ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
            if (thumb == null) {
                return null;
            }
            return new BitmapDrawable(mContext.getResources(), thumb);
        }
    }

    /**
     * Shut down worker thread
     */
    private void shutdownWorker() {
        if (mWorkerThread != null) {
            mWorkerThread.getLooper().quit();
            mWorkerHandler = null;
            mWorkerThread = null;
        }
    }

    /**
     * Free any resources used by this instance
     */
    public void cleanup() {
        this.mRequests.clear();
        this.mIcons.clear();
        this.mAppIcons.clear();
        mContext.getContentResolver().unregisterContentObserver(mMediaObserver);
        shutdownWorker();
    }
}
