/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket.spi;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * An abstract locatable service for decorating I/O buffer pools.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IOPoolLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class IOPoolDecorator {

    /**
     * Decorates the given I/O buffer pool.
     *
     * @param  <B> the type of the I/O buffers managed by the pool.
     * @param  pool the I/O buffer pool to decorate.
     * @return The decorated I/O buffer pool.
     */
    public abstract <B extends IOPool.Entry<B>> IOPool<B> decorate(IOPool<B> pool);

    /**
     * Returns a priority to help service locators to prioritize the services
     * provided by this object.
     * The decorators will be applied in ascending order of priority so that
     * the decorator with the greatest number becomes the head of the decorator
     * chain.
     * <p>
     * The implementation in the class {@link IOPoolDecorator} returns zero.
     *
     * @return A priority to help service locators to prioritize the services
     *         provided by this object.
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
