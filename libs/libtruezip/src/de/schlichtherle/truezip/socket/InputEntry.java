/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;

/**
 * An entry which provides input sockets.
 *
 * @param  <E> the type of this input entry.
 * @see    OutputEntry
 * @author Christian Schlichtherle
 */
public interface InputEntry<E extends InputEntry<E>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    // TODO: Declare to return InputSocket<? extends E>
    InputSocket<E> getInputSocket();
}
