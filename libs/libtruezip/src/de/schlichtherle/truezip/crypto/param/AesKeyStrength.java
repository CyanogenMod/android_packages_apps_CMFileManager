/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.param;

/**
 * Enumerates the AES cipher key strenghts.
 *
 * @author  Christian Schlichtherle
 */
public enum AesKeyStrength implements KeyStrength {
    /** Enum identifier for a 128 bit AES cipher key. */
    BITS_128,

    /** Enum identifier for a 192 bit AES cipher key. */
    BITS_192,

    /** Enum identifier for a 256 bit AES cipher key. */
    BITS_256;

    /** Returns the key strength in bytes. */
    @Override
    public int getBytes() {
        return 16 + 8 * ordinal();
    }

    /** Returns the key strength in bits. */
    @Override
    public int getBits() {
        return 8 * getBytes();
    }

    @Override
    public String toString() {
        if (this.equals(BITS_128)) {
            return "128 bit: medium security / shortest runtime";
        }
        if (this.equals(BITS_192)) {
            return "192 bit: strong security / medium runtime";
        }
        return "256 bit: very strong security / longest runtime";
    }
}