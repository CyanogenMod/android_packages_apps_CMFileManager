/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An abstract decorator which protects the decorated stream from all access
 * unless it's {@linkplain #isOpen() open}.
 *
 * @see    DisconnectingInputStream
 * @author Christian Schlichtherle
 */
public abstract class DisconnectingOutputStream extends DecoratingOutputStream {

    protected DisconnectingOutputStream(OutputStream out) {
        super(out);
    }

    public abstract boolean isOpen();

    /**
     * Throws an {@link OutputClosedException} iff {@link #isOpen()} returns
     * {@code false}.
     *
     * @throws OutputClosedException iff {@link #isOpen()} returns {@code false}.
     */
    protected final void checkOpen() throws OutputClosedException {
        if (!isOpen()) throw new OutputClosedException();
    }

    @Override
    public void write(int b) throws IOException {
        checkOpen();
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkOpen();
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        delegate.flush();
    }

    @Override
    public abstract void close() throws IOException;
}
