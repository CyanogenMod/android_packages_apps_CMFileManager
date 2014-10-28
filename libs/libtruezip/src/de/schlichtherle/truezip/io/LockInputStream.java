/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.InputShop;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link InputShop}, via a {@link Lock} object.
 *
 * @see     LockOutputStream
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
public class LockInputStream extends DecoratingInputStream {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockInputStream(
            final InputStream in,
            final Lock lock) {
        super(in);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            return delegate.read();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            return delegate.read(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        lock.lock();
        try {
            return delegate.skip(n);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int available() throws IOException {
        lock.lock();
        try {
            return delegate.available();
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

    @Override
    public void mark(int readlimit) {
        lock.lock();
        try {
            delegate.mark(readlimit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() throws IOException {
        lock.lock();
        try {
            delegate.reset();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean markSupported() {
        lock.lock();
        try {
            return delegate.markSupported();
        } finally {
            lock.unlock();
        }
    }
}