/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorating output stream which saves the last {@link IOException}
 * in a {@linkplain #exception protected field} for later use.
 *
 * @since  TrueZIP 7.3.2
 * @author Christian Schlichtherle
 * @deprecated This class will be removed in TrueZIP 8.
 */
public class IOExceptionOutputStream extends DecoratingOutputStream {

    /** The nullable last I/O exception. */
    protected IOException exception;

    /**
     * Constructs a new I/O exception output stream.
     *
     * @param delegate the nullable output stream to decorate.
     */
    protected IOExceptionOutputStream(
            OutputStream delegate) {
        super(delegate);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            delegate.write(b);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            delegate.write(b, off, len);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            delegate.flush();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
}
