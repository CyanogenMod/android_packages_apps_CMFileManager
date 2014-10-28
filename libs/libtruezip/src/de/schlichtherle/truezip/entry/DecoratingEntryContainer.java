/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import java.util.Iterator;

/**
 * An abstract decorator for an entry container.
 *
 * @param  <E> the type of the entries in the container.
 * @param  <C> the type of the entry container.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingEntryContainer<
        E extends Entry,
        C extends EntryContainer<E>>
implements EntryContainer<E> {

    /** The decorated entry container. */
    protected final C delegate;

    /**
     * Constructs a new filter entry container.
     *
     * @param  delegate the entry container to decorate.
     */
    protected DecoratingEntryContainer(final C delegate) {
        if (null == (this.delegate = delegate))
            throw new NullPointerException();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public E getEntry(String name) {
        return delegate.getEntry(name);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
