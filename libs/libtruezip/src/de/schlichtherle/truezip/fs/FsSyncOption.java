/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;

/**
 * Defines options for (federated) file system synchronization.
 *
 * @see    FsController#sync(BitField)
 * @see    FsSyncOptions
 * @author Christian Schlichtherle
 */
public enum FsSyncOption {

    /**
     * Suppose there are any open input resources (input streams etc.) for any
     * file system entries.
     * Then if this option is set, the respective file system controller waits
     * until all <em>other</em> threads have closed their input resources
     * before proceeding with the update of the federated file system.
     * Input resources allocated by the <em>current</em> thread are always
     * ignored.
     * If the current thread gets interrupted while waiting, it will stop
     * waiting and proceed normally as if this option wasn't set.
     * <p>
     * Beware: If an input resource has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java applications),
     * then the respective file system controller will not return from the
     * update until the current thread gets interrupted!
     */
    // TODO: Merge WAIT_CLOSE_INPUT and WAIT_CLOSE_OUTPUT into WAIT_CLOSE_IO.
    WAIT_CLOSE_INPUT,

    /**
     * Suppose there are any open input resources (input streams etc.) for any
     * file system entries.
     * Then if this option is set, the respective file system controller
     * proceeds to update the federated file system anyway and finally throws
     * an {@link FsSyncWarningException} with a
     * {@link FsResourceOpenException} as its cause to indicate that any
     * subsequent operations on these resources will fail with an
     * {@link InputClosedException} because they have been forced to close.
     * <p>
     * If this option is not set however, the federated file system is
     * <em>not</em> updated, but instead
     * an {@link FsSyncException} with a {@link FsResourceOpenException} as
     * its cause is thrown to indicate
     * that the application must close all input resources first.
     */
    // TODO: Merge FORCE_CLOSE_INPUT and FORCE_CLOSE_OUTPUT into FORCE_CLOSE_IO.
    FORCE_CLOSE_INPUT,

    /**
     * Similar to {@link #WAIT_CLOSE_INPUT},
     * but applies to file system entry output resources (output streams etc.)
     * instead.
     */
    // TODO: Merge WAIT_CLOSE_INPUT and WAIT_CLOSE_OUTPUT into WAIT_CLOSE_IO.
    WAIT_CLOSE_OUTPUT,

    /**
     * Similar to {@link #FORCE_CLOSE_INPUT},
     * but applies to file system entry output resources (output streams etc.)
     * and may respectively throw an {@link OutputClosedException}.
     * <p>
     * If this option is set, then
     * {@link #FORCE_CLOSE_INPUT} must be set, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    // TODO: Merge FORCE_CLOSE_INPUT and FORCE_CLOSE_OUTPUT into FORCE_CLOSE_IO.
    FORCE_CLOSE_OUTPUT,

    /**
     * If this option is set, all pending changes are aborted.
     * This option is only meaningful immediately before the federated file
     * system itself gets deleted.
     */
    ABORT_CHANGES,

    /**
     * Suppose a controller for a federated file system has selectively cached
     * entry contents.
     * Then if this option is set when the file system gets synchronized,
     * the entry contents get cleared.
     * <p>
     * Note that this option may induce dead locks or even busy loops
     * when accessing nested archive files in different threads.
     *
     * @see <a href="http://java.net/jira/browse/TRUEZIP-268">#TRUEZIP-268</a>
     * @see <a href="http://java.net/jira/browse/TRUEZIP-269">#TRUEZIP-269</a>
     */
    CLEAR_CACHE,
}
