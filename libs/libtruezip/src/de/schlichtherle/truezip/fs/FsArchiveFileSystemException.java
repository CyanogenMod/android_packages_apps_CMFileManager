/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Thrown to indicate an exceptional condition in an {@link FsArchiveFileSystem}.
 *
 * @author Christian Schlichtherle
 */
public class FsArchiveFileSystemException extends IOException {
    private static final long serialVersionUID = 4652084652223428651L;

    /** The nullable entry path name. */
    private final String path;

    /** @since TrueZIP 7.5 */
    FsArchiveFileSystemException(FsEntryName name, String message) {
        this(name.toString(), message);
    }

    FsArchiveFileSystemException(String path, String message) {
        super(message);
        this.path = path;
    }

    FsArchiveFileSystemException(String path, Throwable cause) {
        super(cause);
        this.path = path;
    }

    FsArchiveFileSystemException(String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    @Override
    public String getMessage() {
        final String m = super.getMessage();
        return null == path
                ? m
                : new StringBuilder(path.isEmpty() ? "<file system root>" : path)
                    .append(" (")
                    .append(m)
                    .append(")")
                    .toString();
    }
}
