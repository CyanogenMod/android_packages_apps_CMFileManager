/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.util.HashMaps;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Drop-in replacement for
 * {@link java.util.zip.ZipOutputStream java.util.zip.ZipOutputStream}.
 * <p>
 * This class starts writing ordinary ZIP32 File Format.
 * It automatically adds ZIP64 extensions if required,
 * i.e. if the file size exceeds 4GB or more than 65535 entries are written.
 * This implies that the class may produce ZIP archive files which cannot
 * be read by older ZIP implementations.
 * <p>
 * If the system property {@code de.schlichtherle.truezip.io.zip.zip64ext}
 * is set to {@code true} (case is ignored),
 * then ZIP64 extensions are always added when writing a ZIP archive file,
 * regardless of its size.
 * This system property is primarily intended for unit testing purposes.
 * During normal operations, it should not be set as many
 * third party tools would not treat the redundant ZIP64 extensions
 * correctly.
 * Note that it's impossible to inhibit ZIP64 extensions.
 * <p>
 * This class provides some limited support for multithreading: You can read
 * its properties from multiple threads, but obviously you cannot concurrently
 * write multiple entries at the same time by different threads.
 *
 * @see     ZipFile
 * @author  Christian Schlichtherle
 */
public class ZipOutputStream extends RawZipOutputStream<ZipEntry> {

    /**
     * The number of entries which can be additionally accomodated by
     * the internal hash map without resizing it, which is {@value}.
     *
     * @since  TrueZIP 7.3
     * @deprecated since TrueZIP 7.5.5
     */
    public static final int OVERHEAD_SIZE = HashMaps.OVERHEAD_SIZE;

    /**
     * The default character set used for entry names and comments in ZIP files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     */
    public static final Charset DEFAULT_CHARSET = Constants.DEFAULT_CHARSET;

    private static final ZipOutputStreamParameters DEFAULT_PARAM
            = new DefaultZipOutputStreamParameters(DEFAULT_CHARSET);

    private ZipCryptoParameters cryptoParameters;

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * using the {@code "UTF-8"} charset.
     *
     * @param  out The output stream to write the ZIP file to.
     */
    public ZipOutputStream(OutputStream out) {
        super(out, null, DEFAULT_PARAM);
    }

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * using the given charset.
     *
     * @param  out The output stream to write the ZIP file to.
     * @param  charset the character set to use.
     */
    public ZipOutputStream(OutputStream out, Charset charset) {
        super(out, null, new DefaultZipOutputStreamParameters(charset));
    }

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * and appends to the given ZIP file.
     *
     * @param  out The output stream to write the ZIP file to.
     *         If {@code appendee} is not {@code null}, then this must be set
     *         up so that it appends to the same ZIP file from which
     *         {@code appendee} is reading.
     * @param  appendee the ZIP file to append to.
     *         This may already be closed.
     */
    public ZipOutputStream(OutputStream out, ZipFile appendee) {
        super(out, appendee, DEFAULT_PARAM);
        if (null == appendee)
            throw new NullPointerException();
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    /**
     * Returns a safe enumeration of clones for all entries written so far.
     * This method takes a snapshot of the collection of all entries and
     * enumerates their clones, so concurrent modifications or state changes
     * do not affect this instance, the returned enumeration or the
     * enumerated ZIP entries.
     *
     * @deprecated Use {@link #iterator()} instead.
     */
    @SuppressWarnings("dep-ann")
	@Override
    public synchronized Enumeration<? extends ZipEntry> entries() {
        class CloneEnumeration implements Enumeration<ZipEntry> {
            final Enumeration<? extends ZipEntry> e
                    = Collections.enumeration(Collections.list(
                        ZipOutputStream.super.entries()));

            @Override
            public boolean hasMoreElements() {
                return e.hasMoreElements();
            }

            @Override
            public ZipEntry nextElement() {
                return e.nextElement().clone();
            }
        } // class CloneEnumeration

        return new CloneEnumeration();
    }

    /**
     * Returns a safe iteration of clones for all entries written to this ZIP
     * file so far.
     * This method takes a snapshot of the collection of all entries and
     * clones them while iterating, so concurrent modifications or state
     * changes do not affect this instance, the returned enumeration or the
     * enumerated ZIP entries.
     * The iteration does not support element removal.
     */
    @Override
    public synchronized Iterator<ZipEntry> iterator() {
        class EntryIterator implements Iterator<ZipEntry> {
            final Iterator<ZipEntry> i;

            EntryIterator() {
                List<ZipEntry> l = new ArrayList<ZipEntry>(ZipOutputStream.super.size());
                Iterator<ZipEntry> si = ZipOutputStream.super.iterator();
                while (si.hasNext())
                    l.add(si.next());
                i = l.iterator();
            }

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public ZipEntry next() {
                return i.next().clone();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        } // class EntryIterator

        return new EntryIterator();
    }

    /**
     * Returns a clone of the entry for the given name or {@code null} if no
     * entry with this name exists.
     *
     * @param name the name of the ZIP entry.
     */
    @Override
    public synchronized ZipEntry getEntry(String name) {
        final ZipEntry entry = super.getEntry(name);
        return entry != null ? entry.clone() : null;
    }

    @Override
    public synchronized void setComment(String comment) {
        super.setComment(comment);
    }

    @Override
    public synchronized String getComment() {
        return super.getComment();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The initial value is {@code ZipEntry#DEFLATED}.
     */
    @Override
    public synchronized int getMethod() {
        return super.getMethod();
    }

    @Override
    public synchronized void setMethod(int method) {
        super.setMethod(method);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The initial value is {@code Deflater#DEFAULT_COMPRESSION}.
     */
    @Override
    public synchronized int getLevel() {
        return super.getLevel();
    }
   
    @Override
    public synchronized void setLevel(int level) {
        super.setLevel(level);
    }

    @Override
    public synchronized ZipCryptoParameters getCryptoParameters() {
        return cryptoParameters;
    }

    /**
     * Sets the parameters for encryption or authentication of entries.
     * <p>
     * Note that only {@link WinZipAesParameters WinZip AES encryption} is
     * currently supported.
     *
     * @param cryptoParameters the parameters for encryption or authentication
     *        of entries.
     * @since TrueZIP 7.3
     */
    public synchronized void setCryptoParameters(
            final ZipCryptoParameters cryptoParameters) {
        this.cryptoParameters = cryptoParameters;
    }

    @Override
    public synchronized long length() {
        return super.length();
    }

    @Override
    public synchronized final boolean isBusy() {
        return super.isBusy();
    }

    @Override
    public synchronized void putNextEntry(
            final ZipEntry entry,
            final boolean process)
    throws IOException {
        super.putNextEntry(entry, process);
    }

    @Override
    public synchronized void write(int b)
    throws IOException {
        super.write(b);
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len)
    throws IOException {
        super.write(b, off, len);
    }

    @Override
    public synchronized void closeEntry() throws IOException {
        super.closeEntry();
    }

    @Override
    public synchronized void finish() throws IOException {
        super.finish();
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
    }
}