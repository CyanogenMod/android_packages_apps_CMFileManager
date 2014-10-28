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

package com.cyanogenmod.filemanager.commands.shell;

import android.util.Log;

import com.cyanogenmod.filemanager.commands.SyncResultExecutable;
import com.cyanogenmod.filemanager.commands.WritableExecutable;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.ReadOnlyFilesystemException;

/**
 * An abstract class that represents a command to wrap others commands,
 * like <code>sh</code> or <code>su</code> commands.
 */
public abstract class Shell extends Command {

    private int mPid;

    private final static String TAG = "Shell"; //$NON-NLS-1$

    /**
     * @Constructor of <code>Shell</code>
     *
     * @param id The resource identifier of the command
     * @param args Arguments of the command (will be formatted with the arguments from
     * the command definition)
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public Shell(String id, String... args) throws InvalidCommandDefinitionException {
        super(id, args);
        this.mPid = -1;
    }

    /**
     * Method that returns the process identifier of the console
     *
     * @return int The process identifier
     */
    public final int getPid() {
        return this.mPid;
    }

    /**
     * Method that sets the process identifier of the console
     *
     * @param pid The process identifier
     */
    public final void setPid(int pid) {
        this.mPid = pid;
    }

    /**
     * Method that returns the desired runtime environment of the console
     *
     * @return The environment
     */
    public String[] getEnvironment() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {
        //Command not found
        if (exitCode == 127) {
            Log.w(TAG, String.format(
                        "CommandNotFound: %s %s", //$NON-NLS-1$
                        getCommand(),
                        getArguments()));
            throw new CommandNotFoundException(getId());
        }
        //No exit code
        if (exitCode == 255) {
            throw new ExecutionException(getId());
        }
    }

    /**
     * Method that checks if the standard errors has exceptions.
     *
     * @param program The program
     * @param exitCode Program exit code
     * @param err Standard Error buffer
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws CommandNotFoundException If the command was not found
     * @throws ExecutionException If the another exception is detected in the standard error
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @hide
     */
    @SuppressWarnings("static-method")
    public void checkStdErr(Program program, int exitCode, String err)
            throws InsufficientPermissionsException, NoSuchFileOrDirectory,
            CommandNotFoundException, ExecutionException, ReadOnlyFilesystemException {
        //Check problems in the standard error
        if (exitCode != 0 && err.indexOf("No such file or directory") != -1) { //$NON-NLS-1$
            throw new NoSuchFileOrDirectory();
        }
        //Normally usage code is generated for invalid commands, but let's assume
        //that the invalid command is generated for and error caused for non
        //existing directory
        if (err.indexOf("Usage:") != -1) { //$NON-NLS-1$
            throw new NoSuchFileOrDirectory();
        }
        if (err.indexOf("Permission denied") != -1) { //$NON-NLS-1$
            if (program instanceof SyncResultExecutable) {
                throw new InsufficientPermissionsException((SyncResultExecutable)program);
            }
            throw new InsufficientPermissionsException();
        }
        if (exitCode != 0 && err.indexOf("Operation not permitted") != -1) { //$NON-NLS-1$
            if (program instanceof SyncResultExecutable) {
                throw new InsufficientPermissionsException((SyncResultExecutable)program);
            }
            throw new InsufficientPermissionsException();
        }
        if (err.indexOf("Read-only file system") != -1) { //$NON-NLS-1$
            if (program instanceof WritableExecutable) {
                // This error could be caused by dst or src. No matter which. Use dst.
                throw new ReadOnlyFilesystemException(
                        ((WritableExecutable)program).getDstWritableMountPoint());
            }
            throw new ExecutionException("Read-only file system");  //$NON-NLS-1$
        }
    }

}
