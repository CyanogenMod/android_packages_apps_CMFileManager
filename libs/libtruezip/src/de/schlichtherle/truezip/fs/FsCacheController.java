/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.fs.FsOutputOption.EXCLUSIVE;
import static de.schlichtherle.truezip.fs.FsOutputOption.GROW;
import static de.schlichtherle.truezip.fs.FsSyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.fs.FsSyncOption.CLEAR_CACHE;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import static de.schlichtherle.truezip.socket.IOCache.Strategy.WRITE_BACK;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A selective cache for file system entries.
 * Decorating a file system controller with this class has the following
 * effects:
 * <ul>
 * <li>Caching and buffering for an entry needs to get activated by using the
 *     method
 *     {@link #getInputSocket input socket} with the input option
 *     {@link FsInputOption#CACHE} or the method
 *     {@link #getOutputSocket output socket} with the output option
 *     {@link FsOutputOption#CACHE}.
 * <li>Unless a write operation succeeds, upon each read operation the entry
 *     data gets copied from the backing store for buffering purposes only.
 * <li>Upon a successful write operation, the entry data gets cached for
 *     subsequent read operations until the file system gets
 *     {@link #sync synced} again.
 * <li>Entry data written to the cache is not written to the backing store
 *     until the file system gets {@link #sync synced} - this is a
 *     <i>write back</i> strategy.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the copied
 *     entry data.
 * </ul>
 * <p>
 * <strong>TO THE FUTURE ME:</strong>
 * FOR TRUEZIP 7.5, IT TOOK ME TWO MONTHS OF CONSECUTIVE CODING, TESTING,
 * DEBUGGING, ANALYSIS AND SWEATING TO GET THIS DAMN BEAST WORKING STRAIGHT!
 * DON'T EVEN THINK YOU COULD CHANGE A SINGLE CHARACTER IN THIS CODE AND EASILY
 * GET AWAY WITH IT!
 * <strong>YOU HAVE BEEN WARNED!</strong>
 * <p>
 * Well, if you really feel like changing something, run the integration test
 * suite at least ten times to make sure your changes really work - I mean it!
 *
 * @author Christian Schlichtherle
 */
final class FsCacheController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final Logger logger = Logger.getLogger(FsCacheController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private final IOPool<?> pool;

    private final Map<FsEntryName, EntryCache>
            caches = new HashMap<FsEntryName, EntryCache>();

    /**
     * Constructs a new file system cache controller.
     *
     * @param controller the decorated file system controller.
     * @param pool the pool of I/O buffers to hold the cached entry contents.
     */
    FsCacheController(
            final IOPool<?> pool,
            final FsController<? extends FsLockModel> controller) {
        super(controller);
        if (null == (this.pool = pool)) throw new NullPointerException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        /** This class requires ON-DEMAND LOOKUP of its delegate! */
        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsInputOption.CACHE))
                        return delegate.getInputSocket(name, options);
                    cache = new EntryCache(name);
                }
                return cache.getInputSocket(options);
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        /** This class requires ON-DEMAND LOOKUP of its delegate! */
        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsOutputOption.CACHE))
                        return delegate.getOutputSocket(name, options, template);
                    cache = new EntryCache(name);
                }
                return cache.getOutputSocket(options, template);
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final Entry template)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        delegate.mknod(name, type, options, template);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        delegate.unlink(name, options);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        syncCacheEntries(options);
        delegate.sync(options.clear(CLEAR_CACHE));
        if (caches.isEmpty()) setMounted(false);
    }

    private void syncCacheEntries(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        assert isWriteLockedByCurrentThread();
        // HC SVNT DRACONES!
        if (0 >= caches.size()) return;
        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        assert flush || clear;
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        for (   final Iterator<EntryCache> i = caches.values().iterator();
                i.hasNext(); ) {
            final EntryCache cache = i.next();
            if (flush) {
                try {
                    cache.flush();
                } catch (final IOException ex) {
                    throw builder.fail(new FsSyncException(getModel(), ex));
                }
            }
            if (clear) {
                i.remove();
                try {
                    cache.clear();
                } catch (final IOException ex) {
                    builder.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        }
        builder.check();
    }

    private enum SocketFactory {
        OIO() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryCache cache,
                    BitField<FsOutputOption> options,
                    Entry template) {
                return cache.new Output(options, template);
            }
        };

        abstract OutputSocket<?> newOutputSocket(
                EntryCache cache,
                BitField<FsOutputOption> options,
                Entry template);
    } // SocketFactory

    /** A cache for the contents of an individual archive entry. */
    private final class EntryCache {
        final FsEntryName name;
        final IOCache cache;

        EntryCache(final FsEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(FsCacheController.this.pool);
        }

        InputSocket<?> getInputSocket(BitField<FsInputOption> options) {
            return cache.configure(new Input(options)).getInputSocket();
        }

        OutputSocket<?> getOutputSocket(BitField<FsOutputOption> options,
                                        Entry template) {
            return SOCKET_FACTORY.newOutputSocket(this, options, template);
        }

        void flush() throws IOException {
            cache.flush();
        }

        void clear() throws IOException {
            cache.clear();
        }

        void register() {
            assert isWriteLockedByCurrentThread();
            caches.put(name, this);
        }

        /**
         * This class requires LAZY INITIALIZATION of its delegate, but NO
         * automatic decoupling on exceptions!
         */
        final class Input extends ClutchInputSocket<Entry> {
            final BitField<FsInputOption> options;

            Input(final BitField<FsInputOption> options) {
                this.options = options.clear(FsInputOption.CACHE); // consume
            }

            @Override
            protected InputSocket<? extends Entry> getLazyDelegate() {
                return delegate.getInputSocket(name, options);
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                // Bypass the super class implementation to keep the
                // socket even upon an exception!
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                return new Stream();
            }

            final class Stream extends DecoratingInputStream {
                Stream() throws IOException {
                    // Bypass the super class implementation to keep the
                    // socket even upon an exception!
                    //super(Input.super.newInputStream());
                    super(getBoundSocket().newInputStream());
                    assert isMounted();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                    register();
                }
            } // Stream
        } // Input

        /**
         * This class requires LAZY INITIALIZATION of its delegate, but NO
         * automatic decoupling on exceptions!
         */
        class Output extends ClutchOutputSocket<Entry> {
            final BitField<FsOutputOption> options;
            final Entry template;

            Output( final BitField<FsOutputOption> options,
                    final Entry template) {
                this.options = options.clear(FsOutputOption.CACHE); // consume
                this.template = template;
            }

            @Override
            protected OutputSocket<? extends Entry> getLazyDelegate() {
                return cache.configure( delegate.getOutputSocket(
                                            name,
                                            options.clear(EXCLUSIVE),
                                            template))
                            .getOutputSocket();
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                // Note that the super class implementation MUST get
                // bypassed because the socket MUST get kept even upon an
                // exception!
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                preOutput();
                return new Stream();
            }

            final class Stream extends DecoratingOutputStream {
                Stream() throws IOException {
                    // Note that the super class implementation MUST get
                    // bypassed because the socket MUST get kept even upon an
                    // exception!
                    //super(Output.super.newOutputStream());
                    super(getBoundSocket().newOutputStream());
                    register();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                    postOutput();
                }
            } // Stream

            void preOutput() throws IOException {
                mknod(options, template);
            }

            void postOutput() throws IOException {
                mknod(  options.clear(EXCLUSIVE),
                        null != template ? template : cache.getEntry());
                register();
            }

            void mknod( BitField<FsOutputOption> mknodOpts,
                        final Entry template)
            throws IOException {
                while (true) {
                    try {
                        delegate.mknod(name, FILE, mknodOpts, template);
                        break;
                    } catch (final FsNeedsSyncException mknodEx) {
                        // In this context, this exception means that the entry
                        // has already been written to the output archive for
                        // the target archive file.

                        // Pass on the exception if there is no means to
                        // resolve the issue locally, that is if we were asked
                        // to create the entry exclusively or this is a
                        // non-recursive file system operation.
                        if (mknodOpts.get(EXCLUSIVE))
                            throw mknodEx;
                        final BitField<FsSyncOption> syncOpts = FsSyncController.modify(SYNC);
                        if (SYNC == syncOpts)
                            throw mknodEx;

                        // Try to resolve the issue locally.
                        // Even if we were asked to create the entry
                        // EXCLUSIVEly, first we must try to get the cache in
                        // sync() with the virtual file system again and retry
                        // the mknod().
                        try {
                            delegate.sync(syncOpts);
                            //continue; // sync() succeeded, now repeat mknod()
                        } catch (final FsSyncException syncEx) {
                            // sync() failed, maybe just because the current
                            // thread has already acquired some open I/O
                            // resources for the same target archive file, e.g.
                            // an input stream for a copy operation and this
                            // is an artifact of an attempt to acquire the
                            // output stream for a child file system.
                            if (!(syncEx.getCause() instanceof FsResourceOpenException)) {
                                // Too bad, sync() failed because of a more
                                // serious issue than just some open resources.
                                // Let's rethrow the sync exception.
                                throw syncEx;
                            }

                            // OK, we couldn't sync() because the current
                            // thread has acquired open I/O resources for the
                            // same target archive file.
                            // Normally, we would be expected to rethrow the
                            // mknod exception to trigger another sync(), but
                            // this would fail for the same reason und create
                            // an endless loop, so we can't do this.
                            //throw mknodEx;

                            // Dito for mapping the exception.
                            //throw FsNeedsLockRetryException.get(getModel());

                            // Check if we can retry the mknod with GROW set.
                            final BitField<FsOutputOption> oldMknodOpts = mknodOpts;
                            mknodOpts = oldMknodOpts.set(GROW);
                            if (mknodOpts == oldMknodOpts) {
                                // Finally, the mknod failed because the entry
                                // has already been output to the target archive
                                // file - so what?!
                                // This should mark only a volatile issue because
                                // the next sync() will sort it out once all the
                                // I/O resources have been closed.
                                // Let's log the sync exception - mind that it has
                                // suppressed the mknod exception - and continue
                                // anyway...
                                logger.log(Level.FINE, "Ignoring an exception which should only mark "
                                        + "a volatile issue because\nthe next sync() could resolve "
                                        + "it once all I/O resources have been closed:", syncEx);
                                break;
                            }
                        }
                    }
                }
            }
        } // Output
    } // EntryCache
}
