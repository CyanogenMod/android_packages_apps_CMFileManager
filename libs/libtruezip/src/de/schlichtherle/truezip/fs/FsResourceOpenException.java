/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Indicates that a call to {@link FsController#sync} cannot succeed because
 * some threads have unclosed I/O resources, e.g. streams or channels.
 * <p>
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as these I/O resources have been
 * closed and no other exceptional conditions apply.
 *
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
public final class FsResourceOpenException extends IOException {
    private static final long serialVersionUID = 1L;

    final int local, total;

    FsResourceOpenException(int total, int local) {
        super("Thread-local / total number of open I/O resources (streams, channels etc): %d / %d");
        this.local = local;
        this.total = total;
    }

    public int getLocal() { return local; }

    public int getTotal() { return total; }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), local, total);
    }
}
