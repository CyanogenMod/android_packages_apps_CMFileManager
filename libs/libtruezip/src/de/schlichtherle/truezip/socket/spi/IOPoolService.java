/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket.spi;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * An abstract locatable service for an I/O buffer pool.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IOPoolLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 */
public abstract class IOPoolService implements IOPoolProvider {

    /**
     * Returns a priority to help the I/O pool service locator.
     * The greater number wins!
     *
     * @return {@code 0}, as by the implementation in the class
     *         {@link IOPoolService}.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}