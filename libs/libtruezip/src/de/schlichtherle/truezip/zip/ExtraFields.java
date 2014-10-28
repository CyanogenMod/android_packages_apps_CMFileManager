/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.Constants.EMPTY;
import static de.schlichtherle.truezip.zip.LittleEndian.readUShort;
import static de.schlichtherle.truezip.zip.LittleEndian.writeShort;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a collection of {@link ExtraField Extra Fields} as they may
 * be present at several locations in ZIP files.
 *
 * @author  Christian Schlichtherle
 */
final class ExtraFields implements Cloneable {

    /**
     * The map of Extra Fields.
     * Maps from Header ID [{@link Integer}] to Extra Field [{@link ExtraField}].
     * Must not be {@code null}, but may be empty if no Extra Fields are used.
     * The map is sorted by Header IDs in ascending order.
     */
    private Map<Integer, ExtraField> extra = new TreeMap<Integer, ExtraField>();

    /** Returns a shallow clone of this collection. */
    @Override
    public ExtraFields clone() {
        try {
            final ExtraFields clone = (ExtraFields) super.clone();
            clone.extra = new TreeMap<Integer, ExtraField>(extra);
            return clone;
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** Returns the number of Extra Fields in this collection. */
    int size() {
        return extra.size();
    }

    /**
     * Returns the Extra Field with the given Header ID or {@code null}
     * if no such Extra Field exists.
     *
     * @param headerId The requested Header ID.
     * @return The Extra Field with the given Header ID or {@code null}
     *         if no such Extra Field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.UShort#MAX_VALUE}).
     */
    ExtraField get(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = extra.get(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Stores the given Extra Field in this collection.
     *
     * @param ef The Extra Field to store in this collection.
     * @return The Extra Field previously associated with the Header ID of
     *         of the given Extra Field or {@code null} if no such
     *         Extra Field existed.
     * @throws NullPointerException If {@code ef} is {@code null}.
     * @throws IllegalArgumentException If the Header ID of the given Extra
     *         Field is not in the range of {@code 0} to
     *         {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.UShort#MAX_VALUE}).
     */
    ExtraField add(final ExtraField ef) {
        final int headerId = ef.getHeaderId();
        assert UShort.check(headerId);
        return extra.put(headerId, ef);
    }

    /**
     * Removes the Extra Field with the given Header ID.
     *
     * @param headerId The requested Header ID.
     * @return The Extra Field with the given Header ID or {@code null}
     *         if no such Extra Field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.UShort#MAX_VALUE}).
     */
    ExtraField remove(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = extra.remove(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Returns the number of bytes required to hold the Extra Fields.
     *
     * @return The length of the Extra Fields in bytes.
     *         May be {@code 0}.
     * @see #getExtra
     */
    int getExtraLength() {
        final Map<Integer, ExtraField> extra = this.extra;
        if (extra.isEmpty())
            return 0;
        int l = 0;
        for (ExtraField ef : extra.values())
            l += 4 + ef.getDataSize();
        return l;
    }

    /**
     * Returns a protective copy of the Extra Fields.
     * {@code null} is never returned.
     *
     * @see #getExtraLength
     */
    byte[] getExtra() {
        final int size = getExtraLength();
        assert UShort.check(size);
        if (0 == size)
            return EMPTY;
        final byte[] data = new byte[size];
        writeTo(data, 0);
        return data;
    }

    /**
     * Deserializes this collection of extra fields from
     * the data block starting at the zero based offset {@code off} with
     * {@code len} bytes length in the byte array {@code buf}.
     * After return, this collection does not access {@code buf} anymore
     * and {@link #getDataSize} equals {@code len}.
     *
     * @param  buf The byte array to read the data block from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the data block is read from.
     * @param  len The length of the data block in bytes.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code buf} does not hold at least {@code len}
     *         bytes at the zero based offset {@code off}
     *         or if {@code len} is smaller than the extra field data requires.
     * @throws IllegalArgumentException If the data block does not conform to
     *         the ZIP File Format Specification.
     * @see    #getExtraLength
     */
    void readFrom(final byte[] buf, int off, final int len)
    throws IndexOutOfBoundsException, IllegalArgumentException {
        assert UShort.check(len);
        final Map<Integer, ExtraField> map = new TreeMap<Integer, ExtraField>();
        if (null != buf && 0 < len) {
            final int end = off + len;
            while (off < end) {
                final int headerId = readUShort(buf, off);
                off += 2;
                final int dataSize = readUShort(buf, off);
                off += 2;
                final ExtraField ef = ExtraField.create(headerId);
                ef.readFrom(buf, off, dataSize);
                off += dataSize;
                map.put(headerId, ef);
            }
            assert off == end;
        }
        extra = map;
    }

    /**
     * Serializes a list of Extra Fields of {@link #getExtraLength} bytes to the
     * byte array {@code data} at the zero based offset {@code off}.
     * Upon return, this collection shall not access {@code data}
     * subsequently.
     *
     * @param  data The byte array to write the list of Extra Fields to.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the list of Extra Fields is written to.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code data} does not hold at least {@link #getExtraLength}
     *         bytes at the zero based offset {@code off}.
     * @see    #getExtraLength
     */
    void writeTo(final byte[] data, int off)
    throws IndexOutOfBoundsException {
       for (final ExtraField ef : extra.values()) {
            writeShort(ef.getHeaderId(), data, off);
            off += 2;
            writeShort(ef.getDataSize(), data, off);
            off += 2;
            ef.writeTo(data, off);
            off += ef.getDataSize();
        }
    }
}