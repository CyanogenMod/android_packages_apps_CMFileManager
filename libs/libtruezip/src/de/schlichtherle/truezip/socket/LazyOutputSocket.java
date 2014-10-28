/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A lazy output socket provides proxy output streams which acquire their
 * underlying local target upon the first read access.
 *
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @see    LazyInputSocket
 * @author Christian Schlichtherle
 * @deprecated This class will be removed in TrueZIP 8.
 */
@Deprecated
public final class LazyOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    public LazyOutputSocket(OutputSocket<? extends E> output) {
        super(output);
    }

    /**
     * Returns a proxy output stream which acquires its underlying output
     * stream upon the first read access.
     *
     * @return A proxy output stream which acquires its underlying output
     *         stream upon the first write access.
     */
    @Override
    public OutputStream newOutputStream() {
        return new ProxyOutputStream();
    }

    private class ProxyOutputStream extends DecoratingOutputStream {
        ProxyOutputStream() {
            super(null);
        }

        OutputStream getDelegate() throws IOException {
            final OutputStream out = delegate;
            return null != out
                    ? out
                    : (delegate = getBoundSocket().newOutputStream());
        }

        @Override
        public void write(int b) throws IOException {
            getDelegate().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            getDelegate().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            getDelegate().flush();
        }

        @Override
        public void close() throws IOException {
            final OutputStream out = delegate;
            if (null != out)
                out.close();
        }
    } // ProxyOutputStream
}
