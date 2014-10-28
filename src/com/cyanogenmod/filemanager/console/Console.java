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
package com.cyanogenmod.filemanager.console;

import android.content.Context;

import com.cyanogenmod.filemanager.commands.AsyncResultExecutable;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.model.Identity;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;

/**
 * This class represents a class for executing commands in the operating system layer,
 * being the base for all type of consoles (shell, java, ...).
 */
public abstract class Console
    implements AsyncResultExecutable.OnEndListener, AsyncResultExecutable.OnCancelListener {

    private boolean mTrace;

    /**
     * Constructor of <code>Console</code>
     */
    public Console() {
        super();

        // Get the current trace value
        reloadTrace();
    }

    /**
     * Method that return if the console has to trace his operations
     *
     * @return boolean If the console has to trace
     */
    public final boolean isTrace() {
        return this.mTrace;
    }

    /**
     * Method that reload the status of trace setting
     */
    public final void reloadTrace() {
        this.mTrace = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_SHOW_TRACES.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_SHOW_TRACES.getDefaultValue()).booleanValue());
    }

    /**
     * Method that returns the identity of the console (the current user).
     *
     * @return Identity The current identity of the console
     */
    public abstract Identity getIdentity();

    /**
     * Method that allocates the console.
     *
     * @throws ConsoleAllocException If the console can't be allocated
     */
    public abstract void alloc() throws ConsoleAllocException;

    /**
     * Method that deallocates the actual console.
     */
    public abstract void dealloc();

   /**
    * Method that reallocates the console. This method drops the actual console
    * and create a new one exactly as the current.
    *
    * @throws ConsoleAllocException If the console can't be reallocated
    */
   public abstract void realloc() throws ConsoleAllocException;

   /**
    * Method that returns if the console has root privileged.
    *
    * @return boolean Indicates if the console has root privileged
    */
   public abstract boolean isPrivileged();

   /**
    * Method that returns if the console is active and allocated.
    *
    * @return boolean Indicates if the console is active and allocated
    */
   public abstract boolean isActive();

   /**
    * Method that retrieves the {@link ExecutableFactory} associated with the {@link Console}.
    *
    * @return ExecutableFactory The execution program factory
    */
   public abstract ExecutableFactory getExecutableFactory();

   /**
    * Method for execute a command in the operating system layer.
    *
    * @param executable The executable command to be executed
    * @param ctx The current context
    * @throws ConsoleAllocException If the console is not allocated
    * @throws InsufficientPermissionsException If an operation requires elevated permissions
    * @throws NoSuchFileOrDirectory If the file or directory was not found
    * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
    * @throws CommandNotFoundException If the executable program was not found
    * @throws ExecutionException If the operation returns a invalid exit code
    * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
    * @throws CancelledOperationException If the operation was cancelled
    * @throws AuthenticationFailedException If the operation failed because an
    * authentication failure
 * @throws AuthenticationFailedException
    */
   public abstract void execute(final Executable executable, final Context ctx)
           throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
           OperationTimeoutException, ExecutionException, CommandNotFoundException,
           ReadOnlyFilesystemException, CancelledOperationException, AuthenticationFailedException;

}
