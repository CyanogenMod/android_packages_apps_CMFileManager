/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * An inflater input stream which uses a custom {@link Inflater} and provides
 * access to it.
 *
 * @author  Christian Schlichtherle
 */
final class ZipInflaterInputStream extends InflaterInputStream {

    private static final InflaterFactory factory = new Jdk6InflaterFactory();

    ZipInflaterInputStream(DummyByteInputStream in, int size) {
        super(in, factory.newInflater(), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    public void close() throws IOException {
        super.close();
        inf.end();
    }

    /** A factory for {@link Inflater} objects. */
    private static class InflaterFactory {
        protected Inflater newInflater() {
            return new Inflater(true);
        }
    }

    /** A factory for {@link Jdk6Inflater} objects. */
    private static final class Jdk6InflaterFactory extends InflaterFactory {
        @Override
        protected Inflater newInflater() {
            return new Jdk6Inflater(true);
        }
    }
}