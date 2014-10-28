/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.io.InputClosedException;
import java.io.IOException;

/**
 * An abstract decorator which protects the decorated read-only-file from all
 * access unless it's {@linkplain #isOpen() open}.
 *
 * @author Christian Schlichtherle
 */
public abstract class DisconnectingReadOnlyFile extends DecoratingReadOnlyFile {

    protected DisconnectingReadOnlyFile(ReadOnlyFile rof) {
        super(rof);
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
    public long length() throws IOException {
        checkOpen();
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        checkOpen();
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        checkOpen();
        delegate.seek(pos);
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
    public abstract void close() throws IOException;
}
