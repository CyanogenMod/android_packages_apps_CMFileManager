/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ControlFlowException;

/**
 * Indicates that the file system needs to get write locked before the
 * operation can get retried.
 *
 * @see    FsLockController
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class FsNeedsWriteLockException extends ControlFlowException {

    private static final FsNeedsWriteLockException
            INSTANCE = new FsNeedsWriteLockException();

    private FsNeedsWriteLockException() { }

    static FsNeedsWriteLockException get() {
        return isTraceable() ? new FsNeedsWriteLockException() : INSTANCE;
    }
}
