/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Provides static utility methods for reading and writing integer values in
 * little endian format from or to byte arrays.
 *
 * @author  Christian Schlichtherle
 */
final class LittleEndian {

    /** This class cannot get instantiated. */
    private LittleEndian() {
    }

    /**
     * Reads a signed byte integer value from the byte array
     * {@code buf} at the offset {@code off}.
     *
     * @param  buf The byte array to read the signed byte integer value from.
     * @param  off The zero based offset in the byte array where the signed
     *         byte integer value is read from.
     * @return The signed byte integer value read from the byte array.
     */
    static byte readByte(final byte[] buf, final int off) {
        return buf[off];
    }

    /**
     * Reads an unsigned byte integer value from the byte array
     * {@code buf} at the offset {@code off}.
     * Note that it is <em>not</em> necessary to check the return value with
     * {@link UByte#check}.
     *
     * @param  buf The byte array to read the unsigned byte integer value from.
     * @param  off The zero based offset in the byte array where the unsigned
     *         byte integer value is read from.
     * @return The unsigned byte integer value read from the byte array.
     *         Java does not provide {@code unsigned short} as a primitive
     *         type, hence an {@code int} is returned which's three most
     *         significant bytes are zero.
     */
    static int readUByte(final byte[] buf, final int off) {
        return buf[off] & UByte.MAX_VALUE;
    }

    /**
     * Reads a signed short integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as two bytes, low byte first.
     *
     * @param  buf The byte array to read the signed short integer value from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the signed short integer value is read from.
     * @return The signed short integer value read from the byte array.
     */
    static int readShort(final byte[] buf, final int off) {
        return (buf[off + 1] << 8) | (buf[off] & UByte.MAX_VALUE);
    }

    /**
     * Reads an unsigned short integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as two bytes, low byte first.
     * Note that it is <em>not</em> necessary to check the return value with
     * {@link UShort#check}.
     *
     * @param  buf The byte array to read the unsigned short integer value from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the unsigned short integer value is read from.
     * @return The unsigned short integer value read from the byte array.
     *         Java does not provide {@code unsigned short} as a primitive
     *         type, hence an {@code int} is returned which's two most
     *         significant bytes are zero.
     */
    static int readUShort(final byte[] buf, final int off) {
        return ((buf[off + 1] & UByte.MAX_VALUE) << 8) | (buf[off] & UByte.MAX_VALUE);
    }

    /**
     * Reads a signed integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as four bytes, low byte first.
     *
     * @param  buf The byte array to read the signed integer value from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the signed integer value is read from.
     * @return The signed integer value read from the byte array.
     */
    static int readInt(final byte[] buf, int off) {
        off += 3;
        int i = buf[off--]; // expands sign
        i <<= 8;
        i |= buf[off--] & UByte.MAX_VALUE;
        i <<= 8;
        i |= buf[off--] & UByte.MAX_VALUE;
        i <<= 8;
        i |= buf[off] & UByte.MAX_VALUE;
        return i;
    }

    /**
     * Reads an unsigned integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as four bytes, low byte first.
     * Note that it is <em>not</em> necessary to check the return value with
     * {@link UInt#check}.
     *
     * @param  buf The byte array to read the unsigned integer value from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the unsigned integer value is read from.
     * @return The unsigned integer value read from the byte array.
     *         Java does not provide {@code unsigned int} as a primitive
     *         type, hence a {@code long} is returned which's four most
     *         significant bytes are zero.
     */
    static long readUInt(final byte[] buf, int off) {
        return readInt(buf, off) & UInt.MAX_VALUE;
    }

    /**
     * Reads a (signed) long integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as eight bytes, low byte first.
     *
     * @param  buf The byte array to read the signed long integer value from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the (signed) long integer value is read from.
     * @return The (signed) long integer value read from the byte array.
     */
    static long readLong(final byte[] buf, int off) {
        off += 7;
        long l = buf[off--]; // expands sign
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off--] & UByte.MAX_VALUE;
        l <<= 8;
        l |= buf[off] & UByte.MAX_VALUE;
        return l;
    }

    /**
     * Writes the integer value {@code b} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as one byte.
     * The most significant three bytes of the integer value are ignored.
     *
     * @param b The integer value to be written.
     * @param buf The byte array to write the integer value to.
     * @param off The zero based offset in the byte array where the byte
     *        of the integer value is written to.
     */
    static void writeByte(int b, byte[] buf, int off) {
        buf[off] = (byte) b;
    }

    /**
     * Writes the integer value {@code s} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as two bytes, low byte first.
     * The most significant two bytes of the integer value are ignored.
     *
     * @param s The integer value to be written.
     * @param buf The byte array to write the integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the integer value is written to.
     */
    static void writeShort(int s, final byte[] buf, final int off) {
        buf[off] = (byte) s;
        s >>= 8;
        buf[off + 1] = (byte) s;
    }

    /**
     * Writes the integer value {@code i} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as four bytes, low byte first.
     *
     * @param i The integer value to be written.
     * @param buf The byte array to write the integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the integer value is written to.
     */
    static void writeInt(int i, final byte[] buf, final int off) {
        buf[off] = (byte) i;
        i >>= 8;
        buf[off + 1] = (byte) i;
        i >>= 8;
        buf[off + 2] = (byte) i;
        i >>= 8;
        buf[off + 3] = (byte) i;
    }

    /**
     * Writes the long integer value {@code l} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as eight bytes, low byte first.
     *
     * @param l The long integer value to be written.
     * @param buf The byte array to write the long integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the long integer value is written to.
     */
    static void writeLong(long l, final byte[] buf, final int off) {
        buf[off] = (byte) l;
        l >>= 8;
        buf[off + 1] = (byte) l;
        l >>= 8;
        buf[off + 2] = (byte) l;
        l >>= 8;
        buf[off + 3] = (byte) l;
        l >>= 8;
        buf[off + 4] = (byte) l;
        l >>= 8;
        buf[off + 5] = (byte) l;
        l >>= 8;
        buf[off + 6] = (byte) l;
        l >>= 8;
        buf[off + 7] = (byte) l;
    }
}