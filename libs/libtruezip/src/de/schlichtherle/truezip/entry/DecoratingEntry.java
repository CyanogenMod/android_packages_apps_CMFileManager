/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

/**
 * An abstract decorator for an entry.
 *
 * @param  <E> the type of the decorated entries.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingEntry<E extends Entry>
implements Entry {

    /** The decorated entry. */
    protected final E delegate;

    /**
     * Constructs a new decorating file system entry.
     *
     * @param entry the decorated entry.
     */
    protected DecoratingEntry(final E entry) {
        if (null == entry)
            throw new NullPointerException();
        this.delegate = entry;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getSize(Size type) {
        return delegate.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return delegate.getTime(type);
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
