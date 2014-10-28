/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output socket which obtains its delegate lazily and {@link #reset()}s it
 * upon any {@link Throwable}.
 *
 * @see    ClutchInputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public abstract class ClutchOutputSocket<E extends Entry>
extends DelegatingOutputSocket<E> {
    OutputSocket<? extends E> delegate;

    @Override
    protected final OutputSocket<? extends E> getDelegate() throws IOException {
        final OutputSocket<? extends E> os = delegate;
        return null != os ? os : (delegate = getLazyDelegate());
    };

    /**
     * Returns the delegate socket for lazy initialization.
     *
     * @return the delegate socket for lazy initialization.
     * @throws IOException on any I/O failure.
     */
    protected abstract OutputSocket<? extends E> getLazyDelegate()
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
    public OutputStream newOutputStream() throws IOException {
        try {
            return getBoundSocket().newOutputStream();
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
