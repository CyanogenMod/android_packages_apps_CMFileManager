/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.nio.charset.Charset;

/**
 * Default implementation of {@link ZipCharsetParameters}.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
class DefaultZipCharsetParameters implements ZipCharsetParameters {

    private final Charset charset;

    DefaultZipCharsetParameters(final Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }
}