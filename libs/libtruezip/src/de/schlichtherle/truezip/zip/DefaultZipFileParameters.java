/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.nio.charset.Charset;

/**
 * The default implementation of {@link ZipFileParameters}.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
final class DefaultZipFileParameters
extends DefaultZipCharsetParameters
implements ZipFileParameters<ZipEntry> {

    private final boolean preambled, postambled;

    DefaultZipFileParameters(
            final Charset charset,
            final boolean preambled,
            final boolean postambled) {
        super(charset);
        this.preambled = preambled;
        this.postambled = postambled;
    }

    @Override
    public boolean getPreambled() {
        return preambled;
    }

    @Override
    public boolean getPostambled() {
        return postambled;
    }

    @Override
    public ZipEntry newEntry(String name) {
        return new ZipEntry(name);
    }
}