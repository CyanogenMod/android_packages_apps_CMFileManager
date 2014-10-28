/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;

/**
 * An entry which provides output sockets.
 *
 * @param   <E> the type of this output entry.
 * @see     InputEntry
 * @author  Christian Schlichtherle
 */
public interface OutputEntry<E extends OutputEntry<E>> extends Entry {

    /**
     * Returns an output socket for writing this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An output socket for writing this entry.
     */
    // TODO: Declare to return OutputSocket<? extends E>
    OutputSocket<E> getOutputSocket();
}
