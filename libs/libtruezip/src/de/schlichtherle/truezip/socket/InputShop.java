/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import java.io.Closeable;
import java.io.IOException;

/**
 * A closable input service.
 *
 * @param  <E> the type of the entries.
 * @see    OutputShop
 * @author Christian Schlichtherle
 */
//TODO: Consider renaming to InputArchive
public interface InputShop<E extends Entry> extends Closeable, InputService<E> {

    @Override
    void close() throws IOException;
}
