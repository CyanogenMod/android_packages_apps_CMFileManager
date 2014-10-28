/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Provides random read-only access to a file.
 * The methods of this interface form a subset of {@link RandomAccessFile}
 * which is required for random read-only access.
 * The default implementation can be found in {@link DefaultReadOnlyFile}.
 *
 * @author  Christian Schlichtherle
 */
public interface ReadOnlyFile extends Closeable {

    /**
     * Returns the length of the file in bytes.
     *
     * @return The length of the file in bytes.
     * @throws IOException On any I/O failure.
     */
    long length() throws IOException;

    /**
     * Returns the current byte position in the file as a zero-based index.
     *
     * @return The current byte position in the file as a zero-based index.
     * @throws IOException On any I/O failure.
     */
    long getFilePointer() throws IOException;

    /**
     * Sets the current byte position in the file as a zero-based index at
     * which the next read occurs.
     * Whether the offset may be set beyond the end of the file is up to
     * the implementor.
     * For example, the constructor of the class {@link DefaultReadOnlyFile}
     * passes {@code "r"} as a parameter to the constructor of its super-class
     * {@link java.io.RandomAccessFile}.
     * With Oracle's JSE implementation, on the Windows platform this
     * implementation allows to seek past the end of file, but on the Linux
     * platform it doesn't.
     *
     * @param pos The current byte position as a zero-based index.
     * @throws IOException If {@code pos} is less than {@code 0} or on any
     *         I/O failure.
     */
    void seek(long pos) throws IOException;

    /**
     * Reads and returns the next byte or -1 if the end of the file has been
     * reached.
     *
     * @return The next byte or -1 if the end of the file has been reached.
     * @throws IOException On any I/O failure.
     */
    int read() throws IOException;

    /**
     * Equivalent to {@link #read(byte[], int, int) read(b, 0, b.length)}.
     *
     * @param  b The buffer to fill with data.
     * @return The total number of bytes read, or {@code -1} if there is
     *         no more data because the end of the file has been reached.
     * @throws IOException On any I/O failure.
     */
    int read(byte[] b) throws IOException;

    /**
     * Reads up to {@code len} bytes of data from this read only file into
     * the given array.
     * This method blocks until at least one byte of input is available unless
     * {@code len} is zero.
     *
     * @param  b The buffer to fill with data.
     * @param  off The start offset of the data.
     * @param  len The maximum number of bytes to read.
     * @return The total number of bytes read, or {@code -1} if there is
     *         no more data because the end of the file has been reached.
     * @throws IOException On any I/O failure.
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Equivalent to {@link #readFully(byte[], int, int) readFully(b, 0, b.length)}.
     *
     * @param  buf the buffer to fill with data.
     * @throws EOFException If less than {@code len} bytes are available
     *         before the end of the file is reached.
     * @throws IOException On any I/O failure.
     */
    void readFully(byte[] buf) throws IOException;

    /**
     * Reads {@code len} bytes into the given buffer at the given position.
     *
     * @param  buf the buffer to fill with data.
     * @param  off the start offset of the data.
     * @param  len the number of bytes to read.
     * @throws EOFException If less than {@code len} bytes are available
     *         before the end of the file is reached.
     * @throws IOException On any I/O failure.
     */
    void readFully(byte[] buf, int off, int len) throws IOException;

    /**
     * Closes this read-only file and releases any non-heap resources
     * allocated for it.
     *
     * @throws IOException On any I/O failure.
     */
    @Override
    void close() throws IOException;
}