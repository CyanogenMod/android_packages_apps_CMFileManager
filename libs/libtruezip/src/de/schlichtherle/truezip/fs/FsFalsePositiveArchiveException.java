/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ControlFlowException;
import java.io.IOException;

/**
 * Indicates that a file system is a false positive file system.
 * <p>
 * This exception type is reserved for use by
 * {@link FsController file system controllers} in order to reroute file system
 * operations to the parent file system of a false positive federated (archive)
 * file system.
 *
 * @see    FsFalsePositiveArchiveController
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
class FsFalsePositiveArchiveException extends ControlFlowException {

    FsFalsePositiveArchiveException(final IOException cause) {
        super(cause);
        assert null != cause;
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
