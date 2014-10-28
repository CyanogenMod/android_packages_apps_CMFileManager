/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input socket which obtains its delegate lazily and {@link #reset()}s it
 * upon any {@link Throwable}.
 *
 * @see    ClutchOutputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public abstract class ClutchInputSocket<E extends Entry>
extends DelegatingInputSocket<E> {
    InputSocket<? extends E> delegate;

    @Override
    protected final InputSocket<? extends E> getDelegate() throws IOException {
        final InputSocket<? extends E> is = delegate;
        return null != is ? is : (delegate = getLazyDelegate());
    };

    /**
     * Returns the delegate socket for lazy initialization.
     *
     * @return the delegate socket for lazy initialization.
     * @throws IOException on any I/O failure.
     */
    protected abstract InputSocket<? extends E> getLazyDelegate()
    throws IOException;

    @Override
    public E getLocalTarget() throws IOException {
        try {
            return getBoundSocket().getLocalTarget();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        try {
            return getBoundSocket().newReadOnlyFile();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    @Override
    public InputStream newInputStream() throws IOException {
        try {
            return getBoundSocket().newInputStream();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    private IOException reset(final Throwable ex) {
        reset();
        if (ex instanceof RuntimeException)
            throw (RuntimeException) ex;
        else if (ex instanceof Error)
            throw (Error) ex;
        return (IOException) ex;
    }

    protected final void reset() {
        this.delegate = null;
    }
}
