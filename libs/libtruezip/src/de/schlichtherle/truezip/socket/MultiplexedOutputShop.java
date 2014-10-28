/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.ALL_ACCESS_SET;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.entry.MutableEntry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import de.schlichtherle.truezip.util.JointIterator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decorates annother output shop to support a virtually unlimited number of
 * entries which may be written concurrently while actually at most one entry
 * is written concurrently to the decorated output shop.
 * If there is more than one entry to be written concurrently, the additional
 * entries are buffered to an I/O entry allocated from an I/O pool and copied
 * to the decorated output shop upon a call to their
 * {@link OutputStream#close()} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
public class MultiplexedOutputShop<E extends MutableEntry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private final IOPool<?> pool;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output archive.
     */
    private final Map<String, BufferedEntryOutputStream> buffers
            = new LinkedHashMap<String, BufferedEntryOutputStream>();

    /** @see #isBusy */
    private boolean busy;

    /**
     * Constructs a new multiplexed output shop.
     *
     * @param output the decorated output shop.
     * @param pool the pool for buffering entry data.
     */
    public MultiplexedOutputShop(
            final OutputShop<E> output,
            final IOPool<?> pool) {
        super(output);
        if (null == (this.pool = pool))
            throw new NullPointerException();
    }

    @Override
    public int getSize() {
        return delegate.getSize() + buffers.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new JointIterator<E>(
                delegate.iterator(),
                new BufferedEntriesIterator());
    }

    private class BufferedEntriesIterator implements Iterator<E> {
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public E next() {
            return i.next().getLocalTarget();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public E getEntry(String name) {
        final E entry = delegate.getEntry(name);
        if (null != entry) return entry;
        final BufferedEntryOutputStream out = buffers.get(name);
        return null == out ? null : out.getLocalTarget();
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(final E local) {
        if (null == local) throw new NullPointerException();
        final class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(MultiplexedOutputShop.super.getOutputSocket(local));
            }

            @Override
            public E getLocalTarget() throws IOException {
                return local;
            }

            @Override
            @SuppressWarnings("resource")
            public OutputStream newOutputStream() throws IOException {
                final OutputSocket<? extends E> output = getBoundSocket();
                return isBusy() ? new BufferedEntryOutputStream(output)
                                : new EntryOutputStream(output);
            }
        } // Output
        return new Output();
    }

    /**
     * Returns whether the container output archive is busy writing an archive
     * entry or not.
     *
     * @return Whether the container output archive is busy writing an archive
     *         entry or not.
     */
    public boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        if (isBusy())
            throw new IOException("This multiplexed output shop is still busy with writing a stream!");
        storeBuffers();
        assert buffers.isEmpty();
        delegate.close();
    }

    final void storeBuffers() throws IOException {
        if (isBusy()) return;
        final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                = SequentialIOExceptionBuilder.create(IOException.class, SequentialIOException.class);
        for (   final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
                i.hasNext(); ) {
            final BufferedEntryOutputStream out = i.next();
            try {
                if (out.storeBuffer()) i.remove();
            } catch (final InputException ex) {
                builder.warn(ex);
            } catch (final IOException ex) {
                throw builder.fail(ex);
            }
        }
        builder.check();
    }

    /** This entry output stream writes directly to this output shop. */
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        EntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            super(output.newOutputStream());
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                busy = false;
                delegate.close();
            }
            storeBuffers();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted unless this output shop is still busy.
     */
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final InputSocket<Entry> input;
        final OutputSocket<? extends E> output;
        final IOPool.Entry<?> buffer;
        boolean closed;

        BufferedEntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            super(null);
            // FC SUNT DRACONES!
            final E local = (this.output = output).getLocalTarget();
            final Entry _peer = output.getPeerTarget();
            final IOPool.Entry<?> buffer = this.buffer = pool.allocate();
            final Entry peer = null != _peer ? _peer : buffer;
            final class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() { super(buffer.getInputSocket()); }

                @Override
                public Entry getLocalTarget() {
                    return peer;
                }
            } // InputProxy
            try {
                this.input = new InputProxy();
                this.delegate = buffer.getOutputSocket().newOutputStream();
            } catch (final IOException ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    // Ignore
                }
                throw ex;
            }
            buffers.put(local.getName(), this);
        }

        E getLocalTarget() {
            try {
                return output.getLocalTarget();
            } catch (final IOException ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        public void close() throws IOException {
            final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                    = SequentialIOExceptionBuilder.create(IOException.class, SequentialIOException.class);
            if (!closed) {
                closed = true;
                try {
                    delegate.close();
                    final E local = output.getLocalTarget();
                    if (this == buffers.get(local.getName()))
                        updateProperties(local, input.getLocalTarget());
                    else
                        discardBuffer();
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            }
            try {
                storeBuffers();
            } catch (final IOException ex) {
                builder.warn(ex);
            }
            builder.check();
        }

        void updateProperties(final E local, final Entry peer) {
            for (final Access type : ALL_ACCESS_SET)
                if (UNKNOWN == local.getTime(type))
                    local.setTime(type, peer.getTime(type));
            // Never copy any but the DATA size!
            if (UNKNOWN == local.getSize(DATA))
                local.setSize(DATA, peer.getSize(DATA));
        }

        void discardBuffer() throws IOException {
            assert closed;
            buffer.release();
        }

        boolean storeBuffer() throws InputException, IOException {
            if (!closed || isBusy()) return false;
            IOSocket.copy(input, output);
            buffer.release();
            return true;
        }
    } // BufferedEntryOutputStream
}
