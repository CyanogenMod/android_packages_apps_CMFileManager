/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

/**
 * The parameters of this interface are used with RAES <i>type 0</i> files.
 * Type 0 RAES files use password based encryption according to the
 * specifications in PKCS #5 V2.0 und PKCS #12 V1.0.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @see     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html">PKCS #5</a>
 * @see     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-12/index.html">PKCS #12</a>
 * @author  Christian Schlichtherle
 */
public interface Type0RaesParameters extends RaesParameters {

    /**
     * Returns the password to use for writing a RAES type 0 file.
     *
     * @return A clone of the char array holding the password to use
     *         for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    char[] getWritePassword() throws RaesKeyException;

    /**
     * Returns the password to use for reading a RAES type 0 file.
     * This method is called consecutively until either the returned password
     * is successfully validated or an exception is thrown.
     *
     * @param  invalid {@code true} iff a previous call to this method returned
     *         an invalid password.
     * @return A clone of the char array holding the password to use
     *         for reading a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    char[] getReadPassword(boolean invalid) throws RaesKeyException;

    /**
     * Returns the key strength to use for writing a RAES type 0 file.
     *
     * @return The key strength to use for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    KeyStrength getKeyStrength() throws RaesKeyException;

    /**
     * Sets the key strength obtained from reading a RAES type 0 file.
     *
     * @param  keyStrength the key strength obtained from reading a RAES type 0
     *         file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    void setKeyStrength(KeyStrength keyStrength) throws RaesKeyException;

    /**
     * Invalidate the current key
     */
    void invalidate();

    /** Enumerates the AES cipher key strenghts. */
    enum KeyStrength implements de.schlichtherle.truezip.crypto.param.KeyStrength {
        /** Enum identifier for a 128 bit AES cipher key. */
        BITS_128,

        /** Enum identifier for a 192 bit AES cipher key. */
        BITS_192,

        /** Enum identifier for a 256 bit AES cipher key. */
        BITS_256;

        @Override
        public int getBytes() {
            return 16 + 8 * ordinal();
        }

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
    } // KeyStrength
}