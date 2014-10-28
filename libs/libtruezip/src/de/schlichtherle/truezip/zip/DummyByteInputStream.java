/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import java.io.IOException;

/**
 * A read only file input stream which adds a dummy zero byte to the end of
 * the input in order to support {@link ZipInflaterInputStream}.
 *
 * @author  Christian Schlichtherle
 */
final class DummyByteInputStream extends ReadOnlyFileInputStream {
    private boolean added;

    DummyByteInputStream(ReadOnlyFile rof) {
        super(rof);
    }

    @Override
    public int read() throws IOException {
        final int read = rof.read();
        if (read < 0 && !added) {
            added = true;
            return 0;
        }
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len) throws IOException {
        if (0 == len)
            return 0;
        final int read = rof.read(buf, off, len);
        if (read < len && !added) {
            added = true;
            if (read < 0) {
                buf[0] = 0;
                return 1;
            } else {
                buf[read] = 0;
                return read + 1;
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        int available = super.available();
        return added || available >= Integer.MAX_VALUE ? available : available + 1;
    }
}