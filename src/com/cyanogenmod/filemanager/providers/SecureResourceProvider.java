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

package com.cyanogenmod.filemanager.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RegularFile;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A {@link ContentProvider} to allow access secure filesystems.
 */
public final class SecureResourceProvider extends ContentProvider {

    private static final String TAG = "SecureResourceProvider";

    public static final String AUTHORITY =
            "com.cyanogenmod.filemanager.providers.resources";

    private static final String CONTENT_AUTHORITY = "content://" + AUTHORITY;

    private static final String COLUMS_ID = "auth_id";
    private static final String COLUMS_NAME = OpenableColumns.DISPLAY_NAME;
    private static final String COLUMS_SIZE = OpenableColumns.SIZE;

    private static final String[] COLUMN_PROJECTION = {
        COLUMS_ID, COLUMS_NAME, COLUMS_SIZE
    };

    public static class AuthorizationResource {
        public final RegularFile mFile;
        private String mPackage;

        private AuthorizationResource(RegularFile file) {
            mFile = file;
            mPackage = null;
        }
    }

    /**
     * An implementation of an {@code AsyncResultListener}
     */
    private static class AsyncReader implements AsyncResultListener {

        private final CancellationSignal mSignal;
        private final ParcelFileDescriptor mFdIn;
        private final ParcelFileDescriptor mFdOut;
        private final OutputStream mOut;

        public AsyncReader(ParcelFileDescriptor fdIn, ParcelFileDescriptor fdOut,
                CancellationSignal signal) throws IOException {
            super();
            mFdIn = fdIn;
            mFdOut = fdOut;
            mOut = new ParcelFileDescriptor.AutoCloseOutputStream(fdOut);
            mSignal = signal;
        }

        @Override
        public void onAsyncStart() {
            // Ignore
        }

        @Override
        public void onAsyncEnd(boolean cancelled) {
            // Ignore
        }

        @Override
        public void onAsyncExitCode(int exitCode) {
            close();
        }

        @Override
        public void onPartialResult(Object result) {
            try {
                if (result == null) return;
                byte[] partial = (byte[])result;
                mOut.write(partial);
                mOut.flush();
            } catch (Exception ex) {
                Log.w(TAG, "Failed to parse partial result data", ex);
                closeWithError("Failed to parse partial result data: " + ex.getMessage());
                if (mSignal != null) {
                    mSignal.cancel();
                }
            }
        }

        @Override
        public void onException(Exception cause) {
            Log.w(TAG, "Got exception while reading data", cause);
            closeWithError("Got exception while reading data: " + cause.getMessage());
            if (mSignal != null) {
                mSignal.cancel();
            }
        }

        private void close() {
            try {
                mOut.close();
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdOut.close();
            } catch (IOException ex) {
                // Ignore
            }
        }

        private void closeWithError(String msg) {
            try {
                mOut.close();
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdOut.closeWithError(msg);
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdIn.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private static final Callback CLEAR_AUTH_CALLBACK = new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLEAR_AUTHORIZATIONS:
                    // Remove authorization
                    UUID uuid = UUID.fromString(msg.getData().getString(EXTRA_AUTH_ID));
                    AUTHORIZATIONS.remove(uuid);
                    break;

                default:
                    break;
            }
            return true;
        }
    };

    private static final long MAX_AUTH_LIVE_TIME = 20000L;
    private static final int MSG_CLEAR_AUTHORIZATIONS = 1;
    private static final String EXTRA_AUTH_ID = "auth_id";
    private static final Handler CLEAR_AUTH_HANDLER = new Handler(CLEAR_AUTH_CALLBACK);

    private static Map<UUID, AuthorizationResource> AUTHORIZATIONS =
            (Map<UUID, AuthorizationResource>) Collections.synchronizedMap(
                    new HashMap<UUID, AuthorizationResource>());

    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(1);

    /**
     * This method creates an authorization uri for a file, but this not grants
     * access to this file. Callers must explicitly call to grantAuthorization in
     * order to set the package associated with this grant
     *
     * @param fso The file to authorize
     * @return Uri The uri of this authorized resource
     */
    public static Uri createAuthorizationUri(RegularFile file) {
        // Generate a new authorization for the filesystem
        UUID uuid = null;
        do {
            uuid = UUID.randomUUID();
            if (!AUTHORIZATIONS.containsKey(uuid)) {
                AuthorizationResource resource = new AuthorizationResource(file);
                AUTHORIZATIONS.put(uuid, resource);
                break;
            }
        } while(true);

        // Post a message to clear authorization after an interval of time
        Message msg = Message.obtain(CLEAR_AUTH_HANDLER, MSG_CLEAR_AUTHORIZATIONS);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_AUTH_ID, uuid.toString());
        msg.setData(bundle);
        CLEAR_AUTH_HANDLER.sendMessageDelayed(msg, MAX_AUTH_LIVE_TIME);
        return createAuthorizationUri(uuid);
    }

    /**
     * Method that register the {@link FileSystemObject} that allow external apps to access
     * private files. An authorization MUST be explicit done by this app. Third party apps
     * can register
     *
     * @param uri The authorized uri
     * @param pkg The package to authorize
     */
    public static void grantAuthorizationUri(Uri uri, String pkg) {
        // Check that exists that authorization
        AuthorizationResource authResource = getAuthorizacionResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Check that the authorization doesn't was granted before
        if (authResource.mPackage != null) {
            throw new SecurityException("The authorization was granted before");
        }

        // And now grant the access
        Log.i(TAG, "grant authorization of uri " + uri.toString() + " to package " + pkg);
        authResource.mPackage = pkg;
    }

    /**
     * Method that unregister un-granted authorizations.
     *
     * @param uri The authorized uri
     */
    public static AuthorizationResource revertAuthorization(Uri uri) {
        // Check that exists that authorization
        AuthorizationResource authResource = getAuthorizacionResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Check that the authorization was granted before
        if (authResource.mPackage != null) {
            throw new SecurityException("The authorization was granted before");
        }

        // And now remove the un-granted authorization
        UUID uuid = UUID.fromString(uri.getLastPathSegment());
        return AUTHORIZATIONS.remove(uuid);
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // Retrieve the authorization
        AuthorizationResource authResource = getAuthorizacionResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Create an in-memory cursor
        String[] cols = new String[COLUMN_PROJECTION.length];
        Object[] values = new Object[COLUMN_PROJECTION.length];
        for (int i = 0; i < COLUMN_PROJECTION.length; i++) {
            cols[i] = COLUMN_PROJECTION[i];
            switch (i) {
                case 0:
                    values[i] = uri.getLastPathSegment();
                    break;
                case 1:
                    values[i] = authResource.mFile.getName();
                    break;
                case 2:
                    values[i] = authResource.mFile.getSize();
                    break;

                default:
                    break;
            }
        }

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        // Retrieve the authorization
        AuthorizationResource authResource = getAuthorizacionResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        return MimeTypeHelper.getMimeType(getContext(), authResource.mFile);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new SecurityException("Insert is not allowed");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new SecurityException("Delete is not allowed");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new SecurityException("Update is not allowed");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return this.openFile(uri, mode, null);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode, final CancellationSignal signal)
            throws FileNotFoundException {
        // Retrieve the authorization
        final AuthorizationResource authResource = getAuthorizacionResourceForUri(uri);
        if (authResource == null) {
            throw new SecurityException("Authorization not exists");
        }

        // Check that the request comes from the authorized package
        String[] pkgs = getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (pkgs == null) {
            throw new SecurityException("Authorization denied. No packages");
        }
        boolean isPackageAuthorized = false;
        for (String pkg : pkgs) {
            if (pkg.equals(authResource.mPackage)) {
                isPackageAuthorized = true;
                break;
            }
        }
        if (!isPackageAuthorized) {
            throw new SecurityException("Authorization denied. Package mismatch");
        }

        // Open a pipe between the package and this provider
        try {
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createReliablePipe();
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AsyncReader reader = new AsyncReader(fds[0], fds[1], signal);
                        CommandHelper.read(getContext(), authResource.mFile.getFullPath(),
                                reader, null);
                    } catch (Exception e) {
                        Log.w(TAG, "Failure writing pipe. ", e);
                    }
                }
            });
            return fds[0];

        } catch (IOException ex) {
            Log.w(TAG, "Failed to create pipe descriptors. ", ex);
        }
        return null;
    }

    /**
     * Method that returns an authorization for the passed Uri.
     *
     * @param uri The uri to check
     * @param revoke Whether revoke the grant
     * @return AuthorizationResource The authorization resource or null if not there is not
     * authorization
     */
    private static AuthorizationResource getAuthorizacionResourceForUri(Uri uri) {
        UUID uuid = UUID.fromString(uri.getLastPathSegment());
        if (uuid == null || !AUTHORIZATIONS.containsKey(uuid)) {
            return null;
        }
        return AUTHORIZATIONS.get(uuid);
    }

    /**
     * Method that returns an authorization URI from the authorization UUID
     *
     * @param uuid The UUID of the authorization
     * @return Uri The authorization Uri
     */
    private static Uri createAuthorizationUri(UUID uuid) {
        return Uri.withAppendedPath(Uri.parse(CONTENT_AUTHORITY),
                uuid.toString());
    }
}
