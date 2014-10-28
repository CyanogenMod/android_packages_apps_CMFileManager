/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.*;

/**
 * Provides caching services for input and output sockets with the following
 * features:
 * <ul>
 * <li>Upon the first read operation, the entry data will be read from the
 *     backing store and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the entry data from the backing store again until
 *     the cache gets {@link #clear cleared}.
 * <li>At the discretion of the {@link Strategy}, entry data written to the
 *     cache may not be written to the backing store until the cache gets
 *     {@link #flush flushed}.
 * <li>After a write operation, the entry data will be stored in the cache
 *     for subsequent read operations until the cache gets
 *     {@link #clear cleared}.
 * <li>As a side effect, caching decouples the backing store from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the cached
 *     entry data.
 * </ul>
 * <p>
 * Note that you need to call `configure` before you can do any input or
 * output.
 *
 * @author Christian Schlichtherle
 */
public final class IOCache implements Flushable, Closeable {

    private static final SocketFactory FACTORY = SocketFactory.OIO;

    private final Strategy strategy;
    private final IOPool<?> pool;
    private InputSocket<?> input;
    private OutputSocket<?> output;
    private InputBufferPool inputBufferPool;
    private OutputBufferPool outputBufferPool;
    private Buffer buffer;

    /**
     * Constructs a new cache which applies the given caching strategy,
     * uses the given pool to allocate and release temporary I/O entries.
     * <p>
     * Note that you need to call {@link #configure(InputSocket)} before
     * you can do any input.
     * Likewise, you need to call {@link #configure(OutputSocket)} before
     * you can do any output.
     *
     * @param strategy the caching strategy.
     * @param pool the pool for allocating and releasing temporary I/O entries.
     */
    private IOCache(final Strategy strategy, final IOPool<?> pool) {
        if (null == (this.strategy = strategy)) throw new NullPointerException();
        if (null == (this.pool = pool)) throw new NullPointerException();
    }

    /**
     * Configures the input socket for reading the entry data from the
     * backing store.
     * This method needs to be called before any input can be done -
     * otherwise a {@link NullPointerException} will be thrown on the first
     * read attempt.
     * Note that calling this method does <em>not</em> {@link #clear() clear}
     * this cache.
     *
     * @param  input an input socket for reading the entry data from the
     *         backing store.
     * @return {@code this}
     */
    public IOCache configure(final InputSocket<?> input) {
        if (null == input) throw new NullPointerException();
        this.input = input;
        return this;
    }

    /**
     * Configures the output socket for writing the entry data to the
     * backing store.
     * This method needs to be called before any output can be done -
     * otherwise a {@link NullPointerException} will be thrown on the first
     * write attempt.
     * Note that calling this method does <em>not</em> {@link #flush() flush}
     * this cache.
     *
     * @param  output an output socket for writing the entry data to the
     *         backing store.
     * @return {@code this}
     */
    public IOCache configure(final OutputSocket<?> output) {
        if (null == output) throw new NullPointerException();
        this.output = output;
        return this;
    }

    /**
     * Writes the cached entry data to the backing store unless already done.
     * Whether or not this method needs to be called depends on the caching
     * strategy.
     * E.g. the caching strategy {@link Strategy#WRITE_THROUGH} writes any
     * changed entry data immediately, so calling this method has no effect.
     */
    @Override
    public void flush() throws IOException {
        final Buffer buffer = getBuffer();
        if (null != buffer) getOutputBufferPool().release(buffer);
    }

    /**
     * Clears the entry data from this cache without flushing it.
     *
     * @throws IOException on any I/O failure.
     */
    public void clear() throws IOException {
        setBuffer(null);
    }

    /**
     * {@linkplain #flush() Flushes} and {@linkplain #clear() clears}
     * the cached entry data.
     */
    @Override
    public void close() throws IOException {
        flush();
        clear();
    }

    public Entry getEntry() {
        Buffer buffer = getBuffer();
        return null == buffer ? null : buffer.data;
    }

    /**
     * Returns an input socket for reading the cached entry data.
     *
     * @return An input socket for reading the cached entry data.
     */
    public InputSocket<?> getInputSocket() { return new Input(); }

    /**
     * Returns an output socket for writing the cached entry data.
     *
     * @return An output socket for writing the cached entry data.
     */
    public OutputSocket<?> getOutputSocket() { return new Output(); }

    private InputBufferPool getInputBufferPool() {
        final InputBufferPool ibp = inputBufferPool;
        return null != ibp
                ? ibp
                : (inputBufferPool = strategy.newInputBufferPool(this));
    }

    private OutputBufferPool getOutputBufferPool() {
        final OutputBufferPool obp = outputBufferPool;
        return null != obp
                ? obp
                : (outputBufferPool = strategy.newOutputBufferPool(this));
    }

    private Buffer getBuffer() { return buffer; }

    private void setBuffer(final Buffer newBuffer)
    throws IOException {
        final Buffer oldBuffer = this.buffer;
        if (oldBuffer != newBuffer) {
            this.buffer = newBuffer;
            if (null != oldBuffer
                    && 0 == oldBuffer.writers
                    && 0 == oldBuffer.readers)
                oldBuffer.release();
        }
    }

    /** Provides different cache strategies. */
    public enum Strategy {

        /**
         * Any attempt to obtain an output socket will result in a
         * {@link NullPointerException}.
         */
        READ_ONLY {
            @Override
            IOCache.OutputBufferPool newOutputBufferPool(IOCache cache) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by {@link #getOutputSocket} gets closed.
         */
        WRITE_THROUGH {
            @Override
            IOCache.OutputBufferPool newOutputBufferPool(IOCache cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly {@link #flush flushed}.
         */
        WRITE_BACK {
            @Override
            IOCache.OutputBufferPool newOutputBufferPool(IOCache cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        /**
         * Returns a new cache.
         *
         * @param  pool the pool of temporary entries to cache the entry data.
         * @return A new cache.
         */
        public IOCache newCache(IOPool<?> pool) {
            return new IOCache(this, pool);
        }

        IOCache.InputBufferPool newInputBufferPool(IOCache cache) {
            return cache.new InputBufferPool();
        }

        abstract IOCache.OutputBufferPool newOutputBufferPool(IOCache cache);
    } // Strategy

    private final class Input extends DelegatingInputSocket<Entry> {
        Buffer buffer;

        @Override
        protected InputSocket<? extends Entry> getDelegate() throws IOException {
            return (buffer = getInputBufferPool().allocate()).getInputSocket();
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final Buffer b = buffer;
            return null != b ? b.data : new ProxyEntry(
                    input/*.bind(this)*/.getLocalTarget()); // do NOT bind!
        }
    } // Input

    private final class Output extends DelegatingOutputSocket<Entry> {
        Buffer buffer;

        @Override
        protected OutputSocket<? extends Entry> getDelegate() throws IOException {
            return (buffer = getOutputBufferPool().allocate()).getOutputSocket();
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final Buffer b = buffer;
            return null != b ? b.data : new ProxyEntry(
                    output/*.bind(this)*/.getLocalTarget()); // do NOT bind!
        }
    } // Output

    /** Used to proxy the backing store entries. */
    private static class ProxyEntry extends DecoratingEntry<Entry> {
        ProxyEntry(Entry entry) { super(entry); }
    } // Proxy

    private final class InputBufferPool implements Pool<Buffer, IOException> {
        @Override public Buffer allocate() throws IOException {
            Buffer buffer = getBuffer();
            if (null == buffer) {
                buffer = new Buffer();
                try {
                    IOSocket.copy(input, buffer.data.getOutputSocket());
                } catch (IOException ex) {
                    buffer.release();
                    throw ex;
                }
                setBuffer(buffer);
            }
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
            buffer.readers++;
            return buffer;
        }

        @Override public void release(final Buffer buffer) throws IOException {
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
            if (0 < buffer.readers) {
                buffer.readers--;
                if (0 == buffer.readers && 0 == buffer.writers &&
                        getBuffer() != buffer)
                    buffer.release();
            }
        }
    } // InputBufferPool

    private final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override public void release(Buffer buffer) throws IOException {
            if (0 != buffer.writers) super.release(buffer);
        }
    } // WriteThroughOutputBufferPool

    private final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override public void release(final Buffer buffer) throws IOException {
            if (0 != buffer.writers) {
                if (getBuffer() != buffer)  setBuffer(buffer);
                else                        super.release(buffer);
            }
        }
    } // WriteBackOutputBufferPool

    private abstract class OutputBufferPool
    implements Pool<Buffer, IOException> {
        @Override public Buffer allocate() throws IOException {
            final Buffer buffer = new Buffer();
            assert 0 == buffer.readers;
            buffer.writers = 1;
            return buffer;
        }

        @Override public void release(final Buffer buffer) throws IOException {
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.readers;
            buffer.writers = 0;
            try { IOSocket.copy(buffer.data.getInputSocket(), output); }
            finally { setBuffer(buffer); }
        }
    } // OutputBufferPool

    private enum SocketFactory {
        OIO() {
            @Override InputSocket<?> newInputSocket(Buffer buffer) {
                return buffer.new Input();
            }

            @Override OutputSocket<?> newOutputSocket(Buffer buffer) {
                return buffer.new Output();
            }
        };

        abstract InputSocket<?> newInputSocket(Buffer buffer);
        abstract OutputSocket<?> newOutputSocket(Buffer buffer);
    } // SocketFactory

    /** A buffer for the contents of the cache. */
    private final class Buffer {

        final IOPool.Entry<?> data;

        int readers, writers; // max one writer!

        Buffer() throws IOException { data = pool.allocate(); }

        InputSocket<?> getInputSocket() {
            return FACTORY.newInputSocket(this);
        }

        OutputSocket<?> getOutputSocket() {
            return FACTORY.newOutputSocket(this);
        }

        void release() throws IOException {
            assert 0 == writers;
            assert 0 == readers;
            data.release();
        }

        class Input extends DecoratingInputSocket<Entry> {

            Input() { super(Buffer.this.data.getInputSocket()); }

            @Override public final ReadOnlyFile newReadOnlyFile()
            throws IOException {
                class File extends DecoratingReadOnlyFile {

                    boolean closed;

                    File() throws IOException {
                        super(getBoundSocket().newReadOnlyFile());
                    }

                    @Override public void close() throws IOException {
                        if (closed) return;
                        delegate.close();
                        getInputBufferPool().release(Buffer.this);
                        closed = true;
                    }
                } // File

                return new File();
            }

            @Override public final InputStream newInputStream()
            throws IOException {
                class Stream extends DecoratingInputStream {

                    boolean closed;

                    Stream() throws IOException {
                        super(getBoundSocket().newInputStream());
                    }

                    @Override public void close() throws IOException {
                        if (closed) return;
                        delegate.close();
                        getInputBufferPool().release(Buffer.this);
                        closed = true;
                    }
                } // Stream

                return new Stream();
            }
        } // Input

        class Output extends DecoratingOutputSocket<Entry> {

            Output() { super(Buffer.this.data.getOutputSocket()); }

            @Override public final OutputStream newOutputStream()
            throws IOException {
                class Stream extends DecoratingOutputStream {

                    boolean closed;

                    Stream() throws IOException {
                        super(getBoundSocket().newOutputStream());
                    }

                    @Override public void close() throws IOException {
                        if (closed) return;
                        delegate.close();
                        getOutputBufferPool().release(Buffer.this);
                        closed = true;
                    }
                } // Stream

                return new Stream();
            }
        } // Output
    } // Buffer
}
