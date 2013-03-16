/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License") throws CommandNotFoundException;
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

package com.cyanogenmod.filemanager.commands;

import com.cyanogenmod.filemanager.commands.ListExecutable.LIST_MODE;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.preferences.CompressionMode;

/**
 * A interface that defines methods for create {@link Executable} objects.
 */
public interface ExecutableCreator {

    /**
     * Method that creates an executable for change the owner of a file system object.
     *
     * @param fso The absolute path to the source file system object
     * @param newUser The new user of the file system object
     * @param newGroup The new group of the file system object
     * @return ChangeOwnerExecutable A {@link ChangeOwnerExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ChangeOwnerExecutable createChangeOwnerExecutable(
            String fso, User newUser, Group newGroup) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for change the permissions of a file system object.
     *
     * @param fso The absolute path to the source file system object
     * @param newPermissions The new permissions of the file system object
     * @return ChangePermissionsExecutable A {@link ChangePermissionsExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ChangePermissionsExecutable createChangePermissionsExecutable(
            String fso, Permissions newPermissions) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for copy a file system object to
     * other file system object.
     *
     * @param src The absolute path to the source file system object
     * @param dst The absolute path to the destination file system object
     * @return CopyExecutable A {@link CopyExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    CopyExecutable createCopyExecutable(String src, String dst) throws CommandNotFoundException,
    NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for create a new directory.
     *
     * @param dir The absolute path of the new directory
     * @return CreateDirExecutable A {@link CreateDirExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    CreateDirExecutable createCreateDirectoryExecutable(String dir)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for create a new file.
     *
     * @param file The absolute path of the new file
     * @return CreateFileExecutable A {@link CreateFileExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    CreateFileExecutable createCreateFileExecutable(String file) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for delete a directory.
     *
     * @param dir The absolute path to the directory to be deleted
     * @return DeleteDirExecutable A {@link DeleteDirExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    DeleteDirExecutable createDeleteDirExecutable(String dir) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for delete a file.
     *
     * @param file The absolute path to the file to be deleted
     * @return DeleteFileExecutable A {@link DeleteFileExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    DeleteFileExecutable createDeleteFileExecutable(String file) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve the disk usage.
     * for all filesystems
     *
     * @return DiskUsageExecutable A {@link DiskUsageExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    DiskUsageExecutable createDiskUsageExecutable() throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve the disk usage.
     * of the filesystem of a directory
     *
     * @param dir The absolute path to the directory
     * @return DiskUsageExecutable A {@link DiskUsageExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    DiskUsageExecutable createDiskUsageExecutable(String dir) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for expanding environment variables.
     * in a message string
     *
     * @param msg The message to expand
     * @return EchoExecutable A {@link EchoExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    EchoExecutable createEchoExecutable(String msg) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that execute a command
     *
     * @param cmd The command to execute
     * @param asyncResultListener The listener where to return partial results
     * @return ExecExecutable A {@link ExecExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ExecExecutable createExecExecutable(
            String cmd, AsyncResultListener asyncResultListener) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for make searches over the filesystem.
     *
     * @param directory The directory where to search
     * @param query The term of the query
     * @param asyncResultListener The listener where to return partial results
     * @return FindExecutable A {@link FindExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    FindExecutable createFindExecutable(
            String directory, Query query, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for compute the disk usage of a folder.
     *
     * @param directory The directory where to search
     * @param asyncResultListener The listener where to return partial results
     * @return FolderUsageExecutable A {@link FolderUsageExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    FolderUsageExecutable createFolderUsageExecutable(
            String directory, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve the groups of the current user.
     *
     * @return GroupsExecutable A {@link GroupsExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    GroupsExecutable createGroupsExecutable()
            throws com.cyanogenmod.filemanager.console.CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve identity information of the current user.
     *
     * @return IdentityExecutable A {@link IdentityExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    IdentityExecutable createIdentityExecutable() throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates a symlink of an other file system object.
     *
     * @param src The absolute path to the source fso
     * @param link The absolute path to the link fso
     * @return LinkExecutable A {@link LinkExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    LinkExecutable createLinkExecutable(
            String src, String link) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for list files of a directory.
     *
     * @param src The directory where to do the listing
     * @return ListExecutable A {@link ListExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @see LIST_MODE#DIRECTORY
     */
    ListExecutable createListExecutable(String src)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve information of a file
     *
     * @param src The directory where to do the listing
     * @param followSymlinks If follow the symlink
     * @return ListExecutable A {@link ListExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @see LIST_MODE#FILEINFO
     */
    ListExecutable createFileInfoExecutable(String src, boolean followSymlinks)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve identity information of the current user.
     *
     * @param mp The mount point to mount
     * @param rw Indicates if the operation mount the device as read-write
     * @return MountExecutable A {@link MountExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    MountExecutable createMountExecutable(
            MountPoint mp, boolean rw) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve identity information of the current user.
     *
     * @return MountPointInfoExecutable A {@link MountPointInfoExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    MountPointInfoExecutable createMountPointInfoExecutable() throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for move a file system object to
     * other file system object.
     *
     * @param src The absolute path to the source file system object
     * @param dst The absolute path to the destination file system object
     * @return MoveExecutable A {@link MoveExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    MoveExecutable createMoveExecutable(String src, String dst) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve the parent directory
     * of a file system object.
     *
     * @param fso The absolute path to the file system object
     * @return ParentDirExecutable A {@link ParentDirExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ParentDirExecutable createParentDirExecutable(String fso) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve operating system process identifier of a
     * shell.
     *
     * @return ProcessIdExecutable A {@link ProcessIdExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ProcessIdExecutable createShellProcessIdExecutable() throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve operating system process identifiers of a
     * shell.
     *
     * @param pid The shell process id where the process is running
     * @param processName The process name
     * @return ProcessIdExecutable A {@link ProcessIdExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ProcessIdExecutable createProcessIdExecutable(int pid) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for retrieve operating system process identifier of a
     * process.
     *
     * @param pid The shell process id where the process is running
     * @param processName The process name
     * @return ProcessIdExecutable A {@link ProcessIdExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ProcessIdExecutable createProcessIdExecutable(
            int pid, String processName) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for quickly retrieve the name of directories
     * that matches a string.
     *
     * @param regexp The regular expression
     * @return ProcessIdExecutable A {@link ProcessIdExecutable} executable implementation
     * reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    QuickFolderSearchExecutable createQuickFolderSearchExecutable(
            String regexp) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for read data from disk.
     *
     * @param file The file where to read the data
     * @param asyncResultListener The listener where to return partial results
     * @return ReadExecutable A {@link ReadExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ReadExecutable createReadExecutable(
            String file, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for resolves the real
     * path of a symlink or file system object.
     *
     * @param fso The absolute path to the file system object
     * @return ResolveLinkExecutable A {@link ResolveLinkExecutable} executable
     * implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ResolveLinkExecutable createResolveLinkExecutable(String fso) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for send a signal to the current process.
     *
     * @param process The process which to send the signal
     * @param signal The signal to send
     * @return SendSignalExecutable A {@link SendSignalExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    SendSignalExecutable createSendSignalExecutable(
            int process, SIGNAL signal) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for send a kill signal to the current process.
     *
     * @param process The process which to send the signal
     * @param signal The signal to send
     * @return SendSignalExecutable A {@link SendSignalExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    SendSignalExecutable createKillExecutable(
            int process) throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for write data to disk.
     *
     * @param file The file where to write the data
     * @param asyncResultListener The listener where to return partial results
     * @return WriteExecutable A {@link WriteExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    WriteExecutable createWriteExecutable(
            String file, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for archive-compress file system objects.
     *
     * @param mode The compression mode
     * @param dst The destination compressed file
     * @param src The array of source files to compress
     * @param asyncResultListener The listener where to return partial results
     * @return CompressExecutable A {@link CompressExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    CompressExecutable createCompressExecutable(
            CompressionMode mode, String dst, String[] src,
            AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for compress a file system object.
     *
     * @param mode The compression mode
     * @param src The file to compress
     * @param asyncResultListener The listener where to return partial results
     * @return CompressExecutable A {@link CompressExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    CompressExecutable createCompressExecutable(
            CompressionMode mode, String src, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for uncompress file system objects.
     *
     * @param src The compressed file
     * @param dst The destination file of folder (if null this method resolve with the best
     * fit based on the src)
     * @param asyncResultListener The listener where to return partial results
     * @return UncompressExecutable A {@link UncompressExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    UncompressExecutable createUncompressExecutable(
            String src, String dst, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

    /**
     * Method that creates an executable for calculate checksums of file system objects.
     *
     * @param src The compressed file
     * @param asyncResultListener The listener where to return partial results
     * @return ChecksumExecutable A {@link ChecksumExecutable} executable implementation reference
     * @throws CommandNotFoundException If the executable can't be created
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     */
    ChecksumExecutable createChecksumExecutable(
            String src, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException,
            NoSuchFileOrDirectory, InsufficientPermissionsException;

}
