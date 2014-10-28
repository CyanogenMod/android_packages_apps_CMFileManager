/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import java.util.zip.DeflaterOutputStream;

/**
 * A deflater output stream which uses a custom {@link Deflater} and provides
 * access to it.
 *
 * @author  Christian Schlichtherle
 */
final class ZipDeflaterOutputStream extends DeflaterOutputStream {

    private static final DeflaterFactory factory = new Jdk6DeflaterFactory();

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, factory.newDeflater(), size);
        def.setLevel(level);
    }

    Deflater getDeflater() {
        return def;
    }

    @Override
    public void close() throws IOException {
        assert false : "This method should never get called by the current implementation.";
        def.end();
        super.close();
    }

    /** A factory for {@link Deflater} objects. */
    private static class DeflaterFactory {
        protected Deflater newDeflater() {
            return new Deflater(DEFAULT_COMPRESSION, true);
        }
    }

    /** A factory for {@link Jdk6Deflater} objects. */
    private static final class Jdk6DeflaterFactory extends DeflaterFactory {
        @Override
        protected Deflater newDeflater() {
            return new Jdk6Deflater(DEFAULT_COMPRESSION, true);
        }
    }
}