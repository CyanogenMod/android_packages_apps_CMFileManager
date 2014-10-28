/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.DecoratingEntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import java.io.IOException;

/**
 * An abstract decorator for an input shop.
 *
 * @param  <E> the type of the entries served by the decorated input shop.
 * @param  <I> the type of the decorated input shop.
 * @see    DecoratingOutputShop
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputShop<E extends Entry, I extends InputShop<E>>
extends DecoratingEntryContainer<E, I>
implements InputShop<E> {

    protected DecoratingInputShop(final I delegate) {
        super(delegate);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        return delegate.getInputSocket(name);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
