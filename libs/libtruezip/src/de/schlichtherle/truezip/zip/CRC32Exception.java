/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.ZipException;

/**
 * Thrown to indicate a CRC32 mismatch between the declared value in the
 * Central File Header and the Data Descriptor or between the declared value
 * and the computed value from the decompressed data.
 * The prior case is detected on the call to {@link ZipFile#getCheckedInputStream},
 * whereas the latter case is detected when the input stream returned by this
 * method gets closed.
 * <p>
 * The exception's detail message is the name of the ZIP entry.
 *
 * @author  Christian Schlichtherle
 */
public class CRC32Exception extends ZipException {
    private static final long serialVersionUID = 1656298435298526391L;

    private final long expected, actual;

    /**
     * Creates a new instance of {@code CRC32Exception} where the
     * given entry name forms part of the detail message.
     *
     * @param name the entry name.
     * @param expected the declared CRC32 value.
     * @param actual the actual CRC32 value;
     * @see   #getExpectedCrc
     * @see   #getActualCrc
     */
    CRC32Exception(final String name, final long expected, final long actual) {
        super(name
                + " (expected CRC32 value 0x"
                + Long.toHexString(expected)
                + ", but is actually 0x"
                + Long.toHexString(actual)
                + ")");
        assert expected != actual;
        this.expected = expected;
        this.actual = actual;
    }

    /**
     * Returns the CRC32 value which has been expected for the ZIP entry.
     */
    public long getExpectedCrc() {
        return expected;
    }

    /**
     * Returns the CRC32 value which has actually been found for the ZIP entry.
     */
    public long getActualCrc() {
        return actual;
    }
}