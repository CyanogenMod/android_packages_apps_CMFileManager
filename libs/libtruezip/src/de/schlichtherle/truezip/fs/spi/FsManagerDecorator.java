/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.spi;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;

/**
 * An abstract locatable service for decorating file system managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsManagerDecorator {

    /**
     * Decorates the given file system manager.
     *
     * @param  manager the file system manager to decorate.
     * @return The decorated file system manager.
     */
    public abstract FsManager decorate(FsManager manager);

    /**
     * Returns a priority to help service locators to prioritize the services
     * provided by this object.
     * The decorators will be applied in ascending order of priority so that
     * the decorator with the greatest number becomes the head of the decorator
     * chain.
     * <p>
     * The implementation in the class {@link FsManagerDecorator} returns
     * zero.
     *
     * @return A priority to help service locators to prioritize the services
     *         provided by this object.
     */
    public int getPriority() { return 0; }

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
