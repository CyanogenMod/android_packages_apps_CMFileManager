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

package com.cyanogenmod.filemanager.commands.java;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ChangeOwnerExecutable;
import com.cyanogenmod.filemanager.commands.ChangePermissionsExecutable;
import com.cyanogenmod.filemanager.commands.ChecksumExecutable;
import com.cyanogenmod.filemanager.commands.CompressExecutable;
import com.cyanogenmod.filemanager.commands.CopyExecutable;
import com.cyanogenmod.filemanager.commands.CreateDirExecutable;
import com.cyanogenmod.filemanager.commands.CreateFileExecutable;
import com.cyanogenmod.filemanager.commands.DeleteDirExecutable;
import com.cyanogenmod.filemanager.commands.DeleteFileExecutable;
import com.cyanogenmod.filemanager.commands.DiskUsageExecutable;
import com.cyanogenmod.filemanager.commands.EchoExecutable;
import com.cyanogenmod.filemanager.commands.ExecExecutable;
import com.cyanogenmod.filemanager.commands.ExecutableCreator;
import com.cyanogenmod.filemanager.commands.FindExecutable;
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.commands.GroupsExecutable;
import com.cyanogenmod.filemanager.commands.IdentityExecutable;
import com.cyanogenmod.filemanager.commands.LinkExecutable;
import com.cyanogenmod.filemanager.commands.ListExecutable;
import com.cyanogenmod.filemanager.commands.ListExecutable.LIST_MODE;
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
import com.cyanogenmod.filemanager.commands.UncompressExecutable;
import com.cyanogenmod.filemanager.commands.WriteExecutable;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.java.JavaConsole;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.preferences.CompressionMode;

/**
 * A class for create shell {@link "Executable"} objects.
 */
public class JavaExecutableCreator implements ExecutableCreator {

    private final JavaConsole mConsole;

    /**
     * Constructor of <code>JavaExecutableCreator</code>.
     *
     * @param console A shell console that use for create objects
     */
    JavaExecutableCreator(JavaConsole console) {
        super();
        this.mConsole = console;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeOwnerExecutable createChangeOwnerExecutable(
            String fso, User newUser, Group newGroup) throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangePermissionsExecutable createChangePermissionsExecutable(
            String fso, Permissions newPermissions) throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CopyExecutable createCopyExecutable(String src, String dst)
            throws CommandNotFoundException {
        return new CopyCommand(src, dst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateDirExecutable createCreateDirectoryExecutable(String dir)
            throws CommandNotFoundException {
        return new CreateDirCommand(dir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateFileExecutable createCreateFileExecutable(String file)
            throws CommandNotFoundException {
        return new CreateFileCommand(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteDirExecutable createDeleteDirExecutable(String dir)
            throws CommandNotFoundException {
        return new DeleteDirCommand(dir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteFileExecutable createDeleteFileExecutable(String file)
            throws CommandNotFoundException {
        return new DeleteFileCommand(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsageExecutable createDiskUsageExecutable() throws CommandNotFoundException {
        String mountsFile = this.mConsole.getCtx().getString(R.string.mounts_file);
        return new DiskUsageCommand(mountsFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsageExecutable createDiskUsageExecutable(String dir)
            throws CommandNotFoundException {
        String mountsFile = this.mConsole.getCtx().getString(R.string.mounts_file);
        return new DiskUsageCommand(mountsFile, dir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EchoExecutable createEchoExecutable(String msg) throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecExecutable createExecExecutable(
            String cmd, AsyncResultListener asyncResultListener) throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FindExecutable createFindExecutable(
            String directory, Query query, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        return new FindCommand(directory, query, asyncResultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FolderUsageExecutable createFolderUsageExecutable(
            String directory, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        return new FolderUsageCommand(directory, asyncResultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupsExecutable createGroupsExecutable() throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityExecutable createIdentityExecutable() throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkExecutable createLinkExecutable(String src, String link)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ListExecutable createListExecutable(String src)
            throws CommandNotFoundException {
        return new ListCommand(src, LIST_MODE.DIRECTORY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListExecutable createFileInfoExecutable(String src, boolean followSymlinks)
            throws CommandNotFoundException {
        return new ListCommand(src, LIST_MODE.FILEINFO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountExecutable createMountExecutable(MountPoint mp, boolean rw)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPointInfoExecutable createMountPointInfoExecutable()
            throws CommandNotFoundException {
        String mountsFile = this.mConsole.getCtx().getString(R.string.mounts_file);
        return new MountPointInfoCommand(mountsFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MoveExecutable createMoveExecutable(String src, String dst)
            throws CommandNotFoundException {
        return new MoveCommand(src, dst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParentDirExecutable createParentDirExecutable(String fso)
            throws CommandNotFoundException {
        return new ParentDirCommand(fso);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessIdExecutable createShellProcessIdExecutable() throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessIdExecutable createProcessIdExecutable(int pid)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessIdExecutable createProcessIdExecutable(int pid, String processName)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuickFolderSearchExecutable createQuickFolderSearchExecutable(String regexp)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadExecutable createReadExecutable(
            String file, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        return new ReadCommand(file, asyncResultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolveLinkExecutable createResolveLinkExecutable(String fso)
            throws CommandNotFoundException {
        return new ResolveLinkCommand(fso);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SendSignalExecutable createSendSignalExecutable(int process, SIGNAL signal)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SendSignalExecutable createKillExecutable(int process)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteExecutable createWriteExecutable(
            String file, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        return new WriteCommand(file, asyncResultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompressExecutable createCompressExecutable(
            CompressionMode mode, String dst, String[] src,
            AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompressExecutable createCompressExecutable(
            CompressionMode mode, String src,
            AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UncompressExecutable createUncompressExecutable(
            String src, String dst,
            AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        throw new CommandNotFoundException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChecksumExecutable createChecksumExecutable(
            String src, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        return new ChecksumCommand(src, asyncResultListener);
    }

}
