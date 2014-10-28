/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

/**
 * A provider for an I/O buffer pool.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface IOPoolProvider {

    /**
     * Returns the I/O buffer pool to use for allocating temporary I/O buffers.
     * <p>
     * Multiple invocations should return the same I/O buffer pool.
     * However, callers should cache the return value for subsequent use in
     * case it isn't always the same.
     *
     * @return The I/O buffer pool to use for allocating temporary I/O buffers.
     */
    IOPool<?> get();
}
