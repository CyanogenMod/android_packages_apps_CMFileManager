/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.OutputShop;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link OutputShop}, via a {@link Lock} object.
 *
 * @see     LockInputStream
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
public class LockOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockOutputStream(
            final OutputStream out,
            final Lock lock) {
        super(out);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    public void write(int b) throws IOException {
        lock.lock();
        try {
            delegate.write(b);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            delegate.write(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        lock.lock();
        try {
            delegate.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            delegate.close();
        } finally {
            lock.unlock();
        }
    }
}