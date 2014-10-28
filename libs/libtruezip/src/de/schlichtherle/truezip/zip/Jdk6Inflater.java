/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Fixes 32 bit field size in JDK 6 implementation of
 * {@link java.util.zip.Inflater}.
 *
 * @author  Christian Schlichtherle
 */
final class Jdk6Inflater extends Inflater {
    private long read = 0, written = 0;

    Jdk6Inflater(boolean nowrap) {
        super(nowrap);
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        super.setInput(b, off, len);
        read += len;
    }

    @Override
    public int inflate(byte[] b, int off, int len) throws DataFormatException {
        int ilen = super.inflate(b, off, len);
        written += ilen;
        return ilen;
    }

    @Override
    public long getBytesRead() {
        return read - getRemaining();
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