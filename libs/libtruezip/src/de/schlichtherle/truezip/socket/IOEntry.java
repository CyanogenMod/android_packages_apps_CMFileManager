/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

/**
 * An entry which provides I/O sockets.
 *
 * @author Christian Schlichtherle
 */
public interface IOEntry<E extends IOEntry<E>>
extends InputEntry<E>, OutputEntry<E> {
}
