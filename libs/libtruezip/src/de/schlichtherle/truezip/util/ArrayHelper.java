/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * Provides static utility methods for arrays.
 *
 * @author Christian Schlichtherle
 */
public class ArrayHelper {

    private ArrayHelper() {
    }

    /**
     * Compares {@code max} bytes at the specified offsets of the given
     * arrays.
     * If the remaining bytes at the given offset of any array is smaller than
     * {@code max} bytes, it must match the number of remaining bytes at the
     * given offset in the other array.
     */
    public static boolean equals(
            final byte[] b1,
            final int b1off,
            final byte[] b2,
            final int b2off,
            int max) {
        if (b1 == null)
            throw new NullPointerException("b1");
        if (b2 == null)
            throw new NullPointerException("b2");
        if (0 > b1off || b1off > b1.length)
            throw new IndexOutOfBoundsException("b1off = " + b1off + ": not in [0, " + b1.length + "[!");
        if (0 > b2off || b2off > b2.length)
            throw new IndexOutOfBoundsException("b2off = " + b2off + ": not in [0, " + b2.length + "[!");
        if (max < 1)
            throw new IllegalArgumentException("len = " + max + ": too small");

        final int b1rem = b1.length - b1off;
        final int b2rem = b2.length - b2off;
        if (max > b1rem) {
            max = b1rem;
            if (max != b2rem)
                return false;
        } else if (max > b2rem) {
            max = b2rem;
            if (max != b1rem)
                return false;
        }

        while (--max >= 0)
            if (b1[b1off + max] != b2[b2off + max])
                return false;

        return true;
    }
}