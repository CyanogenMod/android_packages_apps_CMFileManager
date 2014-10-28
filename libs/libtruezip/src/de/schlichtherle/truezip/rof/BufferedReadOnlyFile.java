/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.io.Streams;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A {@link ReadOnlyFile} implementation which provides buffered random read
 * only access to another {@code ReadOnlyFile}.
 * <p>
 * Note that this class implements a virtual file pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using the decorating read only file, then you should
 * synchronize their file pointers using the following idiom:
 * <pre>
 *     ReadOnlyFile rof = new DefaultReadOnlyFile(new File("HelloWorld.java"));
 *     try {
 *         ReadOnlyFile brof = new BufferedReadOnlyFile(rof);
 *         try {
 *             // Do any file input on frof here...
 *             brof.seek(1);
 *         } finally {
 *             // Synchronize the file pointers.
 *             rof.seek(brof.getFilePointer());
 *         }
 *         // This assertion would fail if we hadn't done the file pointer
 *         // synchronization!
 *         assert rof.getFilePointer() == 1;
 *     } finally {
 *         rof.close();
 *     }
 * </pre>
 *
 * @author Christian Schlichtherle
 */
public class BufferedReadOnlyFile extends DecoratingReadOnlyFile {

    private static final long INVALID = Long.MIN_VALUE;

    /** The default buffer length of the window to the file. */
    public static final int WINDOW_LEN = Streams.BUFFER_SIZE;

    /**
     * Returns the smaller parameter.
     *
     * @deprecated Use {@link Math#min(long, long) instead.
     */
    protected static long min(long a, long b) {
        return a < b ? a : b;
    }

    /**
     * Returns the greater parameter.
     *
     * @deprecated Use {@link Math#max(long, long) instead.
     */
    protected static long max(long a, long b) {
        return a < b ? b : a;
    }

    /** The virtual file pointer. */
    private long pos;

    /**
     * The position in the decorated file data where the buffer starts.
     * This is always a multiple of the buffer size.
     */
    private long bufferStart = INVALID;

    /** The buffer for the file data. */
    private final byte[] buffer;

    /**
     * Constructs a new buffered read only file.
     *
     * @param  file The file to read.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException on any I/O error.
     */
    public BufferedReadOnlyFile(File file) throws IOException {
        this(null, file, WINDOW_LEN);
    }

    /**
     * Constructs a new buffered read only file.
     *
     * @param  file the file to read.
     * @param  bufferSize the size of the buffer window in bytes.
     * @throws FileNotFoundException if the file cannot get opened for reading.
     * @throws IOException on any I/O error.
     */
    public BufferedReadOnlyFile(
            final File file,
            final int bufferSize)
    throws IOException {
        this(null, file, bufferSize);
    }

    /**
     * Constructs a new buffered read only file.
     *
     * @param rof the read only file to read.
     * @throws FileNotFoundException if the file cannot get opened for reading.
     * @throws IOException on any I/O error.
     */
    public BufferedReadOnlyFile(final ReadOnlyFile rof)
    throws IOException {
        this(rof, null, WINDOW_LEN);
    }

    /**
     * Constructs a new buffered read only file.
     *
     * @param rof the read only file to read.
     * @param bufferSize the size of the buffer window in bytes.
     * @throws FileNotFoundException if the file cannot get opened for reading.
     * @throws IOException on any I/O error.
     */
    public BufferedReadOnlyFile(
            final ReadOnlyFile rof,
            final int bufferSize)
    throws IOException {
        this(rof, null, bufferSize);
    }

    private BufferedReadOnlyFile(
            final ReadOnlyFile rof,
            final File file,
            final int bufferSize)
    throws IOException {
        super(check(rof, file, bufferSize));
        buffer = new byte[bufferSize];
    }

    /** Check constructor parameters (fail fast). */
    private static ReadOnlyFile check(
            final ReadOnlyFile rof,
            final File file,
            final int windowLen)
    throws FileNotFoundException {
        if (0 >= windowLen)
            throw new IllegalArgumentException();
        if (null != rof) {
            assert null == file;
            return rof;
        } else {
            return new DefaultReadOnlyFile(file);
        }
    }

    /**
     * Asserts that this file is open.
     *
     * @throws IOException If the preconditions do not hold.
     */
    protected final void assertOpen() throws IOException {
        if (null == delegate)
            throw new IOException("File is closed!");
    }

    @Override
    public int read()
    throws IOException {
        // Check state.
        assertOpen();
        if (pos >= delegate.length())
            return -1;

        // Position window and return its data.
        positionBuffer();
        return buffer[(int) (pos++ % buffer.length)] & 0xff;
    }

    @Override
    public int read(final byte[] dst, final int offset, final int remaining)
    throws IOException {
        // Check no-op first for compatibility with RandomAccessFile.
        if (remaining <= 0)
            return 0;

        // Check is open and not at EOF.
        final long length = length();
        if (getFilePointer() >= length) // ensure pos is initialized, but do NOT cache!
            return -1;

        // Check parameters.
        if (0 > (offset | remaining | dst.length - offset - remaining))
	    throw new IndexOutOfBoundsException();

        // Read of buffer data.
        int total = 0; // amount of data copied to dst
        final int bufferSize = buffer.length;
        while (total < remaining && pos < length) {
            positionBuffer();
            final int bufferPos = (int) (pos - bufferStart);
            int bufferLimit = Math.min(remaining - total, bufferSize - bufferPos);
            bufferLimit = (int) Math.min(bufferLimit, length - pos);
            assert bufferLimit > 0;
            System.arraycopy(buffer, bufferPos, dst, offset + total, bufferLimit);
            total += bufferLimit;
            pos += bufferLimit;
        }

        return total;
    }

    @Override
    public long getFilePointer() throws IOException {
        assertOpen();
        return pos;
    }

    @Override
    public void seek(final long pos) throws IOException {
        assertOpen();
        if (pos < 0)
            throw new IOException("File pointer must not be negative!");
        final long length = delegate.length();
        if (pos > length)
            throw new IOException("File pointer (" + pos
                    + ") is larger than file length (" + length + ")!");
        this.pos = pos;
    }

    @Override
    public long length() throws IOException {
        assertOpen();
        return delegate.length();
    }

    /**
     * Closes this read only file.
     * As a side effect, this will set the reference to the decorated read
     * only file ({@link #delegate} to {@code null}.
     */
    @Override
    public void close()
    throws IOException {
        // Order is important here!
        if (null == delegate)
            return;
        delegate.close();
        delegate = null;
    }

    /**
     * Positions the window so that the block containing the current virtual
     * file pointer in the encrypted file is entirely contained in it.
     *
     * @throws IOException on any I/O error.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        final byte[] buffer = this.buffer;
        final int bufferSize = buffer.length;

        // Check position.
        final long pos = this.pos;
        long bufferStart = this.bufferStart;
        final long nextBufferStart = bufferStart + bufferSize;
        if (bufferStart <= pos && pos < nextBufferStart)
            return;

        try {
            final ReadOnlyFile delegate = this.delegate;

            // Move position.
            // Round down to multiple of buffer size.
            this.bufferStart = bufferStart = pos / bufferSize * bufferSize;
            if (bufferStart != nextBufferStart)
                delegate.seek(bufferStart);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of ReadOnlyFile's
            // contract.
            int total = 0;
            do {
                int read = delegate.read(buffer, total, bufferSize - total);
                if (read < 0)
                    break;
                total += read;
            } while (total < bufferSize);
        } catch (final IOException ex) {
            this.bufferStart = INVALID;
            throw ex;
        }
    }
}
