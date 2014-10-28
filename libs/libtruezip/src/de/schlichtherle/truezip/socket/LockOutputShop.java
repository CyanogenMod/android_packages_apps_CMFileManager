/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.LockOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another output shop to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries.
 * @see    LockInputShop
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public class LockOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private final Lock lock;

    /**
     * Constructs a new concurrent output shop.
     *
     * @param output the shop to decorate.
     */
    public LockOutputShop(OutputShop<E> output) {
        this(output, new ReentrantLock());
    }

    /**
     * Constructs a new concurrent output shop.
     *
     * @param output the shop to decorate.
     * @param lock The lock to use.
     */
    public LockOutputShop(
            final OutputShop<E> output,
            final Lock lock) {
        super(output);
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
        throw new UnsupportedOperationException("The returned iterator would not be thread-safe!");
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(LockOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return entry;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final OutputStream out;
                lock.lock();
                try {
                    out = getBoundSocket().newOutputStream();
                } finally {
                    lock.unlock();
                }
                return new LockOutputStream(out, lock);
            }
        } // Output

        return new Output();
    }
}
