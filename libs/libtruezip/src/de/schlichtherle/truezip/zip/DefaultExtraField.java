/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import static java.lang.System.arraycopy;

/**
 * Default implementation for an Extra Field in a Local or Central Header of a
 * ZIP file.
 *
 * @author Christian Schlichtherle
 */
final class DefaultExtraField extends ExtraField {

    private final short headerId;
    private byte[] data;

    /**
     * Constructs a new extra field.
     *
     * @param  headerId an unsigned short integer (two bytes) indicating the
     *         type of the extra field.
     */
    DefaultExtraField(final int headerId) {
        assert UShort.check(headerId);
        this.headerId = (short) headerId;
    }

    @Override
    int getHeaderId() {
        return headerId & UShort.MAX_VALUE;
    }

    @Override
    int getDataSize() {
        final byte[] data = this.data;
        return null != data ? data.length : 0;
    }

    @Override
    void readFrom(final byte[] buf, final int off, final int len) {
        assert UShort.check(len);
        arraycopy(buf, off, this.data = new byte[len], 0, len);
    }

    @Override
    void writeTo(byte[] buf, int off) {
        final byte[] src = this.data;
        if (null != src) arraycopy(src, 0, buf, off, src.length);
    }
}
