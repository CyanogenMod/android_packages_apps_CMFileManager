/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.Constants.EMPTY;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for an extra field in a Local or Central Header of a
 * ZIP file.
 * It defines the common properties of all extra fields and how to
 * serialize/deserialize them to/from byte arrays.
 *
 * @author Christian Schlichtherle
 */
abstract class ExtraField {

    private static final Map<Integer, Class<? extends ExtraField>> registry
            = new HashMap<Integer, Class<? extends ExtraField>>();

    /** The Header ID for a ZIP64 Extended Information extra field. */
    static final int ZIP64_HEADER_ID = 0x0001;

    /** The Header ID for a WinZip AES extra field. */
    static final int WINZIP_AES_ID = 0x9901;

    static {
        register(WinZipAesEntryExtraField.class);
    }

    /**
     * Registers a concrete implementation of this abstract base class for
     * use with the static factory method {@link #create}.
     *
     * @param  c the implementation class of this abstract base class.
     * @throws IllegalArgumentException if {@code c} cannot get instantiated,
     *         is not a subclass of {@code ExtraField} or its Header ID is out
     *         of range.
     *         A more descriptive exception may be associated to it as its
     *         cause.
     * @see    #create
     */
    static void register(final Class<? extends ExtraField> c) {
        final ExtraField ef;
        try {
            ef = (ExtraField) c.newInstance();
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        final int headerId = ef.getHeaderId();
        assert UShort.check(headerId);
        registry.put(headerId, c);
    }

    /**
     * A static contrstructor which creates a new extra field based on the
     * given header id.
     * The returned extra field still requires proper initialization, for
     * example by calling {@link #readFrom}.
     *
     * @param  headerId An unsigned short integer (two bytes) which indicates
     *         the type of the returned extra field.
     * @return A new extra field
     * @see    #register
     */
    static ExtraField create(final int headerId) {
        assert UShort.check(headerId);
        final Class<? extends ExtraField> c = registry.get(headerId);
        final ExtraField ef;
        try {
            ef = null != c
                    ? (ExtraField) c.newInstance()
                    : new DefaultExtraField(headerId);
        } catch (Exception cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
        assert headerId == ef.getHeaderId();
        return ef;
    }

    /**
     * Returns the Header ID (type) of this extra field.
     * The Header ID is an unsigned short integer (two bytes)
     * which must be constant during the life cycle of this object.
     */
    abstract int getHeaderId();

    /**
     * Returns the data size of this extra field.
     * The data size is an unsigned short integer (two bytes)
     * which indicates the length of the data block in bytes and does not
     * include its own size in this extra field.
     * This property may be initialized by calling {@link #readFrom}.
     *
     * @return The size of the data block in bytes
     *         or {@code 0} if unknown.
     * @see    #getDataBlock
     */
    abstract int getDataSize();

    /**
     * Returns a protective copy of the data block.
     *
     * @see #getDataSize
     */
    final byte[] getDataBlock() {
        final int size = getDataSize();
        assert UShort.check(size);
        if (0 == size)
            return EMPTY;
        final byte[] data = new byte[size];
        writeTo(data, 0);
        return data;
    }

    /**
     * Deserializes this extra field from
     * the data block starting at the zero based offset {@code off} with
     * {@code len} bytes length in the byte array {@code buf}.
     * Upon return, this extra field shall not access {@code data}
     * subsequently and {@link #getDataSize} must equal {@code size}.
     *
     * @param  buf The byte array to read the data block from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the data block is read from.
     * @param  len The length of the data block in bytes.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code buf} does not hold at least {@code len}
     *         bytes at the zero based offset {@code off}.
     * @throws IllegalArgumentException If the data block does not conform to
     *         this type of extra field.
     * @see    #getDataSize
     */
    abstract void readFrom(byte[] buf, int off, int len)
    throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Serializes this extra field to
     * the data block starting at the zero based offset {@code off} with
     * {@link #getDataSize} bytes length in the byte array {@code buf}.
     * Upon return, this extra field shall not access {@code buf}
     * subsequently.
     *
     * @param  buf The byte array to write the data block to.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the data block is written to.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code buf} does not hold at least {@link #getDataSize}
     *         bytes at the zero based offset {@code off}.
     * @see    #getDataSize
     */
    abstract void writeTo(byte[] buf, int off)
    throws IndexOutOfBoundsException;
}
