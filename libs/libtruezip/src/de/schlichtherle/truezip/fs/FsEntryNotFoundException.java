/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Indicates that a file system entry does not exist or is not accessible.
 *
 * @author Christian Schlichtherle
 */
public final class FsEntryNotFoundException extends FileNotFoundException {
    private static final long serialVersionUID = 2972350932856838564L;

    private final FsPath path;

    public FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final String msg) {
        super(msg);
        this.path = model.getMountPoint().resolve(name);
    }

    public FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final IOException cause) {
        super(null != cause ? cause.toString() : null);
        super.initCause(cause);
        this.path = model.getMountPoint().resolve(name);
    }

    @Override
    public String getMessage() {
        final String msg = super.getMessage();
        return null != msg
                ? new StringBuilder(path.toString()).append(" (").append(msg).append(")").toString()
                : path.toString();
    }
}
