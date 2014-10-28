/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.HashMaps;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Accounts for {@link Closeable} resources
 * ({@link InputStream}, {@link OutputStream} etc.) which are used in multiple
 * threads.
 * <p>
 * For synchronization, each accountant uses a lock which has to be provided
 * to its {@link #FsResourceAccountant constructor}.
 * In order to start accounting for a closeable resource,
 * call {@link #startAccountingFor(Closeable)}.
 * In order to stop accounting for a closeable resource,
 * call {@link #stopAccountingFor(Closeable)}.
 *
 * @see    FsResourceController
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
final class FsResourceAccountant {

    /**
     * The map of all accounted closeable resources.
     * The initial capacity for the hash map accounts for the number of
     * available processors, a 90% blocking factor for typical I/O and a 2/3
     * map resize threshold.
     */
    private static final ConcurrentMap<Closeable, Account> accounts;
    static {
        final int threads = Runtime.getRuntime().availableProcessors() * 10;
        final int initialCapacity = HashMaps.initialCapacity(threads);
        accounts = new ConcurrentHashMap<Closeable, Account>(
                initialCapacity, 0.75f, threads);
    }

    private final Lock lock;
    private final Condition condition;

    /**
     * Constructs a new resource accountant with the given lock.
     * You MUST MAKE SURE not to use two instances of this class which share
     * the same lock!
     * Otherwise {@link #waitOtherThreads} will not work as designed!
     *
     * @param lock the lock to use for accounting resources.
     *             Though not required by the use in this class, this
     *             parameter should normally be an instance of
     *             {@link ReentrantLock} because chances are that it gets
     *             locked recursively.
     */
    FsResourceAccountant(final Lock lock) {
        this.condition = (this.lock = lock).newCondition();
    }

    /**
     * Starts accounting for the given closeable resource.
     *
     * @param resource the closeable resource to start accounting for.
     */
    void startAccountingFor(final Closeable resource) {
        accounts.put(resource, new Account());
    }

    /**
     * Stops accounting for the given closeable resource.
     * This method should be called from the implementation of
     * {@link Closeable#close()} in the given closeable resource.
     *
     * @param resource the closeable resource to stop accounting for.
     */
    void stopAccountingFor(final Closeable resource) {
        if (null != accounts.remove(resource)) {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Waits until all closeable resources which have been started accounting
     * for by <em>other</em> threads get stopped accounting for or a timeout
     * occurs or the current thread gets interrupted, whatever happens first.
     * <p>
     * Waiting for such resources can get cancelled immediately by interrupting
     * the current thread.
     * Unless the number of closeable resources which have been accounted for
     * by <em>all</em> threads is zero, this will leave the interrupt status of
     * the current thread cleared.
     * If no such foreign resources exist, then interrupting the current thread
     * does not have any effect.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller has acquired the lock
     * provided to the constructor - use with care!
     * <p>
     * Note that this method WILL NOT WORK if any two instances of this class
     * share the same lock provided to their constructor!
     *
     * @param  timeout the number of milliseconds to await the closing of
     *         resources which have been accounted for by <em>other</em>
     *         threads once the lock has been acquired.
     *         If this is non-positive, then there is no timeout for waiting.
     */
    void awaitClosingOfOtherThreadsResources(final long timeout) {
        lock.lock();
        try {
            try {
                if (0 < timeout) {
                    long toWait = TimeUnit.MILLISECONDS.toNanos(timeout);
                    while (0 < toWait && resources().needsWaiting())
                        toWait = condition.awaitNanos(toWait);
                } else {
                    while (resources().needsWaiting())
                        condition.await();
                }
            } catch (InterruptedException cancel) {
                // Fix rare racing condition between Thread.interrupt() and
                // Condition.signalAll() events.
                if (0 == resources().total)
                    Thread.currentThread().interrupt();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of closeable resources which have been accounted for.
     * The first element contains the number of closeable resources which have
     * been created by the current thread (<i>local</i>).
     * The second element contains the number of closeable resources which have
     * been created by all threads (<i>total</i>).
     * Mind that this value may reduce concurrently, even while the lock is
     * held, so it should <i>not</i> get cached!
     *
     * @return The number of closeable resources which have been accounted for.
     */
    Resources resources() {
        final Thread currentThread = Thread.currentThread();
        int local = 0, total = 0;
        for (final Account account : accounts.values()) {
            if (account.getAccountant() == this) {
                if (account.owner == currentThread) local++;
                total++;
            }
        }
        return new Resources(local, total);
    }

    /**
     * For each accounted closeable resource,
     * stops accounting for it and closes it.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also locks the lock
     * provided to the constructor - use with care!
     */
    <X extends Exception> void
    closeAllResources(final ExceptionHandler<? super IOException, X> handler)
    throws X {
        assert null != handler;
        lock.lock();
        try {
            for (   final Iterator<Entry<Closeable, Account>>
                        i = accounts.entrySet().iterator();
                    i.hasNext(); ) {
                final Entry<Closeable, Account> entry = i.next();
                final Account account = entry.getValue();
                if (account.getAccountant() != this) continue;
                i.remove();
                final Closeable closeable = entry.getKey();
                try {
                    // This should trigger an attempt to remove the closeable
                    // from the map, but it can cause no
                    // ConcurrentModificationException because the entry is
                    // already removed and a ConcurrentHashMap doesn't do that
                    // anyway.
                    closeable.close();
                } catch (final IOException ex) {
                    handler.warn(ex); // may throw an exception!
                }
            }
        } finally {
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    private final class Account {
        final Thread owner = Thread.currentThread();

        FsResourceAccountant getAccountant() {
            return FsResourceAccountant.this;
        }
    } // Account

    static final class Resources {
        final int local, total;

        private Resources(final int local, final int total) {
            this.local = local;
            this.total = total;
        }

        boolean needsWaiting() { return local < total; }
    } // Resources
}
