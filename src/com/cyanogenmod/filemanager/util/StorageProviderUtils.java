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

package com.cyanogenmod.filemanager.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogen.ambient.storage.StorageApi.DocumentInfo.DocumentInfoResult;
import com.cyanogen.ambient.storage.StorageApi.StatusResult;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.storageapi.Program;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * A helper class with useful methods for dealing with Storage Providers.
 */
public final class StorageProviderUtils {

    public static final String CACHE_DIR = ".storage-provider-files";
    private static final String DEFAULT_MIMETYPE = "text/plain";

    private static final String TAG = StorageProviderUtils.class.getSimpleName();

    public static class PathInfo {
        private String mDisplayName;
        private String mPath;

        public PathInfo(String displayName, String path) {
            mDisplayName = displayName;
            mPath = path;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public String getPath() {
            return mPath;
        }
    }

    /**
     * Return the Drawable for this Storage Provider
     * @param context
     * @param authority
     * @param icon
     */
    public static Drawable loadPackageIcon(Context context, String authority, int icon) {
        if (icon != 0) {
            if (authority != null) {
                final PackageManager pm = context.getPackageManager();
                final ProviderInfo info = pm.resolveContentProvider(authority, 0);
                if (info != null) {
                    return pm.getDrawable(info.packageName, icon, info.applicationInfo);
                }
            } else {
                return context.getDrawable(icon);
            }
        }
        return null;
    }

    public static int loadProviderColor(Context context, String authority, int colorResId) {
        int color = context.getResources().getColor(R.color.misc_primary);
        if (colorResId != 0) {
            if (authority != null) {
                final PackageManager pm = context.getPackageManager();
                final ProviderInfo info = pm.resolveContentProvider(authority, 0);
                if (info != null) {
                    Resources res = null;
                    try {
                        res = pm.getResourcesForApplication(info.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        // No-op let's just return the default
                    }
                    if (res != null) {
                        color = res.getColor(colorResId);
                    }
                }
            }
        }
        return color;
    }

    public static List<PathInfo> reconstructStorageApiFilePath(final String file) {
        final LinkedList<PathInfo> pathList = new LinkedList<PathInfo>();

        StorageApiConsole console = StorageApiConsole.getStorageApiConsoleForPath(file);
        if (console != null) {
            StorageApi storageApi = console.getStorageApi();
            final StorageProviderInfo providerInfo = console.getStorageProviderInfo();
            if (storageApi == null) {
                return null;
            } else if (providerInfo == null || TextUtils.isEmpty(file)) {
                return null;
            }
            final int hashCode = StorageApiConsole.getHashCodeFromProvider(providerInfo);
            final String rootId = StorageApiConsole.constructStorageApiFilePathFromProvider(
                    providerInfo.getRootDocumentId(), hashCode);
            String path = StorageApiConsole.getProviderPathFromFullPath(file);

            do {
                PendingResult<DocumentResult> pendingResult =
                        storageApi.getMetadata(providerInfo, path, false);
                DocumentResult documentResult = pendingResult.await();
                if (documentResult == null) {
                    Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
                    break;
                }
                Document document = documentResult.getDocument();
                String documentPath =
                        StorageApiConsole
                                .constructStorageApiFilePathFromProvider(
                                        document.getId(), hashCode);

                String documentName;
                if (TextUtils.equals(rootId, documentPath)) {
                    documentName = providerInfo.getTitle();
                    path = null;
                } else {
                    documentName = document.getDisplayName();
                    path = document.getParentId();

                }

                PathInfo pathInfo;
                pathInfo = new PathInfo(documentName, documentPath);
                pathList.addFirst(pathInfo);
            } while (!TextUtils.isEmpty(path));
        }
        return pathList;
    }

    /**
     * Method that copies recursively to the destination
     *
     * @param console The console that will be used for this action
     * @param src The source file or folder
     * @param dst The destination file or folder
     * @return boolean If the operation complete successfully
     * @throws ExecutionException If a problem was detected in the operation
     */
    public static boolean copyFromProviderRecursive(final StorageApiConsole console,
            final Document src, final File dst, Program program)
            throws ExecutionException, CancelledOperationException {
        if (src.isDir()) {
            // Create the directory
            if (dst.exists() && !dst.isDirectory()) {
                Log.e(TAG,
                        String.format("Failed to check destination dir: %s", dst)); //$NON-NLS-1$
                throw new ExecutionException("the path exists but is not a folder"); //$NON-NLS-1$
            }
            if (!dst.exists()) {
                if (!dst.mkdir()) {
                    Log.e(TAG, String.format("Failed to create directory: %s", dst)); //$NON-NLS-1$
                    return false;
                }
            }
            List<Document> documents = src.getContents();
            if (documents != null && !documents.isEmpty()) {
                for (Document document : documents) {
                    // Short circuit if we've been cancelled. Show's over :(
                    if (program.isCancelled()) {
                        throw new CancelledOperationException();
                    }

                    if (!copyFromProviderRecursive(console, document,
                            new File(dst, document.getDisplayName()), program)) {
                        return false;
                    }
                }
            }
        } else {
            // Copy the file
            if (!copyFileFromProvider(console, src, dst, program)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that copies recursively to the destination
     *
     * @param console The console that will be used for this action
     * @param src The source file or folder
     * @param dst The destination file or folder (parent directory if the directory doesn't exist)
     * @param name The destination file name
     * @return boolean If the operation complete successfully
     * @throws ExecutionException If a problem was detected in the operation
     */
    public static boolean copyToProviderRecursive(final StorageApiConsole console,
            final File src, final Document dst, final String name, Program program)
            throws ExecutionException, CancelledOperationException {
        if (src.isDirectory()) {
            // Create the directory
            if (dst != null && !dst.isDir()) {
                Log.e(TAG,
                        String.format("Failed to check destination dir: %s", dst)); //$NON-NLS-1$
                throw new ExecutionException("the path exists but is not a folder"); //$NON-NLS-1$
            }
            // Get list of storage provider files
            List<Document> documents = dst.getContents();

            // check directory exists
            Document current = null;
            for (Document document : documents) {
                if (document.getDisplayName().equals(name)) {
                    current = document;
                    break;
                }
            }
            // create directory if it doesn't exit.
            if (current == null) {
                PendingResult<DocumentResult> pendingResult =
                        console.getStorageApi().createFolder(console.getStorageProviderInfo(),
                                dst.getId(), name);
                DocumentResult documentResult = pendingResult.await();
                if (documentResult == null || !documentResult.getStatus().isSuccess()) {
                    Log.e(TAG, String.format("Failed to create directory: %s",
                            src.getName())); //$NON-NLS-1$
                    return false;
                } else {
                    current = documentResult.getDocument();
                    if (current == null) {
                        Log.e(TAG, String.format("Failed to create directory: %s",
                                src.getName())); //$NON-NLS-1$
                        return false;
                    }
                }
            }

            File[] files = src.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    // Short circuit if we've been cancelled. Show's over :(
                    if (program.isCancelled()) {
                        throw new CancelledOperationException();
                    }

                    if (!copyToProviderRecursive(console, files[i], current, files[i].getName(),
                            program)) {
                        return false;
                    }
                }
            }
        } else {
            // Copy the file
            if (!copyFileToProvider(console, src, dst, name, program)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that copies a file
     *
     * @param console The console that will be used for this action
     * @param src The source file
     * @param dst The destination file
     * @return boolean If the operation complete successfully
     */
    private static boolean copyFileFromProvider(final StorageApiConsole console, final Document src,
            final File dst, Program program)
            throws ExecutionException, CancelledOperationException {
        OutputStream outputStream = null;
        try {
            if (!dst.exists() && !dst.createNewFile()) {
                throw new NoSuchFileOrDirectory(dst.getAbsolutePath());
            }
            outputStream = new FileOutputStream(dst);

            PendingResult<DocumentInfoResult> pendingResult =
                    console.getStorageApi().getFile(console.getStorageProviderInfo(),
                            src.getId(), outputStream, null);

            DocumentInfoResult result = pendingResult.await();

            if (result == null || !result.getStatus().isSuccess()) {
                Log.e(TAG, String.format("Failed to move file %s to %d",
                        src.getDisplayName(), dst)); //$NON-NLS-1$
            }
            return result.getStatus().isSuccess();

        } catch (Throwable e) {
            Log.e(TAG,
                    String.format(TAG, "Failed to copy from %s to %d", src, dst), e); //$NON-NLS-1$

            try {
                // delete the destination file if it exists since the operation failed
                if (dst.exists()) {
                    dst.delete();
                }
            } catch (Throwable t) {/**NON BLOCK**/}

            // Check if this error is an out of space exception and throw that specifically.
            // ENOSPC -> Error No Space
            if (e.getCause() instanceof ErrnoException
                    && ((ErrnoException)e.getCause()).errno == OsConstants.ENOSPC) {
                throw new ExecutionException(R.string.msgs_no_disk_space);
            } if (e instanceof CancelledOperationException) {
                // If the user cancelled this operation, let it through.
                throw (CancelledOperationException)e;
            }

            return false;
        } finally {
            if (program.isCancelled()) {
                if (!dst.delete()) {
                    Log.e(TAG, "Failed to delete the dest file: " + dst);
                }
            }
        }
    }

    /**
     * Method that copies a file
     *
     * @param console The console that will be used for this action
     * @param src The source file
     * @param dst The destination file
     * @param name The destination file name
     * @return boolean If the operation complete successfully
     */
    private static boolean copyFileToProvider(final StorageApiConsole console, final File src,
            final Document dst, final String name, Program program)
            throws ExecutionException, CancelledOperationException {
        String fileName = name;
        InputStream inputStream = null;
        try {
            if (!src.exists()) {
                throw new NoSuchFileOrDirectory(src.getAbsolutePath());
            }
            inputStream = new FileInputStream(src);

            // Check destination name
            if (TextUtils.isEmpty(name)) {
                fileName = src.getName();
            }

            // Get mime type
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (TextUtils.isEmpty(mimetype)) {
                mimetype = DEFAULT_MIMETYPE;
            }

            PendingResult<DocumentResult> pendingResult =
                    console.getStorageApi().putFile(console.getStorageProviderInfo(),
                            dst.getId(), fileName, inputStream, mimetype, false, null);

            DocumentResult result = pendingResult.await();

            if (result == null || !result.getStatus().isSuccess()) {
                Log.e(TAG, String.format("Failed to move file %s to %s",
                        src.getAbsoluteFile(), dst.getDisplayName())); //$NON-NLS-1$
            }
            return result.getStatus().isSuccess();

        } catch (Throwable e) {
            Log.e(TAG,
                    String.format(TAG, "Failed to copy from %s to %d", src, dst), e); //$NON-NLS-1$

            // Check if this error is an out of space exception and throw that specifically.
            // ENOSPC -> Error No Space
            if (e.getCause() instanceof ErrnoException
                    && ((ErrnoException)e.getCause()).errno == OsConstants.ENOSPC) {
                throw new ExecutionException(R.string.msgs_no_disk_space);
            } if (e instanceof CancelledOperationException) {
                // If the user cancelled this operation, let it through.
                throw (CancelledOperationException)e;
            }

            return false;
        }
    }
}
