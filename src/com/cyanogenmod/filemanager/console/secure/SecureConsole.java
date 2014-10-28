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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.Handler.Callback;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.commands.MountExecutable;
import com.cyanogenmod.filemanager.commands.secure.Program;
import com.cyanogenmod.filemanager.commands.secure.SecureExecutableFactory;
import com.cyanogenmod.filemanager.console.AuthenticationFailedException;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.console.ReadOnlyFilesystemException;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;

import org.apache.http.auth.AuthenticationException;

import de.schlichtherle.truezip.crypto.raes.RaesAuthenticationException;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.key.CancelledOperation;
import static de.schlichtherle.truezip.fs.FsSyncOption.CLEAR_CACHE;
import static de.schlichtherle.truezip.fs.FsSyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.FORCE_CLOSE_OUTPUT;
import de.schlichtherle.truezip.util.BitField;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A secure implementation of a {@link VirtualMountPointConsole} that uses a
 * secure filesystem backend
 */
public class SecureConsole extends VirtualMountPointConsole {

    public static final String TAG = "SecureConsole";

    /** The singleton TArchiveDetector which enclosure this driver **/
    public static final TArchiveDetector DETECTOR = new TArchiveDetector(
            SecureStorageDriverProvider.SINGLETON, SecureStorageDriverProvider.SINGLETON.get());

    public static String getSecureStorageName() {
        return String.format("storage.%s.%s",
                String.valueOf(UserHandle.myUserId()),
                SecureStorageDriverProvider.SECURE_STORAGE_SCHEME);
    }

    public static TFile getSecureStorageRoot() {
        return new TFile(FileManagerApplication.getInstance().getExternalFilesDir(null),
                getSecureStorageName(), DETECTOR);
    }

    public static URI getSecureStorageRootUri() {
        return new File(FileManagerApplication.getInstance().getExternalFilesDir(null),
                getSecureStorageName()).toURI();
    }

    private static SecureConsole sConsole = null;

    public final Handler mSyncHandler;

    private boolean mIsMounted;
    private boolean mRequiresSync;

    private final int mBufferSize;

    private static final long SYNC_WAIT = 10000L;

    private static final int MSG_SYNC_FS = 0;

    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(1);

    private final Callback mSyncCallback = new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SYNC_FS:
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            sync();
                        }
                    });
                    break;

                default:
                    break;
            }
            return true;
        }
    };

    /**
     * Return an instance of the current console
     * @return
     */
    public static synchronized SecureConsole getInstance(Context ctx, int bufferSize) {
        if (sConsole == null) {
            sConsole = new SecureConsole(ctx, bufferSize);
        }
        return sConsole;
    }

    private final TFile mStorageRoot;
    private final String mStorageName;

    /**
     * Constructor of <code>SecureConsole</code>
     *
     * @param ctx The current context
     */
    private SecureConsole(Context ctx, int bufferSize) {
        super(ctx);
        mIsMounted = false;
        mBufferSize = bufferSize;
        mSyncHandler = new Handler(mSyncCallback);
        mStorageRoot = getSecureStorageRoot();
        mStorageName = getSecureStorageName();

        // Save a copy of the console. This has a unique instance for all the app
        if (sConsole != null) {
            sConsole = this;
        }
    }

    @Override
    public void dealloc() {
        super.dealloc();

        // Synchronize the underlaying storage
        mSyncHandler.removeMessages(MSG_SYNC_FS);
        sync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Secure";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMounted() {
        return mIsMounted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MountPoint> getMountPoints() {
        // This console only has one mountpoint
        List<MountPoint> mountPoints = new ArrayList<MountPoint>();
        String status = mIsMounted ? MountExecutable.READWRITE : MountExecutable.READONLY;
        mountPoints.add(new MountPoint(getVirtualMountPoint().getAbsolutePath(),
                "securestorage", "securestoragefs", status, 0, 0, true, false));
        return mountPoints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public List<DiskUsage> getDiskUsage() {
        // This console only has one mountpoint, and is fully usage
        List<DiskUsage> diskUsage = new ArrayList<DiskUsage>();
        File mp = mStorageRoot.getFile();
        diskUsage.add(new DiskUsage(mp.getAbsolutePath(),
                mp.getTotalSpace(),
                mp.length(),
                mp.getTotalSpace() - mp.length()));
        return diskUsage;
    }

    /**
     * Method that returns if the path belongs to the secure storage
     *
     * @param path The path to check
     * @return
     */
    public boolean isSecureStorageResource(String path) {
        return FileHelper.belongsToDirectory(new File(path), getVirtualMountPoint());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsage getDiskUsage(String path) {
        if (isSecureStorageResource(path)) {
            return getDiskUsage().get(0);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPointName() {
        return "secure";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutableFactory getExecutableFactory() {
        return new SecureExecutableFactory(this);
    }

    /**
     * Method that request a reset of the current password
     */
    public void requestReset(final Context ctx) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                // Unmount the filesystem
                if (mIsMounted) {
                    unmount();
                }
                try {
                    SecureStorageKeyManagerProvider.SINGLETON.reset();

                    // Mount with the new key
                    mount(ctx);

                    // In order to claim a write, we need to be sure that an operation is
                    // done to disk before unmount the device.
                    try {
                        String testName = UUID.randomUUID().toString();
                        TFile test = new TFile(getSecureStorageRoot(), testName);
                        test.createNewFile();
                        test.rm();
                        result = true;
                    } catch (IOException ex) {
                        ExceptionUtil.translateException(ctx, ex);
                    }

                } catch (Exception ex) {
                    ExceptionUtil.translateException(ctx, ex);
                } finally {
                    unmount();
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // Success
                    DialogHelper.showToast(ctx, R.string.msgs_success, Toast.LENGTH_SHORT);
                }
            }

        };
        task.execute();
    }

    /**
     * Method that request a reset of the current password
     */
    @SuppressWarnings("deprecation")
    public void requestDrop(final Context ctx) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                // Unmount the filesystem
                if (mIsMounted) {
                    unmount();
                }
                try {
                    SecureStorageKeyManagerProvider.SINGLETON.drop();

                    // Test mount/unmount
                    mount(ctx);
                    unmount();

                    // Password is valid. Drop the storage
                    mStorageRoot.getFile().delete();

                    // Send an broadcast to notify that the mount state of this filesystem changed
                    Intent intent = new Intent(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
                    intent.putExtra(FileManagerSettings.EXTRA_MOUNTPOINT,
                            getVirtualMountPoint().toString());
                    intent.putExtra(FileManagerSettings.EXTRA_STATUS, MountExecutable.READONLY);
                    getCtx().sendBroadcast(intent);

                    result = true;

                } catch (Exception ex) {
                    ExceptionUtil.translateException(ctx, ex);
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // Success
                    DialogHelper.showToast(ctx, R.string.msgs_success, Toast.LENGTH_SHORT);
                }
            }

        };
        task.execute();
    }

    /**
     * {@inheritDoc}
     */
    public boolean unmount() {
        // Unmount the filesystem and cancel the cached key
        mRequiresSync = true;
        boolean ret = sync();
        if (ret) {
            SecureStorageKeyManagerProvider.SINGLETON.unmount();
        }
        mIsMounted = false;

        // Send an broadcast to notify that the mount state of this filesystem changed
        Intent intent = new Intent(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
        intent.putExtra(FileManagerSettings.EXTRA_MOUNTPOINT,
                getVirtualMountPoint().toString());
        intent.putExtra(FileManagerSettings.EXTRA_STATUS, MountExecutable.READONLY);
        getCtx().sendBroadcast(intent);

        return mIsMounted;
    }

    /**
     * Method that verifies if the current storage is open and mount it
     *
     * @param ctx The current context
     * @throws CancelledOperationException If the operation was cancelled (by the user)
     * @throws AuthenticationException If the secure storage isn't unlocked
     * @throws NoSuchFileOrDirectory If the secure storage isn't accessible
     */
    @SuppressWarnings("deprecation")
    public synchronized void mount(Context ctx)
            throws CancelledOperationException, AuthenticationFailedException,
            NoSuchFileOrDirectory {
        if (!mIsMounted) {
            File root = mStorageRoot.getFile();
            try {
                boolean newStorage = !root.exists();
                mStorageRoot.mount();
                if (newStorage) {
                    // Force a synchronization
                    mRequiresSync = true;
                    sync();
                } else {
                    // Remove any previous cache files (if not sync invoked)
                    clearCache(ctx);
                }

                // The device is mounted
                mIsMounted = true;

                // Send an broadcast to notify that the mount state of this filesystem changed
                Intent intent = new Intent(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
                intent.putExtra(FileManagerSettings.EXTRA_MOUNTPOINT,
                        getVirtualMountPoint().toString());
                intent.putExtra(FileManagerSettings.EXTRA_STATUS, MountExecutable.READWRITE);
                getCtx().sendBroadcast(intent);

            } catch (IOException ex) {
                if (ex.getCause() != null && ex.getCause() instanceof CancelledOperation) {
                    throw new CancelledOperationException();
                }
                if (ex.getCause() != null && ex.getCause() instanceof RaesAuthenticationException) {
                    throw new AuthenticationFailedException(ctx.getString(
                            R.string.secure_storage_unlock_failed));
                }
                Log.e(TAG, String.format("Failed to open secure storage: %s", root, ex));
                throw new NoSuchFileOrDirectory();
            }
        }
    }

    /**
     * Method that returns if the path is the real secure storage file
     *
     * @param path The path to check
     * @return boolean If the path is the secure storage
     */
    public static boolean isSecureStorageDir(String path) {
        Console vc = getVirtualConsoleForPath(path);
        if (vc != null && vc instanceof SecureConsole) {
            return isSecureStorageDir(((SecureConsole) vc).buildRealFile(path));
        }
        return false;
    }

    /**
     * Method that returns if the path is the real secure storage file
     *
     * @param path The path to check
     * @return boolean If the path is the secure storage
     */
    public static boolean isSecureStorageDir(TFile path) {
        return getSecureStorageRoot().equals(path);
    }

    /**
     * Method that build a real file from a virtual path
     *
     * @param path The path from build the real file
     * @return TFile The real file
     */
    public TFile buildRealFile(String path) {
        String real = mStorageRoot.toString();
        String virtual = getVirtualMountPoint().toString();
        String src = path.replace(virtual, real);
        return new TFile(src, DETECTOR);
    }

    /**
     * Method that build a virtual file from a real path
     *
     * @param path The path from build the virtual file
     * @return TFile The virtual file
     */
    public String buildVirtualPath(TFile path) {
        String real = mStorageRoot.toString();
        String virtual = getVirtualMountPoint().toString();
        String dst = path.toString().replace(real, virtual);
        return dst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void execute(Executable executable, Context ctx)
            throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
            OperationTimeoutException, ExecutionException, CommandNotFoundException,
            ReadOnlyFilesystemException, CancelledOperationException,
            AuthenticationFailedException {
        // Check that the program is a secure program
        try {
            Program p = (Program) executable;
            p.setBufferSize(mBufferSize);
        } catch (Throwable e) {
            Log.e(TAG, String.format("Failed to resolve program: %s", //$NON-NLS-1$
                    executable.getClass().toString()), e);
            throw new CommandNotFoundException("executable is not a program", e); //$NON-NLS-1$
        }

        //Auditing program execution
        if (isTrace()) {
            Log.v(TAG, String.format("Executing program: %s", //$NON-NLS-1$
                    executable.getClass().toString()));
        }


        final Program program = (Program) executable;

        // Open storage encryption (if required)
        if (program.requiresOpen()) {
            mount(ctx);
        }

        // Execute the program
        program.setTrace(isTrace());
        if (program.isAsynchronous()) {
            // Execute in a thread
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        program.execute();
                        requestSync(program);
                    } catch (Exception e) {
                        // Program must use onException to communicate exceptions
                        Log.v(TAG,
                                String.format("Async execute failed program: %s", //$NON-NLS-1$
                                program.getClass().toString()));
                    }
                }
            };
            t.start();

        } else {
            // Synchronous execution
            program.execute();
            requestSync(program);
        }
    }

    /**
     * Request a synchronization of the underlying filesystem
     *
     * @param program The last called program
     */
    private void requestSync(Program program) {
        if (program.requiresSync()) {
            mRequiresSync = true;
        }

        // There is some changes to synchronize?
        if (mRequiresSync) {
            Boolean defaultValue = ((Boolean)FileManagerSettings.
                    SETTINGS_SECURE_STORAGE_DELAYED_SYNC.getDefaultValue());
            Boolean delayedSync =
                    Boolean.valueOf(
                            Preferences.getSharedPreferences().getBoolean(
                                FileManagerSettings.SETTINGS_SECURE_STORAGE_DELAYED_SYNC.getId(),
                                defaultValue.booleanValue()));
            mSyncHandler.removeMessages(MSG_SYNC_FS);
            if (delayedSync) {
                // Request a sync in 30 seconds, if users is not doing any operation
                mSyncHandler.sendEmptyMessageDelayed(MSG_SYNC_FS, SYNC_WAIT);
            } else {
                // Do the synchronization now
                mSyncHandler.sendEmptyMessage(MSG_SYNC_FS);
            }
        }
    }

    /**
     * Synchronize the underlying filesystem
     *
     * @retun boolean If the unmount success
     */
    public synchronized boolean sync() {
        if (mRequiresSync) {
            Log.i(TAG, "Syncing underlaying storage");
            mRequiresSync = false;
            // Sync the underlying storage
            try {
                TVFS.sync(mStorageRoot,
                        BitField.of(CLEAR_CACHE)
                                .set(FORCE_CLOSE_INPUT, true)
                                .set(FORCE_CLOSE_OUTPUT, true));
                return true;
            } catch (IOException e) {
                Log.e(TAG, String.format("Failed to sync secure storage: %s", mStorageRoot, e));
                return false;
            }
        }
        return true;
    }

    /**
     * Method that clear the cache
     *
     * @param ctx The current context
     */
    private void clearCache(Context ctx) {
        File filesDir = ctx.getExternalFilesDir(null);
        File[] cacheFiles = filesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(mStorageName)
                        && filename.endsWith(".tmp");
            }
        });
        for (File cacheFile : cacheFiles) {
            cacheFile.delete();
        }
    }
}
