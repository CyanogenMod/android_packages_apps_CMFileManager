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

package com.cyanogenmod.filemanager.util;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.provider.MediaStore.Files;

import android.provider.MediaStore;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ChangeOwnerExecutable;
import com.cyanogenmod.filemanager.commands.ChangePermissionsExecutable;
import com.cyanogenmod.filemanager.commands.ChecksumExecutable;
import com.cyanogenmod.filemanager.commands.CompressExecutable;
import com.cyanogenmod.filemanager.commands.ConcurrentAsyncResultListener;
import com.cyanogenmod.filemanager.commands.CopyExecutable;
import com.cyanogenmod.filemanager.commands.CreateDirExecutable;
import com.cyanogenmod.filemanager.commands.CreateFileExecutable;
import com.cyanogenmod.filemanager.commands.DeleteDirExecutable;
import com.cyanogenmod.filemanager.commands.DeleteFileExecutable;
import com.cyanogenmod.filemanager.commands.DiskUsageExecutable;
import com.cyanogenmod.filemanager.commands.EchoExecutable;
import com.cyanogenmod.filemanager.commands.ExecExecutable;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.FindExecutable;
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.commands.GroupsExecutable;
import com.cyanogenmod.filemanager.commands.IdentityExecutable;
import com.cyanogenmod.filemanager.commands.LinkExecutable;
import com.cyanogenmod.filemanager.commands.ListExecutable;
import com.cyanogenmod.filemanager.commands.MountExecutable;
import com.cyanogenmod.filemanager.commands.MountPointInfoExecutable;
import com.cyanogenmod.filemanager.commands.MoveExecutable;
import com.cyanogenmod.filemanager.commands.ParentDirExecutable;
import com.cyanogenmod.filemanager.commands.ProcessIdExecutable;
import com.cyanogenmod.filemanager.commands.QuickFolderSearchExecutable;
import com.cyanogenmod.filemanager.commands.ReadExecutable;
import com.cyanogenmod.filemanager.commands.ResolveLinkExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.commands.SendSignalExecutable;
import com.cyanogenmod.filemanager.commands.SyncResultExecutable;
import com.cyanogenmod.filemanager.commands.UncompressExecutable;
import com.cyanogenmod.filemanager.commands.WritableExecutable;
import com.cyanogenmod.filemanager.commands.WriteExecutable;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.AuthenticationFailedException;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.console.ReadOnlyFilesystemException;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.FolderUsage;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.Identity;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.SearchResult;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.preferences.CompressionMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.util.MediaHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A helper class with useful methods for deal with commands.
 */
public final class CommandHelper {

    /**
     * A wrapper class for asynchronous operations that need restore the filesystem
     * after the operation.
     */
    private static class UnmountAsyncResultListener implements AsyncResultListener {

        Context mCtx;
        AsyncResultListener mRef;
        boolean mUnmount = false;
        Console mConsole;
        MountPoint mMountPoint;

        /**
         * Constructor of <code>UnmountAsyncResultListener</code>
         */
        public UnmountAsyncResultListener() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncStart() {
            if (this.mRef != null) {
                this.mRef.onAsyncStart();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncEnd(boolean cancelled) {
            if (this.mRef != null) {
                this.mRef.onAsyncEnd(cancelled);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncExitCode(int exitCode) {
            if (this.mRef != null) {
                this.mRef.onAsyncExitCode(exitCode);
            }

            // Now if that the process has finished check if the operation
            // requires to unmount the filesystem
            if (this.mUnmount && this.mConsole != null &&
                this.mMountPoint != null && this.mCtx != null) {
                // Run in background because the console is still executing
                // the command
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            MountExecutable unmountExecutable =
                                    UnmountAsyncResultListener.this.mConsole.
                                        getExecutableFactory().newCreator().
                                            createMountExecutable(
                                                    UnmountAsyncResultListener.this.mMountPoint,
                                                    false);
                            UnmountAsyncResultListener.this.mConsole.execute(
                                    unmountExecutable, mCtx);
                        } catch (Exception e) {
                            // Capture the exception but not show to the user
                            ExceptionUtil.translateException(
                                    UnmountAsyncResultListener.this.mCtx, e, true, false);
                        }
                    }
                };
                t.start();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPartialResult(Object result) {
            if (this.mRef != null) {
                this.mRef.onPartialResult(result);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onException(Exception cause) {
            if (this.mRef != null) {
                this.mRef.onException(cause);
            }
        }
    }

    /**
     * Constructor of <code>CommandHelper</code>.
     */
    private CommandHelper() {
        super();
    }

    /**
     * Method that changes the owner of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object to change its permissions
     * @param user The new user owner of the file system object
     * @param group The new group owner of the file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see ChangeOwnerExecutable
     */
    public static boolean changeOwner(
            Context context, String src, User user, Group group, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException, ExecutionException,
            InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);
        ChangeOwnerExecutable executable =
                c.getExecutableFactory().
                    newCreator().createChangeOwnerExecutable(src, user, group);
        writableExecute(context, executable, c);
        return executable.getResult().booleanValue();
    }

    /**
     * Method that changes the permissions of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object to change its permissions
     * @param permissions The new permissions of the file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see ChangePermissionsExecutable
     */
    public static boolean changePermissions(
            Context context, String src, Permissions permissions, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);
        ChangePermissionsExecutable executable =
                c.getExecutableFactory().newCreator().
                    createChangePermissionsExecutable(src, permissions);
        writableExecute(context, executable, c);
        return executable.getResult().booleanValue();
    }

    /**
     * Method that creates a directory.
     *
     * @param context The current context (needed if console == null)
     * @param directory The directory to create
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CreateDirExecutable
     */
    public static boolean createDirectory(Context context, String directory, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, directory);
        CreateDirExecutable executable =
                c.getExecutableFactory().newCreator().createCreateDirectoryExecutable(directory);
        writableExecute(context, executable, c);
        return executable.getResult().booleanValue();
    }

    /**
     * Method that creates a file.
     *
     * @param context The current context (needed if console == null)
     * @param file The file to create
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CreateFileExecutable
     */
    public static boolean createFile(Context context, String file, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, file);
        CreateFileExecutable executable =
                c.getExecutableFactory().newCreator().createCreateFileExecutable(file);
        writableExecute(context, executable, c);

        // Do media scan
        MediaScannerConnection.scanFile(context, new String[]{
                MediaHelper.normalizeMediaPath(file)}, null, null);

        return executable.getResult().booleanValue();
    }

    /**
     * Method that deletes a directory.
     *
     * @param context The current context (needed if console == null)
     * @param directory The directory to delete
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see DeleteDirExecutable
     */
    public static boolean deleteDirectory(Context context, String directory, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, directory);
        DeleteDirExecutable executable =
                c.getExecutableFactory().newCreator().createDeleteDirExecutable(directory);
        writableExecute(context, executable, c);

        // Do media scan
        File parent = new File(directory).getParentFile();
        if (parent != null) {
            MediaScannerConnection.scanFile(context, new String[]{
                    MediaHelper.normalizeMediaPath(parent.getAbsolutePath())}, null, null);
        }

        return executable.getResult().booleanValue();
    }

    /**
     * Method that deletes a file.
     *
     * @param context The current context (needed if console == null)
     * @param file The file to delete
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see DeleteFileExecutable
     */
    public static boolean deleteFile(Context context, String file, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, file);
        DeleteFileExecutable executable =
                c.getExecutableFactory().newCreator().createDeleteFileExecutable(file);
        writableExecute(context, executable, c);

        // Remove from media scanner
        removeFromMediaStore(context, file);

        return executable.getResult().booleanValue();
    }

    /**
     * Method that resolves a symlink to its real file system object.
     *
     * @param context The current context (needed if console == null)
     * @param symlink The link to be resolved
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return String The resolved link
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ResolveLinkExecutable
     */
    public static FileSystemObject resolveSymlink(Context context, String symlink, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        ResolveLinkExecutable executable =
                c.getExecutableFactory().newCreator().createResolveLinkExecutable(symlink);
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that retrieves the information of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return FileSystemObject The file system object reference
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ListExecutable
     */
    public static FileSystemObject getFileInfo(Context context, String src, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        return getFileInfo(context, src, true, console);
    }

    /**
     * Method that retrieves the information of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object
     * @param followSymlinks It should be follow the symlinks
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return FileSystemObject The file system object reference
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ListExecutable
     */
    public static FileSystemObject getFileInfo(
            Context context, String src, boolean followSymlinks, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, src);
        ListExecutable executable =
                c.getExecutableFactory().
                    newCreator().createFileInfoExecutable(src, followSymlinks);
        execute(context, executable, c);
        List<FileSystemObject> files = executable.getResult();
        if (files != null && files.size() > 0) {
            // Resolve symlinks prior to return the object
            if (followSymlinks) {
                FileHelper.resolveSymlinks(context, files);
            }
            return files.get(0);
        }
        return null;
    }

    /**
     * Method that retrieves the information of the groups of the current user.
     *
     * @param context The current context (needed if console == null)
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<Group> The groups of the current user
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see GroupsExecutable
     */
    public static List<Group> getGroups(Context context, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        GroupsExecutable executable =
                c.getExecutableFactory().newCreator().createGroupsExecutable();
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that retrieves the identity of the current user.
     *
     * @param context The current context (needed if console == null)
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return Identity The identity of the current user
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see IdentityExecutable
     */
   public static Identity getIdentity(Context context, Console console)
           throws FileNotFoundException, IOException, ConsoleAllocException,
           NoSuchFileOrDirectory, InsufficientPermissionsException,
           CommandNotFoundException, OperationTimeoutException,
           ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
       Console c = ensureConsole(context, console);
       IdentityExecutable executable =
               c.getExecutableFactory().newCreator().createIdentityExecutable();
       execute(context, executable, c);
       return executable.getResult();
   }

   /**
    * Method that creates a symlink of an other file system object.
    *
    * @param context The current context (needed if console == null)
    * @param src The absolute path to the source fso
    * @param link The absolute path to the link fso
    * @param console The console in which execute the program. <code>null</code>
    * to attach to the default console
    * @return boolean The operation result
    * @throws FileNotFoundException If the initial directory not exists
    * @throws IOException If initial directory couldn't be checked
    * @throws InvalidCommandDefinitionException If the command has an invalid definition
    * @throws NoSuchFileOrDirectory If the file or directory was not found
    * @throws ConsoleAllocException If the console can't be allocated
    * @throws InsufficientPermissionsException If an operation requires elevated permissions
    * @throws CommandNotFoundException If the command was not found
    * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
    * @throws ExecutionException If the operation returns a invalid exit code
    * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
    * @throws CancelledOperationException If the operation was cancelled
    * @see LinkExecutable
    */
   public static boolean createLink(Context context, String src, String link, Console console)
           throws FileNotFoundException, IOException, ConsoleAllocException,
           NoSuchFileOrDirectory, InsufficientPermissionsException,
           CommandNotFoundException, OperationTimeoutException,
           ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
           CancelledOperationException {
       Console c = ensureConsole(context, console);
       LinkExecutable executable =
               c.getExecutableFactory().newCreator().createLinkExecutable(src, link);
       writableExecute(context, executable, c);
       return executable.getResult().booleanValue();
   }

    /**
     * Method that retrieves the parent directory of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return String The current directory
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ParentDirExecutable
     */
    public static String getParentDir(Context context, String src, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, src);
        ParentDirExecutable executable =
                c.getExecutableFactory().newCreator().createParentDirExecutable(src);
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that retrieves the value of a variable.
     *
     * @param context The current context (needed if console == null)
     * @param msg The message to echo. This message can have one or multiple variables
     * and text. xe: "This is $VAR_1 the value of $VAR2" or simple "$PATH"
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return String The value  of the variable
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see EchoExecutable
     */
    public static String getVariable(Context context, String msg, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        EchoExecutable executable =
                c.getExecutableFactory().newCreator().createEchoExecutable(msg);
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that lists a directory.
     *
     * @param context The current context (needed if console == null)
     * @param directory The path of the directory to list
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<FileSystemObject> The list of files of the directory
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ListExecutable
     */
    public static List<FileSystemObject> listFiles(
            Context context, String directory, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, directory);
        ListExecutable executable =
                c.getExecutableFactory().newCreator().
                    createListExecutable(directory);
        execute(context, executable, c);
        List<FileSystemObject> result = executable.getResult();
        FileHelper.resolveSymlinks(context, result);

        // And now we need to verify if the directory is the
        if (VirtualMountPointConsole.isVirtualStorageDir(directory)) {
            result.addAll(VirtualMountPointConsole.getVirtualMountableDirectories());
        }

        return result;
    }

    /**
     * Method that moves a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object to move
     * @param dst The destination file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see MoveExecutable
     */
    public static boolean move(Context context, String src, String dst, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {

        Console cSrc = ensureConsoleForFile(context, console, src);
        Console cDst = ensureConsoleForFile(context, console, dst);
        boolean ret = true;
        if (cSrc.equals(cDst) && !FileHelper.isSamePath(src, dst)) {
            // Is safe to use the same console
            MoveExecutable executable =
                    cSrc.getExecutableFactory().newCreator().createMoveExecutable(src, dst);
            writableExecute(context, executable, cSrc);
            ret = executable.getResult().booleanValue();
        } else {
            // We need to create a temporary file in the external filesystem to make it
            // available to virtual consoles

            // 1.- Move to a temporary file with the source console (destination
            // is a safe location)
            File tmp = FileHelper.createTempFilename(context, true);
            try {
                MoveExecutable moveExecutable =
                        cSrc.getExecutableFactory().newCreator().createMoveExecutable(
                                src, tmp.getAbsolutePath());
                writableExecute(context, moveExecutable, cSrc);
                if (!moveExecutable.getResult().booleanValue()) {
                    ret = false;
                }

                // 2.- Move the temporary file to the final filesystem with the destination console
                if (ret) {
                    moveExecutable =
                            cDst.getExecutableFactory().newCreator().createMoveExecutable(
                                    tmp.getAbsolutePath(), dst);
                    writableExecute(context, moveExecutable, cDst);
                    if (!moveExecutable.getResult().booleanValue()) {
                        ret = false;
                    }
                }

            } finally {
                FileHelper.deleteFileOrFolder(tmp);
            }
        }

        // Do media scan (don't scan the file if is virtual file)
        if (ret) {
            File parent = new File(src).getParentFile();
            if (parent != null) {
                if (!VirtualMountPointConsole.isVirtualStorageResource(parent.getAbsolutePath())) {
                    // Remove from media scanner
                    removeFromMediaStore(context, src);
                }
            }
            if (!VirtualMountPointConsole.isVirtualStorageResource(parent.getAbsolutePath())) {
                MediaScannerConnection.scanFile(context, new String[]{
                        MediaHelper.normalizeMediaPath(dst)}, null, null);
            }
        }

        return ret;
    }

    private static void removeFromMediaStore(Context context, String path) {
        context.getContentResolver().delete(Files.getContentUri(MediaHelper.EXTERNAL_VOLUME),
            MediaStore.Files.FileColumns.DATA + "=?", new String[]{path});
    }

    /**
     * Method that copies a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The file system object to copy
     * @param dst The destination file system object
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CopyExecutable
     */
    public static boolean copy(Context context, String src, String dst, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {

        Console cSrc = ensureConsoleForFile(context, console, src);
        Console cDst = ensureConsoleForFile(context, console, dst);
        boolean ret = true;
        if (cSrc.equals(cDst) && !FileHelper.isSamePath(src, dst)) {
            // Is safe to use the same console
            CopyExecutable executable =
                    cSrc.getExecutableFactory().newCreator().createCopyExecutable(src, dst);
            writableExecute(context, executable, cSrc);
            ret = executable.getResult().booleanValue();
        } else {
            // We need to create a temporary file in the external filesystem to make it
            // available to virtual consoles

            // 1.- Copy to a temporary file with the source console (destination
            // is a safe location)
            File tmp = FileHelper.createTempFilename(context, true);
            try {
                CopyExecutable copyExecutable =
                        cSrc.getExecutableFactory().newCreator().createCopyExecutable(
                                src, tmp.getAbsolutePath());
                writableExecute(context, copyExecutable, cSrc);
                if (!copyExecutable.getResult().booleanValue()) {
                    ret = false;
                }

                // 2.- Move the temporary file to the final filesystem with the destination console
                if (ret) {
                    MoveExecutable moveExecutable =
                            cDst.getExecutableFactory().newCreator().createMoveExecutable(
                                    tmp.getAbsolutePath(), dst);
                    writableExecute(context, moveExecutable, cDst);
                    if (!moveExecutable.getResult().booleanValue()) {
                        ret = false;
                    }
                }

            } finally {
                FileHelper.deleteFileOrFolder(tmp);
            }
        }

        // Do media scan (don't scan the file if is virtual file)
        if (ret) {
            if (!VirtualMountPointConsole.isVirtualStorageResource(dst)) {
                MediaScannerConnection.scanFile(context, new String[]{
                        MediaHelper.normalizeMediaPath(dst)}, null, null);
            }
        }

        return ret;
    }

    /**
     * Method that executes a command.
     *
     * @param context The current context (needed if console == null)
     * @param cmd The command to execute
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return ExecExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ExecExecutable
     */
    public static ExecExecutable exec(
            Context context, String cmd, AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        ExecExecutable executable =
                c.getExecutableFactory().newCreator().
                    createExecExecutable(cmd, asyncResultListener);
        execute(context, executable, c);
        return executable;
    }

    /**
     * Method that does a search in a directory tree seeking for some terms.
     *
     * @param context The current context (needed if console == null)
     * @param directory The "absolute" directory where start the search
     * @param search The terms to be searched
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return FindExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see SearchResult
     * @see FindExecutable
     */
    public static FindExecutable findFiles(
            Context context, String directory, Query search,
            ConcurrentAsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        List<Console> consoles = new ArrayList<Console>();
        List<FindExecutable> executables = new ArrayList<FindExecutable>();
        Console c = ensureConsoleForFile(context, console, directory);
        consoles.add(c);

        // Obtain all the rest of console that will participate in the search, that aren't the
        // current console
        List<Console> vcs = VirtualMountPointConsole.getVirtualConsoleForSearchPath(directory);
        for (int i = vcs.size() - 1; i >= 0; i--) {
            Console vc = vcs.get(i);
            if (vc.equals(c)) {
                vcs.remove(i);
            }
        }
        consoles.addAll(vcs);

        // Register all the executables
        for (Console cc : consoles) {
            executables.add(
                    cc.getExecutableFactory().newCreator().
                        createFindExecutable(directory, search, asyncResultListener));
        }

        // Launch every executable
        int count = executables.size();
        for (int i = 0; i < count; i++) {
            execute(context, executables.get(i), consoles.get(i));
        }

        // Return the first of the executables
        return executables.get(0);
    }

    /**
     * Method that compute the disk usage of a folder.
     *
     * @param context The current context (needed if console == null)
     * @param directory The "absolute" directory where start the search
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return FolderUsageExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see FolderUsage
     * @see FolderUsageExecutable
     */
    public static FolderUsageExecutable getFolderUsage(
            Context context, String directory,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, directory);
        FolderUsageExecutable executable =
                c.getExecutableFactory().newCreator().
                    createFolderUsageExecutable(directory, asyncResultListener);
        execute(context, executable, c);
        return executable;
    }

    /**
     * Method that retrieves the disk usage of all the mount points.
     *
     * @param context The current context (needed if console == null)
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<DiskUsage> The disk usage of all the mount points
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see DiskUsageExecutable
     */
    public static List<DiskUsage> getDiskUsage(Context context, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        DiskUsageExecutable executable =
                c.getExecutableFactory().newCreator().createDiskUsageExecutable();
        execute(context, executable, c);
        List<DiskUsage> diskUsage = executable.getResult();
        diskUsage.addAll(VirtualMountPointConsole.getVirtualDiskUsage());
        return diskUsage;
    }

    /**
     * Method that retrieves the disk usage of all mount points.
     *
     * @param context The current context (needed if console == null)
     * @param dir The directory of which obtain its disk usage
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return DiskUsage The disk usage information
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see DiskUsageExecutable
     */
    public static DiskUsage getDiskUsage(Context context, String dir, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {

        // Virtual directories don't implement a disk usage command, just return the data if
        // the directory belongs to a virtual filesystem
        VirtualMountPointConsole vc = VirtualMountPointConsole.getVirtualConsoleForPath(dir);
        if (vc != null) {
            return vc.getDiskUsage(dir);
        } else {
            Console c = ensureConsole(context, console);
            DiskUsageExecutable executable =
                    c.getExecutableFactory().newCreator().createDiskUsageExecutable(dir);
            execute(context, executable, c);
            List<DiskUsage> du = executable.getResult();
            for (DiskUsage d : du) {
                if (d.getMountPoint().equals(dir)) {
                    return d;
                }
            }
        }
        return null;
    }

    /**
     * Method that retrieves the information about all mount points.
     *
     * @param context The current context (needed if console == null)
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<MountPoint> The filesystem mount points
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see MountPointInfoExecutable
     */
    public static List<MountPoint> getMountPoints(Context context, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        MountPointInfoExecutable executable =
                c.getExecutableFactory().newCreator().createMountPointInfoExecutable();
        execute(context, executable, c);
        List<MountPoint> mountPoints = executable.getResult();
        mountPoints.addAll(VirtualMountPointConsole.getVirtualMountPoints());
        return mountPoints;
    }

    /**
     * Method that re-mounts a filesystem from his mount point info.
     *
     * @param context The current context (needed if console == null)
     * @param mp The mount point to re-mount
     * @param rw Indicates if the operation re-mounted the device as read-write
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return boolean The operation result
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see MountExecutable
     */
    public static boolean remount(Context context, MountPoint mp, boolean rw, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        boolean ret = false;
        if (mp.isSecure()) {
            // Unmount the secure file system
            SecureConsole sc = (SecureConsole) ensureConsoleForFile(
                    context, console, mp.getMountPoint());
            if (rw) {
                sc.mount(context);
            } else {
                sc.unmount();
            }
            ret = true;
        } else {
            Console c = ensureConsole(context, console);
            MountExecutable executable =
                    c.getExecutableFactory().newCreator().createMountExecutable(mp, rw);
            execute(context, executable, c);
            ret = executable.getResult().booleanValue();
        }

        if (ret) {
            // Send an broadcast to notify that the mount state of this filesystem changed
            Intent intent = new Intent(FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED);
            intent.putExtra(FileManagerSettings.EXTRA_MOUNTPOINT, mp.getMountPoint());
            intent.putExtra(FileManagerSettings.EXTRA_STATUS, rw
                    ? MountExecutable.READWRITE : MountExecutable.READONLY);
            context.sendBroadcast(intent);
        }

        return ret;
    }

    /**
     * Method that makes a quick folder search for the passed expression.
     *
     * @param context The current context (needed if console == null)
     * @param regexp The expression to search
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<String> The list of directories found
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see QuickFolderSearchExecutable
     */
    public static List<String> quickFolderSearch(Context context, String regexp, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        QuickFolderSearchExecutable executable =
                c.getExecutableFactory().newCreator().createQuickFolderSearchExecutable(regexp);
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that retrieves the process identifier of all the processes (a program
     * owned by the main process of this application).
     *
     * @param context The current context (needed if console == null)
     * @param pid The process id of the shell where the command is running
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return List<Integer> The processes identifiers of the program or <code>null</code> if not exists
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ProcessIdExecutable
     */
    public static List<Integer> getProcessesIds(
            Context context, int pid, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        ProcessIdExecutable executable =
                c.getExecutableFactory().newCreator().createProcessIdExecutable(pid);
        execute(context, executable, c);
        return executable.getResult();
    }

    /**
     * Method that retrieves the process identifier of a process (a program
     * owned by the main process of this application).
     *
     * @param context The current context (needed if console == null)
     * @param pid The process id of the shell where the command is running
     * @param processName The process name
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return Integer The process identifier of the program or <code>null</code> if not exists
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ProcessIdExecutable
     */
    public static Integer getProcessId(
            Context context, int pid, String processName, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        ProcessIdExecutable executable =
                c.getExecutableFactory().newCreator().createProcessIdExecutable(pid, processName);
        execute(context, executable, c);
        List<Integer> pids = executable.getResult();
        if (pids != null && pids.size() > 0) {
            return pids.get(0);
        }
        return null;
    }

    /**
     * Method that send a signal to a process.
     *
     * @param context The current context (needed if console == null)
     * @param process The process which to send the signal
     * @param signal The signal to send
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ProcessIdExecutable
     */
    public static void sendSignal(
            Context context, int process, SIGNAL signal, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        SendSignalExecutable executable =
                c.getExecutableFactory().newCreator().createSendSignalExecutable(process, signal);
        execute(context, executable, c);
    }

    /**
     * Method that send a kill signal to a process.
     *
     * @param context The current context (needed if console == null)
     * @param process The process which to send the signal
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ProcessIdExecutable
     */
    public static void sendSignal(
            Context context, int process, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsole(context, console);
        SendSignalExecutable executable =
                c.getExecutableFactory().newCreator().createKillExecutable(process);
        execute(context, executable, c);
    }

    /**
     * Method that read data from disk.
     *
     * @param context The current context (needed if console == null)
     * @param file The file where to read the data
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return ReadExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see "byte[]"
     * @see ReadExecutable
     */
    public static ReadExecutable read(
            Context context, String file,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, file);
        ReadExecutable executable =
                c.getExecutableFactory().newCreator().
                    createReadExecutable(file, asyncResultListener);
        execute(context, executable, c);
        return executable;
    }

    /**
     * Method that writes data to disk.
     *
     * @param context The current context (needed if console == null)
     * @param file The file where to write the data
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return WriteExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see WriteExecutable
     */
    public static WriteExecutable write(
            Context context, String file,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, file);

        // Create a wrapper listener, for unmount the filesystem if necessary
        UnmountAsyncResultListener wrapperListener = new UnmountAsyncResultListener();
        wrapperListener.mCtx = context;
        wrapperListener.mConsole = c;
        wrapperListener.mRef = asyncResultListener;

        // Prior to write to disk the data, ensure that can write to the disk using
        // createFile method
        //- Create
        CreateFileExecutable executable1 =
                c.getExecutableFactory().newCreator().createCreateFileExecutable(file);
        boolean unmount = writableExecute(context, executable1, c, true);
        if (executable1.getResult().booleanValue()) {
            // Configure the rest of attributes of the wrapper listener
            wrapperListener.mUnmount = unmount;
            wrapperListener.mMountPoint = executable1.getDstWritableMountPoint();

            //- Write
            WriteExecutable executable2 =
                    c.getExecutableFactory().newCreator().
                        createWriteExecutable(file, wrapperListener);
            execute(context, executable2, c);
            return executable2;
        }
        throw new ExecutionException(String.format("Fail to create file %s", file)); //$NON-NLS-1$
    }

    /**
     * Method that archive-compress file system objects.
     *
     * @param context The current context (needed if console == null)
     * @param mode The compression mode
     * @param dst The destination compressed file
     * @param src The array of source files to compress
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return CompressExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CompressExecutable
     */
    public static CompressExecutable compress(
            Context context, CompressionMode mode, String dst, String[] src,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);

        // Create a wrapper listener, for unmount the filesystem if necessary
        UnmountAsyncResultListener wrapperListener = new UnmountAsyncResultListener();
        wrapperListener.mCtx = context;
        wrapperListener.mConsole = c;
        wrapperListener.mRef = asyncResultListener;

        CompressExecutable executable1 =
                c.getExecutableFactory().newCreator().
                    createCompressExecutable(mode, dst, src, wrapperListener);

        // Prior to write to disk the data, ensure that can write to the disk using
        // createFile method
        //- Create
        String compressOutFile = executable1.getOutCompressedFile();
        CreateFileExecutable executable2 =
                c.getExecutableFactory().
                    newCreator().
                        createCreateFileExecutable(compressOutFile);
        boolean unmount = writableExecute(context, executable2, c, true);
        if (executable2.getResult().booleanValue()) {
            // Configure the rest of attributes of the wrapper listener
            wrapperListener.mUnmount = unmount;
            wrapperListener.mMountPoint = executable2.getDstWritableMountPoint();

            // Some archive modes requires a new file. Ensure that the created
            // file doesn't exists
            DeleteFileExecutable executable3 =
                                c.getExecutableFactory().
                                    newCreator().
                                        createDeleteFileExecutable(compressOutFile);
            writableExecute(context, executable3, c, true);
            if (executable3.getResult().booleanValue()) {
                //- Compress
                execute(context, executable1, c);
                return executable1;
            }
        }
        throw new ExecutionException(
                String.format("Fail to create file %s", compressOutFile)); //$NON-NLS-1$
    }

    /**
     * Method that compress a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param mode The compression mode
     * @param src The file to compress
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return CompressExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CompressExecutable
     */
    public static CompressExecutable compress(
            Context context, CompressionMode mode, String src,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);

        // Create a wrapper listener, for unmount the filesystem if necessary
        UnmountAsyncResultListener wrapperListener = new UnmountAsyncResultListener();
        wrapperListener.mCtx = context;
        wrapperListener.mConsole = c;
        wrapperListener.mRef = asyncResultListener;

        CompressExecutable executable1 =
                c.getExecutableFactory().newCreator().
                    createCompressExecutable(mode, src, wrapperListener);

        // Prior to write to disk the data, ensure that can write to the disk using
        // createFile method
        //- Create
        String compressOutFile = executable1.getOutCompressedFile();
        CreateFileExecutable executable2 =
                c.getExecutableFactory().
                    newCreator().
                        createCreateFileExecutable(compressOutFile);
        boolean unmount = writableExecute(context, executable2, c, true);
        if (executable2.getResult().booleanValue()) {
            // Configure the rest of attributes of the wrapper listener
            wrapperListener.mUnmount = unmount;
            wrapperListener.mMountPoint = executable2.getDstWritableMountPoint();

            //- Compress
            execute(context, executable1, c);
            return executable1;
        }
        throw new ExecutionException(
                String.format("Fail to compress to file %s", compressOutFile)); //$NON-NLS-1$
    }

    /**
     * Method that uncompress file system objects.
     *
     * @param context The current context (needed if console == null)
     * @param src The file to compress
     * @param dst The destination file of folder (if null this method resolve with the best
     * fit based on the src)
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return UncompressExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @see CompressExecutable
     */
    public static UncompressExecutable uncompress(
            Context context, String src, String dst,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, ReadOnlyFilesystemException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);

        // Create a wrapper listener, for unmount the filesystem if necessary
        UnmountAsyncResultListener wrapperListener = new UnmountAsyncResultListener();
        wrapperListener.mCtx = context;
        wrapperListener.mConsole = c;
        wrapperListener.mRef = asyncResultListener;

        UncompressExecutable executable1 =
                c.getExecutableFactory().newCreator().
                    createUncompressExecutable(src, dst, wrapperListener);

        // Prior to write to disk the data, ensure that can write to the disk using
        // createFile or createFolder method

        String compressOutFile = executable1.getOutUncompressedFile();
        WritableExecutable executable2 = null;
        if (executable1.IsArchive()) {
            //- Create Folder
            executable2 =
                    c.getExecutableFactory().
                        newCreator().
                            createCreateDirectoryExecutable(compressOutFile);
        } else {
            //- Create File
            executable2 =
                    c.getExecutableFactory().
                        newCreator().
                            createCreateFileExecutable(compressOutFile);
        }
        boolean unmount = writableExecute(context, executable2, c, true);
        if (((Boolean)executable2.getResult()).booleanValue()) {
            // Configure the rest of attributes of the wrapper listener
            wrapperListener.mUnmount = unmount;
            wrapperListener.mMountPoint = executable2.getDstWritableMountPoint();

            //- Compress
            execute(context, executable1, c);

            // Do media scan
            MediaScannerConnection.scanFile(context, new String[]{
                    MediaHelper.normalizeMediaPath(compressOutFile)}, null, null);

            return executable1;
        }
        throw new ExecutionException(
                String.format("Fail to uncompress to %s", compressOutFile)); //$NON-NLS-1$
    }

    /**
     * Method that calculates the checksum of a file system object.
     *
     * @param context The current context (needed if console == null)
     * @param src The source file
     * @param asyncResultListener The partial result listener
     * @param console The console in which execute the program.
     * <code>null</code> to attach to the default console
     * @return ChecksumExecutable The command executed in background
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @see ChecksumExecutable
     */
    public static ChecksumExecutable checksum(Context context, String src,
            AsyncResultListener asyncResultListener, Console console)
            throws FileNotFoundException, IOException, ConsoleAllocException,
            NoSuchFileOrDirectory, InsufficientPermissionsException,
            CommandNotFoundException, OperationTimeoutException,
            ExecutionException, InvalidCommandDefinitionException, CancelledOperationException {
        Console c = ensureConsoleForFile(context, console, src);
        ChecksumExecutable executable =
                c.getExecutableFactory().newCreator().
                    createChecksumExecutable(src, asyncResultListener);
        execute(context, executable, c);
        return executable;
    }

    /**
     * Method that re-execute the command.
     *
     * @param context The current context (needed if console == null)
     * @param executable The executable program to execute
     * @param console The console in which execute the program. <code>null</code>
     * to attach to the default console
     * @return Object The result of the re-execution
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws IOException If initial directory couldn't be checked
     * @throws CancelledOperationException If the operation was cancelled
     * @throws FileNotFoundException If the initial directory not exists
     */
    public static Object reexecute(
            Context context , SyncResultExecutable executable, Console console)
            throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
            OperationTimeoutException, ExecutionException,
            CommandNotFoundException, ReadOnlyFilesystemException,
            FileNotFoundException, IOException, InvalidCommandDefinitionException,
            CancelledOperationException {
        Console c = ensureConsole(context, console);
        c.execute(executable, context);
        return executable.getResult();
    }


    /**
     * Method that execute a program.
     *
     * @param context The current context (needed if console == null)
     * @param executable The executable program to execute
     * @param console The console in which execute the program. <code>null</code> to attach
     * to the default console
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws CancelledOperationException If the operation was cancelled
     * @throws AuthenticationFailedException If the operation failed caused by an
     * authentication failure
     */
    private static void execute(Context context, Executable executable, Console console)
            throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
            OperationTimeoutException, ExecutionException, CommandNotFoundException,
            CancelledOperationException, AuthenticationFailedException {
        try {
            console.execute(executable, context);
        } catch (ReadOnlyFilesystemException rofEx) {
            // ReadOnlyFilesystemException don't have sense if command is not writable
            // WritableExecutable must be used with "writableExecute" method
            throw new ExecutionException(rofEx.getMessage(), rofEx);
        }
    }

    /**
     * Method that execute a program that requires write permissions over the filesystem. This
     * method ensure mount/unmount the filesystem before/after executing the operation.
     *
     * @param context The current context (needed if console == null)
     * @param executable The writable executable program to execute
     * @param console The console in which execute the program. <code>null</code> to attach
     * to the default console
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @throws AuthenticationFailedException If the operation failed caused by an
     * authentication failure
     */
    private static void writableExecute(Context context, WritableExecutable executable,
            Console console) throws ConsoleAllocException, InsufficientPermissionsException,
            NoSuchFileOrDirectory, OperationTimeoutException, ExecutionException,
            CommandNotFoundException, ReadOnlyFilesystemException, CancelledOperationException,
            AuthenticationFailedException{
        writableExecute(context, executable, console, false);
    }

    /**
     * Method that execute a program that requires write permissions over the filesystem. This
     * method ensure mount/unmount the filesystem before/after executing the operation.
     *
     * @param context The current context (needed if console == null)
     * @param executable The writable executable program to execute
     * @param console The console in which execute the program. <code>null</code> to attach
     * to the default console
     * @param leaveDeviceMounted If the operation must leave the filesystem mounted after
     * the execution
     * @return boolean If the filesystem was left unmount
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     * @throws ReadOnlyFilesystemException If the operation writes in a read-only filesystem
     * @throws CancelledOperationException If the operation was cancelled
     * @throws AuthenticationFailedException If the operation failed caused by an
     * authentication failure
     */
    private static boolean writableExecute(Context context, WritableExecutable executable,
            Console console, boolean leaveDeviceMounted) throws ConsoleAllocException,
            InsufficientPermissionsException, NoSuchFileOrDirectory, OperationTimeoutException,
            ExecutionException, CommandNotFoundException, ReadOnlyFilesystemException,
            CancelledOperationException, AuthenticationFailedException {

        //Retrieve the mount point information to check if a remount operation is required
        //There are 2 mount points: destination and source. Check both
        // - Destination
        boolean needMountDst = false;
        MountPoint mpDst = executable.getDstWritableMountPoint();
        if (mpDst != null) {
            if (MountPointHelper.isMountAllowed(mpDst)) {
                if (!MountPointHelper.isReadWrite(mpDst)) {
                    needMountDst = true;
                } else {
                    //Mount point is already read-write
                }
            } else {
                //For security or physical reasons the mount point can't be
                //mounted as read-write. Execute the command
                //and notify to the user
            }
        } else {
            //Don't have information about the mount point. Execute the command
            //and notify to the user
        }
        // - Source
        boolean needMountSrc = false;
        MountPoint mpSrc = executable.getSrcWritableMountPoint();
        if (mpSrc != null) {
            if (MountPointHelper.isMountAllowed(mpSrc)) {
                if (!MountPointHelper.isReadWrite(mpSrc)) {
                    needMountSrc = true;
                } else {
                    //Mount point is already read-write
                }
            } else {
                //For security or physical reasons the mount point can't be
                //mounted as read-write. Execute the command
                //and notify to the user
            }
        } else {
            //Don't have information about the mount point. Execute the command
            //and notify to the user
        }

        //Create the mount/unmount executables
        MountExecutable mountDstExecutable = null;
        MountExecutable unmountDstExecutable = null;
        if (needMountDst) {
            mountDstExecutable =
                    console.getExecutableFactory().newCreator().
                        createMountExecutable(mpDst, true);
            unmountDstExecutable =
                    console.getExecutableFactory().newCreator().
                        createMountExecutable(mpDst, false);
        }
        MountExecutable mountSrcExecutable = null;
        MountExecutable unmountSrcExecutable = null;
        if (needMountSrc) {
            mountSrcExecutable =
                    console.getExecutableFactory().newCreator().
                        createMountExecutable(mpSrc, true);
            unmountSrcExecutable =
                    console.getExecutableFactory().newCreator().
                        createMountExecutable(mpSrc, false);
        }

        //Execute the commands
        boolean mountExecutedDst = false;
        boolean mountExecutedSrc = false;
        try {
            if (needMountDst) {
                //Execute the mount command
                console.execute(mountDstExecutable, context);
                mountExecutedDst = true;
            }
            if (needMountSrc) {
                //Execute the mount command
                console.execute(mountSrcExecutable, context);
                mountExecutedSrc = true;
            }

            //Execute the command
            console.execute(executable, context);

        } catch (InsufficientPermissionsException ipEx) {
            //Configure the commands to execute
            if (needMountDst && !mountExecutedDst) {
                //The failed operation was the mount rw operation
                //This operations is already in the exception in the fifo queue
                ipEx.addExecutable(executable);
            } else if (needMountSrc && !mountExecutedSrc) {
                //The failed operation was the mount rw operation
                //This operations is already in the exception in the fifo queue
                ipEx.addExecutable(executable);
            }
            if (needMountDst) {
                //A mount operation was executed or will be executed
                ipEx.addExecutable(unmountDstExecutable);
            }
            if (needMountSrc) {
                //A mount operation was executed or will be executed
                ipEx.addExecutable(unmountSrcExecutable);
            }

            //Rethrow the exception
            throw ipEx;

        } finally {
            //If previously was a mount successful execution, then execute
            //and unmount operation
            if (mountExecutedDst && !leaveDeviceMounted) {
                //Execute the unmount command
                console.execute(unmountDstExecutable, context);
            }
            if (mountExecutedSrc && !leaveDeviceMounted) {
                //Execute the unmount command
                console.execute(unmountSrcExecutable, context);
            }
        }

        // If the needed unmount was executed
        return (mountExecutedDst || mountExecutedSrc) && leaveDeviceMounted;
    }

    /**
     * Method that ensure the console retrieve the default console if a console
     * is not passed.
     *
     * @param context The current context (needed if console == null)
     * @param console The console passed
     * @return Console The console passed if not is null. Otherwise, the default console
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws IOException If initial directory couldn't be checked
     * @throws FileNotFoundException If the initial directory not exists
     */
    private static Console ensureConsole(Context context, Console console)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        Console c = console;
        if (c == null) {
            c = ConsoleBuilder.getConsole(context);
        }
        return c;
    }

    /**
     * Method that ensure the console retrieve the default console if a console
     * is not passed.
     *
     * @param context The current context (needed if console == null)
     * @param console The console passed
     * @param src The source file to check
     * @return Console The console passed if not is null. Otherwise, the default console
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws IOException If initial directory couldn't be checked
     * @throws FileNotFoundException If the initial directory not exists
     */
    public static Console ensureConsoleForFile(Context context, Console console, String src)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {

        // Check if the path belongs to a virtual mount point
        Console c = VirtualMountPointConsole.getVirtualConsoleForPath(src);
        if (c != null) {
            return c;
        }

        // Recover a real console
        c = console;
        if (c == null) {
            c = ConsoleBuilder.getConsole(context);
        }
        return c;
    }

}
