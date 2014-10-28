/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.EventObject;

/**
 * An archive file system event.
 *
 * @param  <E> The type of the archive entries.
 * @see    FsArchiveFileSystemTouchListener
 * @author Christian Schlichtherle
 */
final class FsArchiveFileSystemEvent<E extends FsArchiveEntry>
extends EventObject {
    private static final long serialVersionUID = 7205624082374036401L;

    /**
     * Constructs a new archive file system event.
     *
     * @param source the non-{@code null} archive file system source which
     *        caused this event.
     */
    FsArchiveFileSystemEvent(FsArchiveFileSystem<E> source) {
        super(source);
    }

    /**
     * Returns the non-{@code null} archive file system source which caused
     * this event.
     *
     * @return The non-{@code null} archive file system source which caused
     *         this event.
     */
    @Override
    @SuppressWarnings("unchecked")
    public FsArchiveFileSystem<E> getSource() {
        return (FsArchiveFileSystem<E>) source;
    }
}
