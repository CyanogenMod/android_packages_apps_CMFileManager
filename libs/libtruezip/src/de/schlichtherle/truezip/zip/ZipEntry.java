/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.Constants.EMPTY;
import static de.schlichtherle.truezip.zip.Constants.FORCE_ZIP64_EXT;
import static de.schlichtherle.truezip.zip.ExtraField.WINZIP_AES_ID;
import static de.schlichtherle.truezip.zip.ExtraField.ZIP64_HEADER_ID;
import static de.schlichtherle.truezip.zip.LittleEndian.readLong;
import static de.schlichtherle.truezip.zip.LittleEndian.writeLong;
import java.util.Formatter;

/**
 * Drop-in replacement for {@link java.util.zip.ZipEntry java.util.zip.ZipEntry}.
 * For every numeric property of this class, the default value is
 * {@code UNKNOWN} in order to indicate an unknown state and it's
 * permitted to set this value explicitly in order to reset the property.
 * <p>
 * Note that a {@code ZipEntry} object can be used with only one
 * {@link ZipFile} or {@link ZipOutputStream} instance.
 * Reusing the same {@code ZipEntry} object with a second object of these
 * classes is an error and may result in unpredictable behaviour.
 * <p>
 * In general, this class is <em>not</em> thread-safe.
 * However, it is safe to call only the getters of this class from multiple
 * threads concurrently.
 *
 * @author  Christian Schlichtherle
 */
// TODO: Consider implementing de.schlichtherle.truezip.fs.entry.Entry.
public class ZipEntry implements Cloneable {

    // Bit masks for initialized fields.
    private static final int PLATFORM = 1, METHOD = 1 << 1,
                             CRC = 1 << 2, DTIME = 1 << 6,
                             EATTR = 1 << 7;

    /** The unknown value for numeric properties. */
    public static final byte UNKNOWN = -1;

    /** Windows platform. */
    public static final short PLATFORM_FAT  = 0;

    /** Unix platform. */
    public static final short PLATFORM_UNIX = 3;

    /**
     * Method for <em>Stored</em> (uncompressed) entries.
     *
     * @see   #setMethod(int)
     */
    public static final int STORED = 0;

    /**
     * Method for <em>Deflated</em> compressed entries.
     *
     * @see   #setMethod(int)
     */
    public static final int DEFLATED = 8;

    /**
     * Method for <em>BZIP2</em> compressed entries.
     *
     * @see   #setMethod(int)
     * @since TrueZIP 7.3
     */
    public static final int BZIP2 = 12;

    /**
     * Pseudo compression method for WinZip AES encrypted entries.
     *
     * @since TrueZIP 7.3
     */
    static final int WINZIP_AES = 99;

    /** General Purpose Bit Flag mask for encrypted data. */
    static final int GPBF_ENCRYPTED = 1;

    static final int GPBF_DATA_DESCRIPTOR = 1 << 3;
    static final int GPBF_UTF8 = 1 << 11;

    /**
     * Smallest supported DOS date/time value in a ZIP file,
     * which is January 1<sup>st</sup>, 1980 AD 00:00:00 local time.
     */
    public static final long MIN_DOS_TIME = DateTimeConverter.MIN_DOS_TIME;

    /**
     * Largest supported DOS date/time value in a ZIP file,
     * which is December 31<sup>st</sup>, 2107 AD 23:59:58 local time.
     */
    public static final long MAX_DOS_TIME = DateTimeConverter.MAX_DOS_TIME;

    private byte init;                  // bit flags for init state
    private String name;
    private byte platform;              // 1 byte unsigned int (UByte)
    private short general;              // 2 bytes unsigned int (UShort)
    private short method;               // 2 bytes unsigned int (UShort)
    private int dtime;                  // 4 bytes unsigned int (UInt)
    private int crc;                    // 4 bytes unsigned int (UInt)
    private long csize = UNKNOWN;       // 63 bits unsigned integer (ULong)
    private long size = UNKNOWN;        // 63 bits unsigned integer (Ulong)
    private int eattr;                  // 4 bytes unsigned int (Uint)

    /** Relative Offset Of Local File Header. */
    private long offset = UNKNOWN;     // 63 bits unsigned integer (ULong)

    /**
     * The map of extra fields.
     * Maps from Header ID [Integer] to Extra Field [ExtraField].
     * Should be {@code null} or may be empty if no extra fields are used.
     */
    private ExtraFields fields;

    /** Comment field. */
    private String comment;

    /** Constructs a new ZIP entry with the given name. */
    public ZipEntry(final String name) {
        UShort.check(name.length());
        this.name = name;
    }

    /**
     * Constructs a new ZIP entry with the given name and all other properties
     * copied from the given template.
     */
    protected ZipEntry(final String name, final ZipEntry template) {
        UShort.check(name.length());
        this.init = template.init;
        this.name = name;
        this.platform = template.platform;
        this.general = template.general;
        this.method = template.method;
        this.dtime = template.dtime;
        this.crc = template.crc;
        this.csize = template.csize;
        this.size = template.size;
        this.eattr = template.eattr;
        this.offset = template.offset;
        final ExtraFields templateFields = template.fields;
        this.fields = templateFields == null ? null : templateFields.clone();
        this.comment = template.comment;
    }

    @Override
    public ZipEntry clone() {
        final ZipEntry entry;
        try {
            entry = (ZipEntry) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        final ExtraFields fields = this.fields;
        entry.fields = fields == null ? null : fields.clone();
        return entry;
    }

    private boolean isInit(final int mask) {
        return 0 != (init & mask);
    }

    private void setInit(final int mask, final boolean init) {
        if (init)
            this.init |=  mask;
        else
            this.init &= ~mask;
    }

    /** Returns the ZIP entry name. */
    public final String getName() {
        return name;
    }

    /**
     * Returns true if and only if this ZIP entry represents a directory entry
     * (i.e. end with {@code '/'}).
     */
    public final boolean isDirectory() {
        return name.endsWith("/");
    }

    public final short getPlatform() {
        return isInit(PLATFORM) ? (short) (platform & UByte.MAX_VALUE) : UNKNOWN;
    }

    public final void setPlatform(final short platform) {
        final boolean known = UNKNOWN != platform;
        if (known) {
            UByte.check(platform, name, "Platform out of range");
            this.platform = (byte) platform;
        } else {
            this.platform = 0;
        }
        setInit(PLATFORM, known);
    }

    final short getRawPlatform() {
        return (short) (platform & UByte.MAX_VALUE);
    }

    final void setRawPlatform(final int platform) {
        assert UByte.check(platform);
        this.platform = (byte) platform;
        setInit(PLATFORM, true);
    }

    final int getRawVersionNeededToExtract() {
        final int method = getRawMethod();
        return BZIP2 == method
                ? 46
                : isZip64ExtensionsRequired()
                    ? 45
                    : DEFLATED == method || isDirectory()
                        ? 20
                        : 10;
    }

    /** Returns the General Purpose Bit Flags. */
    final int getGeneralPurposeBitFlags() {
        return general & UShort.MAX_VALUE;
    }

    /** Sets the General Purpose Bit Flags. */
    final void setGeneralPurposeBitFlags(final int general) {
        assert UShort.check(general);
        this.general = (short) general;
    }

    /** Returns the indexed General Purpose Bit Flag. */
    final boolean getGeneralPurposeBitFlag(final int mask) {
        return 0 != (general & mask);
    }

    /** Sets the indexed General Purpose Bit Flag. */
    final void setGeneralPurposeBitFlag(final int mask, final boolean bit) {
        if (bit)
            general |=  mask;
        else
            general &= ~mask;
    }

    /**
     * Returns {@code true} if and only if this ZIP entry is encrypted.
     * Note that only WinZip AES encryption is currently supported.
     *
     * @return {@code true} if and only if this ZIP entry is encrypted.
     * @since  TrueZIP 7.3
     */
    public final boolean isEncrypted() {
        return getGeneralPurposeBitFlag(GPBF_ENCRYPTED);
    }

    /**
     * Sets the encryption flag for this ZIP entry.
     * If you set this to {@code true}, you will also need to provide
     * {@link ZipOutputStream#setCryptoParameters(ZipCryptoParameters) crypto parameters}.
     * <p>
     * Note that only {@link WinZipAesParameters WinZip AES encryption} is
     * currently supported.
     *
     * @param encrypted whether or not this ZIP entry should get encrypted.
     * @since TrueZIP 7.3
     */
    public final void setEncrypted(boolean encrypted) {
        setGeneralPurposeBitFlag(GPBF_ENCRYPTED, encrypted);
    }

    /**
     * Sets the encryption property to {@code false} and removes any other
     * encryption artifacts, e.g. a WinZip AES extra field.
     *
     * @since TrueZIP 7.4
     * @see   <a href="http://java.net/jira/browse/TRUEZIP-176">#TRUEZIP-176</a>
     */
    public final void clearEncryption() {
        setEncrypted(false);
        final WinZipAesEntryExtraField field
                = (WinZipAesEntryExtraField) removeExtraField(WINZIP_AES_ID);
        if (WINZIP_AES == getRawMethod())
            setRawMethod(null == field ? UNKNOWN : field.getMethod());
    }

    /**
     * Returns the compression method for this entry.
     *
     * @see #setMethod(int)
     * @see ZipOutputStream#getMethod()
     */
    public final int getMethod() {
        return isInit(METHOD) ? method & UShort.MAX_VALUE : UNKNOWN;
    }

    /**
     * Sets the compression method for this entry.
     *
     * @see    #getMethod()
     * @see    ZipOutputStream#setMethod(int)
     * @throws IllegalArgumentException If {@code method} is not
     *         {@link #STORED}, {@link #DEFLATED}, {@link #BZIP2} or
     *         {@link #UNKNOWN}.
     */
    public final void setMethod(final int method) {
        switch (method) {
            case WINZIP_AES:
                // This is only present to support manual copying of properties.
                // It should never be set by applications.
            case STORED:
            case DEFLATED:
            case BZIP2:
                this.method = (short) method;
                setInit(METHOD, true);
                break;
            case UNKNOWN:
                this.method = 0;
                setInit(METHOD, false);
                break;
            default:
                throw new IllegalArgumentException(
                        name + " (unsupported compression method " + method + ")");
        }
    }

    final int getRawMethod() {
        return method & UShort.MAX_VALUE;
    }

    final void setRawMethod(final int method) {
        assert UShort.check(method);
        this.method = (short) method;
        setInit(METHOD, true);
    }

    public final long getTime() {
        if (!isInit(DTIME))
            return UNKNOWN;
        return getDateTimeConverter().toJavaTime(dtime & UInt.MAX_VALUE);
    }

    public final void setTime(final long jtime) {
        final boolean known = UNKNOWN != jtime;
        if (known) {
            this.dtime = (int) getDateTimeConverter().toDosTime(jtime);
        } else {
            this.dtime = 0;
        }
        setInit(DTIME, known);
    }

    final long getRawTime() {
        return dtime & UInt.MAX_VALUE;
    }

    final void setRawTime(final long dtime) {
        assert UInt.check(dtime);
        this.dtime = (int) dtime;
        setInit(DTIME, true);
    }

    /**
     * Returns a {@link DateTimeConverter} for the conversion of Java time
     * to DOS date/time fields and vice versa.
     * <p>
     * The implementation in the class {@link ZipEntry} returns
     * {@link DateTimeConverter#JAR}.
     *
     * @return {@link DateTimeConverter#JAR}
     */
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }

    public final long getCrc() {
        return isInit(CRC) ? crc & UInt.MAX_VALUE : UNKNOWN;
    }

    public final void setCrc(final long crc) {
        final boolean known = UNKNOWN != crc;
        if (known) {
            UInt.check(crc, name, "CRC-32 out of range");
            this.crc = (int) crc;
        } else {
            this.crc = 0;
        }
        setInit(CRC, known);
    }

    final long getRawCrc() {
        return crc & UInt.MAX_VALUE;
    }

    final void setRawCrc(final long crc) {
        assert UInt.check(crc);
        this.crc = (int) crc;
        setInit(CRC, true);
    }

    /**
     * Returns the compressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public final long getCompressedSize() {
        return csize;
    }

    /**
     * Sets the compressed size of this entry.
     *
     * @param csize The Compressed Size.
     * @throws IllegalArgumentException If {@code csize} is not in the
     *         range from {@code 0} to {@link ULong#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.ULong#MAX_VALUE}).
     * @see #getCompressedSize
     */
    public final void setCompressedSize(final long csize) {
        if (UNKNOWN != csize)
            ULong.check(csize, name, "Compressed Size out of range");
        this.csize = csize;
    }

    final long getRawCompressedSize() {
        final long csize = this.csize;
        if (UNKNOWN == csize)
            return 0;
        return FORCE_ZIP64_EXT || UInt.MAX_VALUE <= csize
                ? UInt.MAX_VALUE
                : csize;
    }

    final void setRawCompressedSize(final long csize) {
        assert ULong.check(csize);
        this.csize = csize;
    }

    /**
     * Returns the uncompressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public final long getSize() {
        return size;
    }

    /**
     * Sets the uncompressed size of this entry.
     *
     * @param size The (Uncompressed) Size.
     * @throws IllegalArgumentException If {@code size} is not in the
     *         range from {@code 0} to {@link ULong#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.ULong#MAX_VALUE}).
     * @see #getCompressedSize
     */
    public final void setSize(final long size) {
        if (UNKNOWN != size)
            ULong.check(size, name, "Uncompressed Size out of range");
        this.size = size;
    }

    final long getRawSize() {
        final long size = this.size;
        if (UNKNOWN == size)
            return 0;
        return FORCE_ZIP64_EXT || UInt.MAX_VALUE <= size
                ? UInt.MAX_VALUE
                : size;
    }

    final void setRawSize(final long size) {
        assert ULong.check(size);
        this.size = size;
    }

    /**
     * Returns the external file attributes.
     *
     * @since  TrueZIP 7.3
     * @return The external file attributes.
     */
    public final long getExternalAttributes() {
        return isInit(EATTR) ? eattr & UInt.MAX_VALUE : UNKNOWN;
    }

    /**
     * Sets the external file attributes.
     *
     * @param eattr the external file attributes.
     * @since TrueZIP 7.3
     */
    public final void setExternalAttributes(final long eattr) {
        final boolean known = UNKNOWN != eattr;
        if (known) {
            UInt.check(eattr, name, "external file attributes out of range");
            this.eattr = (int) eattr;
        } else {
            this.eattr = 0;
        }
        setInit(EATTR, known);
    }

    final long getRawExternalAttributes() {
        if (!isInit(EATTR))
            return isDirectory() ? 0x10 : 0;
        return eattr & UInt.MAX_VALUE;
    }

    final void setRawExternalAttributes(final long eattr) {
        assert UInt.check(eattr);
        this.eattr = (int) eattr;
        setInit(EATTR, true);
    }

    final long getOffset() {
        return offset;
    }

    final long getRawOffset() {
        final long offset = this.offset;
        if (UNKNOWN == offset)
            return 0;
        return FORCE_ZIP64_EXT || UInt.MAX_VALUE <= offset
                ? UInt.MAX_VALUE
                : offset;
    }

    final void setRawOffset(final long offset) {
        assert ULong.check(offset);
        this.offset = offset;
    }

    final ExtraField getExtraField(int headerId) {
        final ExtraFields fields = this.fields;
        return fields == null ? null : fields.get(headerId);
    }

    final ExtraField addExtraField(final ExtraField field) {
        assert null != field;
        ExtraFields fields = this.fields;
        if (null == fields)
            this.fields = fields = new ExtraFields();
        return fields.add(field);
    }

    final ExtraField removeExtraField(final int headerId) {
        final ExtraFields fields = this.fields;
        return null != fields ? fields.remove(headerId) : null;
    }

    /**
     * Returns a protective copy of the serialized extra fields.
     * Note that unlike its template {@link java.util.zip.ZipEntry#getExtra()},
     * this method never returns {@code null}.
     *
     * @return A new byte array holding the serialized extra fields.
     *         {@code null} is never returned.
     */
    public final byte[] getExtra() {
        return getExtraFields(false);
    }

    /**
     * Sets the serialized extra fields by making a protective copy.
     * Note that this method parses the serialized extra fields according to
     * the ZIP File Format Specification and limits its size to 64 KB.
     * Therefore, this property cannot not be used to hold arbitrary
     * (application) data.
     * Consider storing such data in a separate entry instead.
     *
     * @param  buf The byte array holding the serialized extra fields.
     * @throws IllegalArgumentException if the serialized extra fields exceed
     *         64 KB or do not conform to the ZIP File Format Specification.
     */
    public final void setExtra(final byte[] buf)
    throws IllegalArgumentException {
        if (null != buf)
            UShort.check(buf.length, "extra fields too large", null);
        if (null == buf || buf.length <= 0)
            this.fields = null;
        else
            setExtraFields(buf, false);
    }

    /**
     * Returns a protective copy of the serialized extra fields.
     *
     * @return A new byte array holding the serialized extra fields.
     *         {@code null} is never returned.
     * @see    #getRawExtraFields()
     */
    final byte[] getRawExtraFields() {
        return getExtraFields(true);
    }

    /**
     * Sets extra fields and parses ZIP64 extra field.
     * This method <em>must not</em> get called before the uncompressed size,
     * compressed size and offset have been initialized!
     *
     * @throws IllegalArgumentException if the serialized extra fields do not
     *         conform to the ZIP File Format Specification.
     */
    final void setRawExtraFields(final byte[] buf)
    throws IllegalArgumentException {
        setExtraFields(buf, true);
    }

    private byte[] getExtraFields(final boolean zip64) {
        ExtraFields fields = this.fields;
        if (zip64) {
            final ExtraField field = composeZip64ExtraField();
            if (null != field) {
                fields = null != fields ? fields.clone() : new ExtraFields();
                fields.add(field);
            }
        } else {
            assert null == fields || null == fields.get(ZIP64_HEADER_ID);
        }
        return null == fields ? EMPTY : fields.getExtra();
    }

    /**
     * @throws IllegalArgumentException if the serialized extra fields do not
     *         conform to the ZIP File Format Specification.
     */
    private void setExtraFields(final byte[] buf, final boolean zip64)
    throws IllegalArgumentException {
        assert UShort.check(buf.length);
        if (0 < buf.length) {
            final ExtraFields fields = new ExtraFields();
            try {
                fields.readFrom(buf, 0, buf.length);
                if (zip64) parseZip64ExtraField(fields);
            } catch (final IndexOutOfBoundsException ex) {
                throw new IllegalArgumentException(ex);
            }
            fields.remove(ZIP64_HEADER_ID);
            this.fields = 0 < fields.size() ? fields : null;
        } else {
            this.fields = null;
        }
    }

    /**
     * Composes a ZIP64 Extended Information Extra Field from the properties
     * of this entry.
     * If no ZIP64 Extended Information Extra Field is required it is removed
     * from the collection of extra fields.
     */
    private ExtraField composeZip64ExtraField() {
        final byte[] data = new byte[3 * 8]; // maximum size
        int off = 0;
        // Write out Uncompressed Size.
        final long size = getSize();
        if (FORCE_ZIP64_EXT && UNKNOWN != size || UInt.MAX_VALUE <= size) {
            writeLong(size, data, off);
            off += 8;
        }
        // Write out Compressed Size.
        final long csize = getCompressedSize();
        if (FORCE_ZIP64_EXT && UNKNOWN != csize || UInt.MAX_VALUE <= csize) {
            writeLong(csize, data, off);
            off += 8;
        }
        // Write out Relative Header Offset.
        final long offset = getOffset();
        if (FORCE_ZIP64_EXT && UNKNOWN != offset || UInt.MAX_VALUE <= offset) {
            writeLong(offset, data, off);
            off += 8;
        }
        // Create ZIP64 Extended Information Extra Field from serialized data.
        final ExtraField field;
        if (off > 0) {
            field = new DefaultExtraField(ZIP64_HEADER_ID);
            field.readFrom(data, 0, off);
        } else {
            field = null;
        }
        return field;
    }

    /**
     * Parses the properties of this entry from the ZIP64 Extended Information
     * Extra Field, if present.
     * The ZIP64 Extended Information Extra Field is <em>not</em> removed.
     */
    private void parseZip64ExtraField(final ExtraFields fields)
    throws IndexOutOfBoundsException {
        final ExtraField ef = fields.get(ZIP64_HEADER_ID);
        if (null == ef) return;
        final byte[] data = ef.getDataBlock();
        int off = 0;
        // Read in Uncompressed Size.
        final long size = getRawSize();
        if (UInt.MAX_VALUE <= size) {
            assert UInt.MAX_VALUE == size;
            setRawSize(readLong(data, off));
            off += 8;
        }
        // Read in Compressed Size.
        final long csize = getRawCompressedSize();
        if (UInt.MAX_VALUE <= csize) {
            assert UInt.MAX_VALUE == csize;
            setRawCompressedSize(readLong(data, off));
            off += 8;
        }
        // Read in Relative Header Offset.
        final long offset = getRawOffset();
        if (UInt.MAX_VALUE <= offset) {
            assert UInt.MAX_VALUE == offset;
            setRawOffset(readLong(data, off));
            //off += 8;
        }
    }

    public final String getComment() {
        return comment;
    }

    /**
     * Sets the entry comment.
     * Note that this method limits the comment size to 64 KB.
     * Therefore, this property should not be used to hold arbitrary
     * (application) data.
     * Consider storing such data in a separate entry instead.
     *
     * @param  comment The entry comment.
     * @throws RuntimeException if the entry comment exceeds 64 KB.
     */
    public final void setComment(final String comment) {
        if (null != comment)
            UShort.check(comment.length(), name, "Comment too long");
        this.comment = comment;
    }

    final String getRawComment() {
        final String comment = this.comment;
        return null != comment ? comment : "";
    }

    final void setRawComment(final String comment) {
        assert UShort.check(comment.length());
        this.comment = comment;
    }

    final boolean isDataDescriptorRequired() {
        return UNKNOWN == (getCrc() | getCompressedSize() | getSize());
    }

    final boolean isZip64ExtensionsRequired() {
        // Offset MUST be considered in decision about ZIP64 format - see
        // description of Data Descriptor in ZIP File Format Specification!
        if (FORCE_ZIP64_EXT)
            return true /*UNKNOWN != getCompressedSize()
                    || UNKNOWN != getSize()
                    || UNKNOWN != getOffset()*/;
        else
            return UInt.MAX_VALUE <= getCompressedSize()
                    || UInt.MAX_VALUE <= getSize()
                    || UInt.MAX_VALUE <= getOffset();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(256);
        final Formatter f = new Formatter(s);
        try {
            f.format("%s[name=%s", getClass().getName(), getName());
            long value;
            if (UNKNOWN != (value = getGeneralPurposeBitFlags()))
                f.format(", gpbf=0x%04X", value);
            if (UNKNOWN != (value = getMethod()))
                f.format(", method=%d", value);
            if (UNKNOWN != (value = getTime()))
                f.format(", time=%tc", value);
            if (UNKNOWN != (value = getCrc()))
                f.format(", crc=0x%08X", value);
            if (UNKNOWN != (value = getCompressedSize()))
                f.format(", compressedSize=%d", value);
            if (UNKNOWN != (value = getSize()))
                f.format(", size=%d", value);
            if (UNKNOWN != (value = getExternalAttributes()))
                f.format(", ea=0x%08X", value);
            {
                final String comment = getComment();
                if (null != comment)
                    f.format(", comment=\"%s\"", comment);
            }
        } finally {
            f.close();
        }
        return s.append("]").toString();
    }
}