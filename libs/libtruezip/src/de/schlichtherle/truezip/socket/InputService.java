/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.EntryContainer;
import java.util.Iterator;

/**
 * A service for input sockets.
 *
 * @param  <E> the type of the entries.
 * @see    OutputService
 * @author Christian Schlichtherle
 */
public interface InputService<E extends Entry> extends EntryContainer<E> {

    /**
     * {@inheritDoc}
     * <p>
     * The iterator returned by this method must be unmodifiable.
     */
    @Override
    Iterator<E> iterator();

    /**
     * Returns an input socket for reading from the entry with the given name.
     *
     * @param  name an {@link Entry#getName() entry name}.
     * @return An input socket for reading from the entry with the given name.
     */
    // TODO: This should return InputSocket<E>.
    InputSocket<? extends E> getInputSocket(String name);
}
