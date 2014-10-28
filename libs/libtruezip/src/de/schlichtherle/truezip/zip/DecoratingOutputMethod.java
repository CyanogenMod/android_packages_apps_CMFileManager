/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipException;

/**
 * A decorator for output methods.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
abstract class DecoratingOutputMethod implements OutputMethod {
    final OutputMethod delegate;

    DecoratingOutputMethod(final OutputMethod processor) {
        assert null != processor;
        this.delegate = processor;
    }

    @Override
    public void init(ZipEntry entry) throws ZipException {
        delegate.init(entry);
    }

    @Override
    public OutputStream start() throws IOException {
        return delegate.start();
    }

    @Override
    public void finish() throws IOException {
        delegate.finish();
    }
}