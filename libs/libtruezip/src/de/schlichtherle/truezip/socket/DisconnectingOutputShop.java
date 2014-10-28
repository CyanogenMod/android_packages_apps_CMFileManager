/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DisconnectingOutputStream;
import de.schlichtherle.truezip.io.OutputClosedException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Decorates another output shop in order to disconnect any resources when this
 * output shop gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this shop,
 * including all sockets, streams etc. but excluding {@link #getOutputSocket}
 * and all {@link #close()} methods, will throw an
 * {@link OutputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingInputShop
 * @author Christian Schlichtherle
 */
public class DisconnectingOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting output shop.
     *
     * @param output the shop to decorate.
     */
    public DisconnectingOutputShop(OutputShop<E> output) {
        super(output);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpen() {
        if (isClosed()) throw new IllegalStateException(new OutputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed()) throw new OutputClosedException();
    }

    @Override
    public int getSize() {
        assertOpen();
        return delegate.getSize();
    }

    @Override
    public Iterator<E> iterator() {
        assertOpen();
        return delegate.iterator();
    }

    @Override
    public E getEntry(String name) {
        assertOpen();
        return delegate.getEntry(name);
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(E entry) {
        return SOCKET_FACTORY
                .newOutputSocket(this, delegate.getOutputSocket(entry));
    }

    /**
     * Closes this output shop.
     * Subsequent calls to this method will just forward the call to the
     * decorated output shop.
     * Subsequent calls to any other method of this output shop will result in
     * an {@link OutputClosedException}, even if the call to this method fails
     * with an {@link IOException}.
     *
     * @throws IOException on any I/O failure.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        delegate.close();
    }

    private enum SocketFactory {
        OIO() {
            @Override
            <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                    DisconnectingOutputShop<E> shop,
                    OutputSocket<? extends E> output) {
                return shop.new Output(output);
            }
        };

        abstract <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                DisconnectingOutputShop<E> shop,
                OutputSocket <? extends E> output);
    } // SocketFactory

    private class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends E> getBoundSocket() throws IOException {
            checkOpen();
            return getDelegate().bind(this);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new DisconnectingOutputStreamImpl(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class DisconnectingOutputStreamImpl
    extends DisconnectingOutputStream {

        DisconnectingOutputStreamImpl(OutputStream out) {
            super(out);
        }

        @Override
        public boolean isOpen() {
            return !isClosed();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) delegate.close();
        }
    } // DisconnectingOutputStreamImpl
}
