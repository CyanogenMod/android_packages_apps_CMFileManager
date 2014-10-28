/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.Deflater;

/**
 * Fixes 32 bit field size in JDK 6 implementation of
 * {@link java.util.zip.Deflater}.
 *
 * @author  Christian Schlichtherle
 */
final class Jdk6Deflater extends Deflater {
    private long read = 0, written = 0;

    Jdk6Deflater(int level, boolean nowrap) {
        super(level, nowrap);
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        super.setInput(b, off, len);
        read += len;
    }

    @Override
    public int deflate(byte[] b, int off, int len) {
        int dlen = super.deflate(b, off, len);
        written += dlen;
        return dlen;
    }

    @Override
    public long getBytesRead() {
        return read;
    }

    @Override
    public long getBytesWritten() {
        return written;
    }

    @Override
    public void reset() {
        super.reset();
        read = written = 0;
    }
}