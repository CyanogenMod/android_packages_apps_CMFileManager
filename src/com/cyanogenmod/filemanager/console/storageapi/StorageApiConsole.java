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

package com.cyanogenmod.filemanager.console.storageapi;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.commands.storageapi.Program;
import com.cyanogenmod.filemanager.commands.storageapi.StorageApiExecutableFactory;
import com.cyanogenmod.filemanager.console.AuthenticationFailedException;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.console.ReadOnlyFilesystemException;
import com.cyanogenmod.filemanager.console.VirtualConsole;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a {@link VirtualConsole} based on a storage API implementation.<br/>
 * <br/>
 * This console is a non-privileged console an many of the functionality is not implemented
 * because can't be obtain from storage api.
 */
public class StorageApiConsole extends VirtualConsole {
    private static final String TAG = StorageApiConsole.class.getSimpleName();
    private static final String PATH_SEPARATOR = "://";

    private static List<StorageApiConsole> sStorageApiConsoles;

    private final StorageApi mStorageApi;
    private final StorageProviderInfo mProviderInfo;
    private final int mBufferSize;
    private Program mActiveProgram;

    /**
     * Constructor of <code>VirtualConsole</code>
     *
     * @param ctx The current context
     */
    public StorageApiConsole(Context ctx, StorageApi storageApi, StorageProviderInfo providerInfo,
            int bufferSize) {
        super(ctx);
        mStorageApi = storageApi;
        mProviderInfo = providerInfo;
        mBufferSize = bufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "StorageApi";
    }

    /*
     * Get StorageApi associated with this console.
     */
    public StorageApi getStorageApi() {
        return mStorageApi;
    }

    /*
     * Get StorageProviderInfo associated with this console.
     */
    public StorageProviderInfo getStorageProviderInfo() {
        return mProviderInfo;
    }

    /**
     * Method that retrieves the {@link ExecutableFactory} associated with the StorageApiConsole.
     *
     * @return ExecutableFactory The execution program factory
     */
    @Override
    public ExecutableFactory getExecutableFactory() {
        return new StorageApiExecutableFactory(this);
    }

    /**
     * Method for execute a command in the operating system layer.
     *
     * @param executable The executable command to be executed
     * @param ctx        The current context
     * @throws ConsoleAllocException            If the console is not allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws NoSuchFileOrDirectory            If the file or directory was not found
     * @throws OperationTimeoutException        If the operation exceeded the maximum time of wait
     * @throws CommandNotFoundException         If the executable program was not found
     * @throws ExecutionException               If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException      If the operation writes in a read-only filesystem
     * @throws CancelledOperationException      If the operation was cancelled
     * @throws AuthenticationFailedException    If the operation failed because an
     *                                          authentication failure
     */
    @Override
    public synchronized void execute(Executable executable, Context ctx)
            throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
            OperationTimeoutException, ExecutionException, CommandNotFoundException,
            ReadOnlyFilesystemException, CancelledOperationException,
            AuthenticationFailedException {
        // Check that the program is a storage api program
        try {
            Program p = (Program)executable;
            p.isTrace();
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

        // Execute the program
        final Program program = (Program)executable;
        mActiveProgram = program;
        program.setTrace(isTrace());
        program.setBufferSize(this.mBufferSize);
        if (program.isAsynchronous()) {
            // Execute in a thread
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        program.execute();
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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCancel() {
        mActiveProgram.requestCancel();
        return true;
    }

    public int getProviderHash() {
        return StorageApiConsole.getHashCodeFromProvider(mProviderInfo);
    }

    /**
     * Method that register a storage api console. This method should
     * be called only once per storage api on instantiation.
     *
     * @param context The current context
     */
    public static StorageApiConsole registerStorageApiConsole(Context context,
            StorageApi storageApi, StorageProviderInfo providerInfo) {
        if (providerInfo == null) {
            return null;
        }
        if (storageApi == null) {
            storageApi = StorageApi.getInstance();
        }
        if (sStorageApiConsoles == null) {
            sStorageApiConsoles = new ArrayList<StorageApiConsole>();
        }

        int bufferSize = context.getResources().getInteger(R.integer.buffer_size);

        // Register new storage api console
        StorageApiConsole console =
                new StorageApiConsole(context, storageApi, providerInfo, bufferSize);
        sStorageApiConsoles.add(console);
        return console;
    }

    /**
     * Method that returns the virtual console for the path or null if the path
     * is not a virtual filesystem
     *
     * @param path the path to check
     * @return VirtualMountPointConsole The found console
     */
    public static StorageApiConsole getStorageApiConsoleForPath(String path) {
        int hashCode = getHashCodeFromStorageApiPath(path);

        if (hashCode == -1) {
            return null;
        }

        return getConsoleForHashCode(hashCode);
    }

    /**
     * Returns a hash code for this Storage Provider
     * @param storageProviderInfo
     * @return
     */
    public static int getHashCodeFromProvider(StorageProviderInfo storageProviderInfo) {
        String rootTitle = String.format("%s %s", storageProviderInfo.getTitle(),
                storageProviderInfo.getSummary());

        return rootTitle.hashCode();
    }

    /**
     * Returns the StorageApiConsole from sStorageApiConsoles that matches this hash
     * @param hashCode to match against to get the correct StorageApiConsole
     */
    public static StorageApiConsole getConsoleForHashCode (int hashCode) {
        for (StorageApiConsole console : sStorageApiConsoles) {
            if (console.getProviderHash() == hashCode) {
                return console;
            }
        }

        return null;
    }

    /**
     * All paths for StorageApi Providers are prefixed with their hash and the
     * {@link StorageApiConsole#PATH_SEPARATOR}. This helper method constructs this prefix.
     * @param hashCode
     */
    public static String constructStorageApiPrefixFromHash(int hashCode) {
        return Integer.valueOf(hashCode) + PATH_SEPARATOR;
    }

    /**
     * All paths for StorageApi Providers are prefixed with their hash and the
     * {@link StorageApiConsole#PATH_SEPARATOR}. This helper method constructs a full path
     * with the Provider prefix and the Provider relative path.
     * @param path
     * @param hashCode
     */
    public static String constructStorageApiFilePathFromProvider(String path, int hashCode) {
        return Integer.valueOf(hashCode) + PATH_SEPARATOR + path;
    }

    /**
     * Helper method to return the hash code from a full path containing a Provider prefix
     * @param fullPath
     */
    public static int getHashCodeFromStorageApiPath(String fullPath) {
        if (fullPath.contains(PATH_SEPARATOR)) {
            return Integer.valueOf(fullPath.substring(0, fullPath.indexOf(PATH_SEPARATOR)));
        } else {
            return -1;
        }
    }

    /**
     * Helper method to return the Provider relative path from the full path
     * @param fullPath
     */
    public static String getProviderPathFromFullPath(String fullPath) {
        if (!TextUtils.isEmpty(fullPath) && fullPath.contains(PATH_SEPARATOR)) {
            return fullPath.substring(fullPath.indexOf(PATH_SEPARATOR) + PATH_SEPARATOR.length());
        } else {
            return null;
        }
    }

    public static String getProviderNameFromFullPath(String fullPath) {
        String name = null;
        StorageApiConsole storageApiConsole = getStorageApiConsoleForPath(fullPath);
        if (storageApiConsole != null) {
            name = storageApiConsole.getStorageProviderInfo().getTitle();
        }
        return name;
    }
}
