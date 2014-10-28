/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.Link;
import java.io.IOException;

/**
 * Represents an operation on a chain of one or more archive entries.
 * The operation is commit by its {@link #commit} method and the head of the
 * chain can be obtained by its {@link #getTarget} method.
 * <p>
 * Note that the state of the archive file system will not change until
 * the {@link #commit} method is called!
 * <p>
 * Implementations do <em>not</em> need to be thread-safe.
 *
 * @param  <E> The type of the archive entries.
 * @see    FsArchiveFileSystem#mknod
 * @author Christian Schlichtherle
 */
interface FsArchiveFileSystemOperation<E extends FsArchiveEntry>
extends Link<FsCovariantEntry<E>> {

    /** Executes this archive file system operation. */
    void commit() throws IOException;
}
