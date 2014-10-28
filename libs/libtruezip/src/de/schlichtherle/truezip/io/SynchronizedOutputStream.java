/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.OutputShop;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorator which synchronizes all access to an {@link OutputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedInputStream
 * @deprecated Use {@link LockOutputStream} instead.
 * @author  Christian Schlichtherle
 */
@Deprecated
public class SynchronizedOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on. */
    protected final Object lock;

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out the output stream to wrap in this decorator.
     * @deprecated This class exists to control concurrent access to a
     *             protected resource, e.g. an {@link OutputShop}.
     *             So the lock should never be this object itself.
     */
    public SynchronizedOutputStream(OutputStream out) {
    	this(out, null);
    }

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedOutputStream(
            final OutputStream out,
            final Object lock) {
        super(out);
        this.lock = null != lock ? lock : this;
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (lock) {
            delegate.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            delegate.flush();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}