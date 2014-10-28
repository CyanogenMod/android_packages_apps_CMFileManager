/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.param;

/**
 * Defines the key strengths for a cipher.
 * <p>
 * Implementations must be thread-safe!
 *
 * @author  Christian Schlichtherle
 */
public interface KeyStrength {

    /**
     * Returns the index of the key strength.
     *
     * @return The index of the key strength.
     */
    int ordinal();

    /**
     * Returns the key strength in bits.
     *
     * @return The key strength in bits.
     */
    int getBits();

    /**
     * Returns the key strength in bytes.
     *
     * @return The key strength in bytes.
     */
    int getBytes();

    /**
     * Returns a localized display string representing this key strength.
     *
     * @return A localized display string representing this key strength.
     */
    @Override
    String toString();
}