/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Indicates that a file system is a false positive file system and that this
 * exception may get cached until the federated (archive) file system gets
 * {@linkplain FsController#sync(de.schlichtherle.truezip.util.BitField) synced}
 * again.
 * <p>
 * This exception type is reserved for use by a
 * {@link FsController file system controller} in order to reroute file system
 * operations to the parent file system of a false positive federated (archive)
 * file system.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class FsPersistentFalsePositiveArchiveException
extends FsFalsePositiveArchiveException {

    FsPersistentFalsePositiveArchiveException(IOException cause) {
        super(cause);
    }
}
