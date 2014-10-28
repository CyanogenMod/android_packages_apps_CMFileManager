/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator which filters another iterator by means of its
 * {@link #accept(Object)} method.
 *
 * @param   <T> The type of elements returned by this iterator.
 * @author  Christian Schlichtherle
 */
public abstract class FilteringIterator<T> implements Iterator<T> {
    private final Iterator<T> it;
    private Boolean hasNext;
    private T next;

    /**
     * Constructs a new filtering iterator which filters the given iterable.
     *
     * @param iterable the iterable to filter.
     */
    protected FilteringIterator(Iterable<T> iterable) {
        this(iterable.iterator());
    }

    /**
     * Constructs a new filtering iterator which filters the given iterator.
     *
     * @param iterator the iterator to filter.
     */
    protected FilteringIterator(final Iterator<T> iterator) {
        if (null == (this.it = iterator))
            throw new NullPointerException();
    }

    /**
     * Returns {@code true} if and only if this filtering iterator accepts the
     * given element.
     *
     * @param  element the element to test
     * @return {@code true} if and only if this filtering iterator accepts the
     *         given element.
     */
    protected abstract boolean accept(T element);

    @Override
    public boolean hasNext() {
        if (null != hasNext)
            return hasNext;
        while (it.hasNext())
            if (accept(next = it.next()))
                return hasNext = true;
        return hasNext = false;
    }

    @Override
    public T next() {
        if (!hasNext())
            throw new NoSuchElementException();
        hasNext = null; // consume
        return next;
    }

    @Override
    public void remove() {
        it.remove();
    }
}