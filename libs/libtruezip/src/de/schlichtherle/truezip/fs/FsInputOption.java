/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Defines the options for input operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation and file system driver implementation
 * to define which options are supported and available.
 * If an option is not supported, it must get silently ignored.
 * If an option is not available or illegal, an {@link IOException} must get
 * thrown.
 *
 * @see     FsOutputOption
 * @see     FsInputOptions
 * @author  Christian Schlichtherle
 */
public enum FsInputOption {

    /**
     * Whether or not the entry data written by an output socket shall get
     * cached for subsequent access.
     * As a desired side effect, caching allows a file system controller to
     * {@link FsController#sync} the entry data to the backing storage
     * (e.g. a parent file system) while some client is still busy on reading
     * or writing the cached entry data.
     */
    CACHE
}