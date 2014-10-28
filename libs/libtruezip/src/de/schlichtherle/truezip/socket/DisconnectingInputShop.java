/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DisconnectingInputStream;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.rof.DisconnectingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Decorates another input shop in order to disconnect any resources when this
 * input shop gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this shop,
 * including all sockets, streams etc. but excluding {@link #getInputSocket}
 * and all {@link #close()} methods, will throw an
 * {@link InputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingOutputShop
 * @author Christian Schlichtherle
 */
public class DisconnectingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting input shop.
     *
     * @param input the shop to decorate.
     */
    public DisconnectingInputShop(InputShop<E> input) {
        super(input);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpen() {
        if (isClosed()) throw new IllegalStateException(new InputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed()) throw new InputClosedException();
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
    public InputSocket<? extends E> getInputSocket(String name) {
        return SOCKET_FACTORY
                .newInputSocket(this, delegate.getInputSocket(name));
    }

    /**
     * Closes this input shop.
     * Subsequent calls to this method will just forward the call to the
     * decorated input shop.
     * Subsequent calls to any other method of this input shop will result in
     * an {@link InputClosedException}, even if the call to this method fails
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
            <E extends Entry> InputSocket<? extends E> newInputSocket(
                    DisconnectingInputShop<E> shop,
                    InputSocket<? extends E> input) {
                return shop.new Input(input);
            }
        };

        abstract <E extends Entry> InputSocket<? extends E> newInputSocket(
                DisconnectingInputShop<E> shop,
                InputSocket <? extends E> input);
    } // SocketFactory

    private class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends E> getBoundSocket() throws IOException {
            checkOpen();
            return getDelegate().bind(this);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new DisconnectingReadOnlyFileImpl(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new DisconnectingInputStreamImpl(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private final class DisconnectingReadOnlyFileImpl
    extends DisconnectingReadOnlyFile {

        DisconnectingReadOnlyFileImpl(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public boolean isOpen() {
            return !isClosed();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) delegate.close();
        }
    } // DisconnectingReadOnlyFileImpl

    private final class DisconnectingInputStreamImpl
    extends DisconnectingInputStream {

        DisconnectingInputStreamImpl(InputStream in) {
            super(in);
        }

        @Override
        public boolean isOpen() {
            return !isClosed();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) delegate.close();
        }
    } // DisconnectingInputStreamImpl
}
