/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An abstract decorator which protects the decorated stream from all access
 * unless it's {@linkplain #isOpen() open}.
 *
 * @see    DisconnectingOutputStream
 * @author Christian Schlichtherle
 */
public abstract class DisconnectingInputStream extends DecoratingInputStream {

    protected DisconnectingInputStream(InputStream in) {
        super(in);
    }

    public abstract boolean isOpen();

    /**
     * Throws an {@link InputClosedException} iff {@link #isOpen()} returns
     * {@code false}.
     *
     * @throws InputClosedException iff {@link #isOpen()} returns {@code false}.
     */
    protected final void checkOpen() throws InputClosedException {
        if (!isOpen()) throw new InputClosedException();
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkOpen();
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkOpen();
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkOpen();
        return delegate.available();
    }

    @Override
    public void mark(int readlimit) {
        if (isOpen()) delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        checkOpen();
        delegate.reset();
    }

    @Override
    public abstract void close() throws IOException;
}
