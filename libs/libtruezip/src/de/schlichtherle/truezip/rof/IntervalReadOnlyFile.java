/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import java.io.IOException;

/**
 * A decorating read only file which is limited to read an interval of its
 * decorated read only file.
 * <p>
 * Note that this class implements a virtual file pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using this decorating read only file, then you should not
 * assume a particular position of the file pointer in the decorated read only
 * file.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public class IntervalReadOnlyFile extends DecoratingReadOnlyFile {

    private final long offset;
    private final long length;
    private final boolean exclusive;

    /**
     * The virtual file pointer in the file data.
     * This is relative to {@link #offset}.
     */
    private long fp;

    /**
     * Constructs a new interval read only file starting at the current
     * position of the file pointer in the decorated read only file.
     * <p>
     * Note that this constructor assumes that it has exclusive access to the
     * decorated read only file.
     * Concurrent modification of the file pointer in the decorated read only
     * file may corrupt the input of this decorating read only file!
     *
     * @param rof the read only file to decorate.
     * @param length the length of the interval.
     * @throws IOException On any I/O failure.
     */
    public IntervalReadOnlyFile(
            final ReadOnlyFile rof,
            final long length)
    throws IOException {
        this(rof, rof.getFilePointer(), length, true);
    }

    /**
     * Constructs a new interval read only file starting at the given position
     * of the file pointer in the given decorated read only file.
     * <p>
     * Note that this constructor assumes that it does <em>not</em> have
     * exclusive access to the decorated read only file and positions the file
     * pointer in the decorated read only file before each read operation!
     *
     * @param rof the read only file to decorate.
     * @param offset the start of the interval.
     * @param length the length of the interval.
     * @throws IOException On any I/O failure.
     */
    public IntervalReadOnlyFile(
            final ReadOnlyFile rof,
            final long offset,
            final long length)
    throws IOException {
        this(rof, offset, length, false);
    }
   
    /**
     * Constructs a new interval read only file starting at the given position
     * of the file pointer in the given decorated read only file.
     *
     * @param rof the read only file to decorate.
     * @param offset the start of the interval.
     * @param length the length of the interval.
     * @param exclusive whether this decorating read only file may assume it
     *        has exclusive access to the decorated read only file or not.
     *        If this is {@code true}, then the position of the file pointer
     *        in the decorated read only file must be {@code start}!
     *        If this is {@code false}, then the file pointer in the decorated
     *        read only file gets positioned before each read operation.
     * @throws IOException On any I/O failure.
     */
    private IntervalReadOnlyFile(
            final ReadOnlyFile rof,
            final long offset,
            final long length,
            final boolean exclusive)
    throws IOException {
        super(rof);
        if (offset < 0 || length < 0 || rof.length() < offset + length)
            throw new IllegalArgumentException();
        this.offset = offset;
        this.length = length;
        this.exclusive = exclusive;
    }

    @Override
    public long length() throws IOException {
        // Check state.
        final long length = this.length;
        if (this.delegate.length() < this.offset + length)
            throw new IOException("Read Only File has been changed!");

        return length;
    }

    @Override
    public long getFilePointer()
    throws IOException {
        // Check state.
        this.delegate.getFilePointer();

        return this.fp;
    }

    @Override
    public void seek(final long fp)
    throws IOException {
        // Check parameters.
        if (fp < 0)
            throw new IOException("File pointer must not be negative!");
        final long length = this.length;
        if (fp > length)
            throw new IOException("File pointer (" + fp
                    + ") is larger than file length (" + length + ")!");

        // Operate.
        this.delegate.seek(fp + this.offset);
        this.fp = fp;
    }

    @Override
    public int read()
    throws IOException {
        // Check state.
        long fp = this.fp;
        if (fp >= this.length)
            return -1;

        // Operate.
        if (!this.exclusive)
            this.delegate.seek(fp + this.offset);
        final int read = this.delegate.read();

        // Update state.
        this.fp = fp + 1;
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len)
    throws IOException {
        if (0 == len) {
            // Be fault-tolerant and compatible to RandomAccessFile, even if
            // the decorated read only file has been closed before.
            return 0;
        }

        // Check and modify parameters.
        if (0 > (off | len | buf.length - off - len))
	    throw new IndexOutOfBoundsException();
        long fp = this.fp;
        final long length = this.length;
        if (fp + len > length)
            len = (int) (length - fp);

        // Operate.
        if (!this.exclusive)
            this.delegate.seek(fp + this.offset);
        final int read = this.delegate.read(buf, off, len);

        // Post-check state.
        if (0 == len) {
            // This was an attempt to read past the end of the file.
            // This could have been checked in advance, but its still desirable
            // to have the delegate test its state - it might throw an
            // IOException if it has been closed before.
            assert 0 >= read;
            return -1;
        }
        assert 0 < read;

        // Update state.
        this.fp = fp + read;
        return read;
    }

    /**
     * Closes the decorated read only file if and only if it is exclusively
     * accessed by this decorating read only file.
     *
     * @throws IOException On any I/O error.
     */
    @Override
    public void close() throws IOException {
        if (this.exclusive)
            this.delegate.close();
    }
}