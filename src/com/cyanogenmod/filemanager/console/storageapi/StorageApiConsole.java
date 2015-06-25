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
import android.util.Log;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.commands.storageapi.Program;
import com.cyanogenmod.filemanager.commands.storageapi.StorageApiExecutableFactory;
import com.cyanogenmod.filemanager.console.*;

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

    private static List<StorageApiConsole> sStorageApiConsoles;
    private final int mBufferSize;
    private Program mActiveProgram;

    /**
     * Constructor of <code>VirtualConsole</code>
     *
     * @param ctx The current context
     */
    public StorageApiConsole(Context ctx, int bufferSize) {
        super(ctx);
        mBufferSize = bufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "StorageApi";
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
            storageApi = StorageApi.newInstance(context);
        }
        if (sStorageApiConsoles == null) {
            sStorageApiConsoles = new ArrayList<StorageApiConsole>();
        }

        int bufferSize = context.getResources().getInteger(R.integer.buffer_size);

        // Register new storage api console
        StorageApiConsole console =
                new StorageApiConsole(context, bufferSize);
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
        File file = new File(path);
        for (StorageApiConsole console : sStorageApiConsoles) {
            // TODO: Implement this
            //if (FileHelper.belongsToDirectory(file, console.getMountPoint())) {
            //    return console;
            //}
        }
        return null;
    }
}
