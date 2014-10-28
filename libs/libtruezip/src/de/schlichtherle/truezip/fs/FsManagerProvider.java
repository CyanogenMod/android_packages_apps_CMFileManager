/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

/**
 * A provider for the singleton file system manager.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 */
public interface FsManagerProvider {

    /**
     * Returns the file system manager.
     * <p>
     * Calling this method several times must return the <em>same</em> file
     * system manager in order to ensure integrity of the virtual file system
     * space.
     *
     * @return The file system manager.
     */
    FsManager get();
}
