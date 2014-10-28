/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

/**
 * An output stream which updates a CRC-32 checksum.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
final class Crc32OutputStream extends CheckedOutputStream {
    Crc32OutputStream(OutputStream out) {
        super(out, new CRC32());
    }
}
