/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Defines options for output operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation and file system driver implementation
 * to define which options are supported and available.
 * If an option is not supported, it must get silently ignored.
 * If an option is not available or illegal, an {@link IOException} must get
 * thrown.
 *
 * @see     FsInputOption
 * @see     FsOutputOptions
 * @author  Christian Schlichtherle
 */
public enum FsOutputOption {

    /**
     * Whether or not the entry data read by an input socket shall get
     * cached for subsequent access.
     * As a desired side effect, caching allows a file system controller to
     * {@link FsController#sync} the entry data to the backing storage
     * (e.g. a parent file system) while some client is still busy on reading
     * or writing the cached entry data.
     */
    CACHE,

    /**
     * Whether or not any missing parent directory entries shall get created
     * automatically.
     */
    CREATE_PARENTS,

    /**
     * Whether or not the new entry contents shall get appended to the existing
     * entry contents rather than replacing them entirely.
     */
    APPEND,

    /** Whether or not an entry must be exclusively created. */
    EXCLUSIVE,

    /**
     * Expresses a preference to store an entry uncompressed within its archive.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     *
     * @since TrueZIP 7.1
     */
    STORE,

    /**
     * Expresses a preference to compress an entry within its archive.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     *
     * @since TrueZIP 7.1
     */
    COMPRESS,

    /**
     * Expresses a preference to allow an archive file to grow by appending any
     * new or updated archive entry contents or meta data to its end.
     * Setting this option may produce redundant data in the resulting archive
     * file.
     * However, it may yield much better performance if the number and contents
     * of the archive entries to create or update are rather small compared to
     * the total size of the resulting archive file.
     * <p>
     * This option is the equivalent to a multi-session disc (CD, DVD etc.)
     * for archive files.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     *
     * @since TrueZIP 7.3
     */
    GROW,

    /**
     * Expresses a preference to encrypt archive entries when writing them to
     * an archive file.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     *
     * @since TrueZIP 7.3
     */
    ENCRYPT,
}