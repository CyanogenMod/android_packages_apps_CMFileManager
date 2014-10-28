/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A decorating input stream which counts the number of bytes read or skipped!
 *
 * @author Christian Schlichtherle
 */
final class CountingInputStream extends DecoratingInputStream {

    /** The number of bytes read. */
    private long bytesRead;

    /**
     * Constructs a new counting input stream.
     *
     * @param in the decorated input stream.
     */
    CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
    throws IOException {
        final int read = delegate.read(b, off, len);
        if (read > 0)
            this.bytesRead += read;
        return read;
    }

    @Override
    public int read() throws IOException {
        final int read = delegate.read();
        if (read != -1)
            this.bytesRead++;
        return read;
    }

    @Override
    public long skip(final long n) throws IOException {
        final long skipped = delegate.skip(n);
        this.bytesRead += skipped;
        return skipped;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }
}
