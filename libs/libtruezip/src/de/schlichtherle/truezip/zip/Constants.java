/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.io.Streams;
import java.nio.charset.Charset;

/**
 * A package private class with some constants for ZIP files.
 *
 * @author Christian Schlichtherle
 */
final class Constants {

    /**
     * This boolean field is set by the system property
     * {@code de.schlichtherle.truezip.zip.forceZip64Ext}.
     * If this property is set to {@code true} (case is ignored),
     * then ZIP64 extensions are always added when writing a ZIP file,
     * regardless of its size.
     * This system property is intended for testing purposes only.
     * During normal operations, it should not be set as many
     * third party tools would not treat the redundant ZIP64 extensions
     * correctly.
     * Note that it's impossible to inhibit ZIP64 extensions if they are
     * required.
     */
    // TODO: Rename this to forceZip64Extensions
    static final boolean FORCE_ZIP64_EXT = Boolean.getBoolean(
            Constants.class.getPackage().getName() + ".forceZip64Ext");

    /** Local File Header signature. */
    static final int LFH_SIG = 0x04034B50;
   
    /** Data Descriptor signature. */
    static final int DD_SIG = 0x08074B50;
   
    /** Central File Header signature. */
    static final int CFH_SIG = 0x02014B50;

    /** Zip64 End Of Central Directory Record. */
    static final int ZIP64_EOCDR_SIG = 0x06064B50;

    /** Zip64 End Of Central Directory Locator. */
    static final int ZIP64_EOCDL_SIG = 0x07064B50;

    /** End Of Central Directory Record signature. */
    static final int EOCDR_SIG = 0x06054B50;

    /** The minimum length of the Local File Header record. */
    static final int LFH_MIN_LEN =
            /* local file header signature     */ 4 +
            /* version needed to extract       */ 2 +
            /* general purpose bit flag        */ 2 +
            /* compression method              */ 2 +
            /* last mod file time              */ 2 +
            /* last mod file date              */ 2 +
            /* crc-32                          */ 4 +
            /* compressed size                 */ 4 +
            /* uncompressed size               */ 4 +
            /* file name length                */ 2 +
            /* extra field length              */ 2;

    /** The minimum length of the Central File Header record. */
    static final int CFH_MIN_LEN =
            /* central file header signature   */ 4 +
            /* version made by                 */ 2 +
            /* version needed to extract       */ 2 +
            /* general purpose bit flag        */ 2 +
            /* compression method              */ 2 +
            /* last mod file time              */ 2 +
            /* last mod file date              */ 2 +
            /* crc-32                          */ 4 +
            /* compressed size                 */ 4 +
            /* uncompressed size               */ 4 +
            /* file name length                */ 2 +
            /* extra field length              */ 2 +
            /* file comment length             */ 2 +
            /* disk number start               */ 2 +
            /* internal file attributes        */ 2 +
            /* external file attributes        */ 4 +
            /* relative offset of local header */ 4;

    /** The minimum length of the End Of Central Directory Record. */
    static final int EOCDR_MIN_LEN =
            /* end of central dir signature    */ 4 +
            /* number of this disk             */ 2 +
            /* number of the disk with the     */
            /* start of the central directory  */ 2 +
            /* total number of entries in the  */
            /* central directory on this disk  */ 2 +
            /* total number of entries in      */
            /* the central directory           */ 2 +
            /* size of the central directory   */ 4 +
            /* offset of start of central      */
            /* directory with respect to       */
            /* the starting disk number        */ 4 +
            /* zipfile comment length          */ 2;

    /** The minimum length of the Zip64 End Of Central Directory Record. */
    static final int ZIP64_EOCDR_MIN_LEN =
            /* zip64 end of central dir        */
            /* signature                       */ 4 +
            /* size of zip64 end of central    */
            /* directory record                */ 8 +
            /* version made by                 */ 2 +
            /* version needed to extract       */ 2 +
            /* number of this disk             */ 4 +
            /* number of the disk with the     */
            /* start of the central directory  */ 4 +
            /* total number of entries in the  */
            /* central directory on this disk  */ 8 +
            /* total number of entries in      */
            /* the central directory           */ 8 +
            /* size of the central directory   */ 8 +
            /* offset of start of central      */
            /* directory with respect to       */
            /* the starting disk number        */ 8;

    /** The length of the Zip64 End Of Central Directory Locator. */
    static final int ZIP64_EOCDL_LEN =
            /* zip64 end of central dir locator*/
            /* signature                       */ 4 +
            /* number of the disk with the     */
            /* start of the zip64 end of       */
            /* central directory               */ 4 +
            /* relative offset of the zip64    */
            /* end of central directory record */ 8 +
            /* total number of disks           */ 4;

    static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The default character set used for entry names and comments in ZIP
     * archive files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     * Note that you should use &quot;IBM437&quot; for ordinary ZIP archive
     * files instead.
     */
    static final Charset DEFAULT_CHARSET = UTF8;

    /**
     * The maximum buffer size used for deflating and inflating.
     * Optimized for performance.
     */
    static final int MAX_FLATER_BUF_LENGTH = Streams.BUFFER_SIZE;

    /**
     * The minimum buffer size used for deflating and inflating.
     * Optimized for performance.
     */
    static final int MIN_FLATER_BUF_LENGTH = MAX_FLATER_BUF_LENGTH / 8;

    /** An empty byte array. */
    static final byte[] EMPTY = new byte[0];

    /* Can't touch this - hammer time! */
    private Constants() { }
}
