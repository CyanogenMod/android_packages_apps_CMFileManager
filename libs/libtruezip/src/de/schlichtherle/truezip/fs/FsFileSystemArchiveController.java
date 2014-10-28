/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * This abstract archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
abstract class FsFileSystemArchiveController<E extends FsArchiveEntry>
extends FsBasicArchiveController<E> {

    /** The mount state of the archive file system. */
    private MountState<E> mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FsFileSystemArchiveController
     */
    FsFileSystemArchiveController(FsLockModel model) {
        super(model);
    }

    @Override
    final FsArchiveFileSystem<E> autoMount(final boolean autoCreate)
    throws IOException {
        return mountState.autoMount(autoCreate);
    }

    final FsArchiveFileSystem<E> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(FsArchiveFileSystem<E> fileSystem) {
        mountState.setFileSystem(fileSystem);
    }

    /**
     * Mounts the (virtual) archive file system from the target file.
     * This method is called while the write lock to mount the file system
     * for this controller is acquired.
     * <p>
     * Upon normal termination, this method is expected to have called
     * {@link #setFileSystem} to assign the fully initialized file system
     * to this controller.
     * Other than this, the method must not have any side effects on the
     * state of this class or its super class.
     * It may, however, have side effects on the state of the sub class.
     *
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a (virtual) root
     *        directory is created with its last modification time set to the
     *        system's current time.
     */
    abstract void mount(boolean autoCreate) throws IOException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private interface MountState<E extends FsArchiveEntry> {
        FsArchiveFileSystem<E> autoMount(boolean autoCreate)
        throws IOException;

        FsArchiveFileSystem<E> getFileSystem();

        void setFileSystem(FsArchiveFileSystem<E> fileSystem);
    } // MountState

    private final class ResetFileSystem implements MountState<E> {
        @Override
        public FsArchiveFileSystem<E> autoMount(final boolean autoCreate)
        throws IOException {
            checkWriteLockedByCurrentThread();
            mount(autoCreate);
            assert this != mountState;
            return mountState.getFileSystem();
        }

        @Override
        public FsArchiveFileSystem<E> getFileSystem() {
            return null;
        }

        @Override
        public void setFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // ResetFileSystem

    private final class MountedFileSystem implements MountState<E> {
        private final FsArchiveFileSystem<E> fileSystem;

        MountedFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            if (null == fileSystem)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        public FsArchiveFileSystem<E> autoMount(boolean autoCreate) {
            return fileSystem;
        }

        @Override
        public FsArchiveFileSystem<E> getFileSystem() {
            return fileSystem;
        }

        @Override
        public void setFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            if (null != fileSystem)
                throw new IllegalArgumentException("File system already mounted!");
            mountState = new ResetFileSystem();
        }
    } // MountedFileSystem
}
