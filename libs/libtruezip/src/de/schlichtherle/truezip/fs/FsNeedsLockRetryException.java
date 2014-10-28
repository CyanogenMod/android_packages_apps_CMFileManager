/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ControlFlowException;

/**
 * Indicates that all file system locks need to get released before the
 * operation can get retried.
 *
 * @since  TrueZIP 7.5
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class FsNeedsLockRetryException extends ControlFlowException {

    private static final FsNeedsLockRetryException
            INSTANCE = new FsNeedsLockRetryException();

    private FsNeedsLockRetryException() { }

    static FsNeedsLockRetryException get() {
        return isTraceable() ? new FsNeedsLockRetryException() : INSTANCE;
    }
}
