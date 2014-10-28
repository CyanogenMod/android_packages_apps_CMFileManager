/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.Deflater;

/**
 * An interface for {@link ZipOutputStream} parameters.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public interface ZipOutputStreamParameters
extends ZipCharsetParameters {

    /**
     * Returns the number of entries which can be additionally accomodated by
     * the internal hash map without resizing it.
     * When a new ZIP file is created, this constant is used in order to
     * compute the initial capacity of the internal hash map.
     * When an existing ZIP file is appended to, this constant is added to the
     * number of entries in order to compute the initial capacity of the
     * internal hash map.
     *
     * @return The number of entries which can be additionally accomodated by
     *         the internal hash map without resizing it.
     */
    int getOverheadSize();

    /**
     * Returns the default compression method for entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     * Legal values are {@link ZipEntry#STORED}, {@link ZipEntry#DEFLATED}
     * and {@link ZipEntry#BZIP2}.
     *
     * @return The default compression method for entries.
     * @see    ZipEntry#getMethod
     */
    int getMethod();

    /**
     * Returns the compression level for entries.
     * This property is only used if the effective compression method is
     * {@link ZipEntry#DEFLATED} or {@link ZipEntry#BZIP2}.
     * Legal values are {@link Deflater#DEFAULT_COMPRESSION} or range from
     * {@code Deflater#BEST_SPEED} to {@code Deflater#BEST_COMPRESSION}.
     *
     * @return The compression level for entries.
     */
    int getLevel();
}