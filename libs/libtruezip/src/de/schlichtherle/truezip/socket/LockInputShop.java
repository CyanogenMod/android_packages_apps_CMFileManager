/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.LockInputStream;
import de.schlichtherle.truezip.rof.LockReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another input shop to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries.
 * @see    LockOutputShop
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public class LockInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private final Lock lock;

    /**
     * Constructs a new concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    public LockInputShop(InputShop<E> input) {
        this(input, new ReentrantLock());
    }

    /**
     * Constructs a new concurrent input shop.
     *
     * @param input the shop to decorate.
     * @param lock The lock to use.
     */
    public LockInputShop(
            final InputShop<E> input,
            final Lock lock) {
        super(input);
        if (null == (this.lock = lock))
            throw new NullPointerException();
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
    public E getEntry(String name) {
        lock.lock();
        try {
            return delegate.getEntry(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getSize() {
        lock.lock();
        try {
            return delegate.getSize();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("This returned iterator would not be thread-safe!");
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(LockInputShop.super.getInputSocket(name));
            }

            @Override
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final ReadOnlyFile rof;
                lock.lock();
                try {
                    rof = getBoundSocket().newReadOnlyFile();
                } finally {
                    lock.unlock();
                }
                return new LockReadOnlyFile(rof, lock);
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final InputStream in;
                lock.lock();
                try {
                    in = getBoundSocket().newInputStream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in, lock);
            }
        } // Input

        return new Input();
    }
}
