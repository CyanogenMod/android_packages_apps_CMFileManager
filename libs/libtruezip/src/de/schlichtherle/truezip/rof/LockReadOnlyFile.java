/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.socket.InputShop;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link InputShop}, via a {@link Lock} object.
 *
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
public class LockReadOnlyFile extends DecoratingReadOnlyFile {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockReadOnlyFile(
            final ReadOnlyFile rof,
            final Lock lock) {
        super(rof);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    public long length() throws IOException {
        lock.lock();
        try {
            return delegate.length();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        lock.lock();
        try {
            return delegate.getFilePointer();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        lock.lock();
        try {
            delegate.seek(pos);
        } finally {
            lock.unlock();
        }
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
    public void readFully(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            delegate.readFully(b, off, len);
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