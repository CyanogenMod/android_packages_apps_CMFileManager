/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * An abstract file system controller which requires an
 * {@link FsLockModel} so that it can forward its additional method
 * calls to this model for the convenience of the sub-class.
 *
 * @param   <C> The type of the decorated file system controller.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
abstract class FsLockModelDecoratingController<
        C extends FsController<? extends FsLockModel>>
extends FsDecoratingController<FsLockModel, C>  {

    protected static final int WAIT_TIMEOUT_MILLIS = 100;

    /**
     * Constructs a new decorating file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FsLockModelDecoratingController(C controller) {
        super(controller);
    }

    protected ReadLock readLock() {
        return getModel().readLock();
    }

    protected WriteLock writeLock() {
        return getModel().writeLock();
    }

    protected final boolean isReadLockedByCurrentThread() {
        return getModel().isReadLockedByCurrentThread();
    }

    protected final boolean isWriteLockedByCurrentThread() {
        return getModel().isWriteLockedByCurrentThread();
    }

    protected final void checkWriteLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        getModel().checkWriteLockedByCurrentThread();
    }
}