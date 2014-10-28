/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipException;

/**
 * Drop-in replacement for {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
 * <p>
 * Where the constructors of this class accept a {@code charset}
 * parameter, this is used to decode comments and entry names in the ZIP file.
 * However, if an entry has bit 11 set in its General Purpose Bit Flag,
 * then this parameter is ignored and "UTF-8" is used for this entry.
 * This is in accordance to Appendix D of PKWARE's ZIP File Format
 * Specification, version 6.3.0 and later.
 * <p>
 * This class is able to skip a preamble like the one found in self extracting
 * archives.
 * <p>
 * Note that the entries returned by this class are instances of
 * {@code de.schlichtherle.truezip.io.zip.ZipEntry} instead of
 * {@code java.util.zip.ZipEntry}.
 * <p>
 * This class is thread-safe.
 *
 * @see     ZipOutputStream
 * @author  Christian Schlichtherle
 */
public class ZipFile extends RawZipFile<ZipEntry> {

    private volatile ZipCryptoParameters cryptoParameters;

    private final String name;

    /**
     * Equivalent to {@link #ZipFile(String, Charset, boolean, boolean)
     * ZipFile(name, DEFAULT_CHARSET, true, false)}
     */
    public ZipFile(String path)
    throws IOException {
        this(path, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipFile(String, Charset, boolean, boolean)
     * ZipFile(name, charset, true, false)}
     */
    public ZipFile(String path, Charset charset)
    throws IOException {
        this(path, charset, true, false);
    }

    /**
     * Opens the ZIP file identified by the given path name for reading its
     * entries.
     *
     * @param path the path name of the file.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code name} cannot get opened for
     *         reading.
     * @throws ZipException if {@code name} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipFile(
            final String path,
            final Charset charset,
            final boolean preambled,
            final boolean postambled)
    throws IOException {
        super(  new DefaultReadOnlyFilePool(path),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = path;
    }

    /**
     * Equivalent to {@link #ZipFile(File, Charset, boolean, boolean)
     * ZipFile(file, DEFAULT_CHARSET, true, false)}
     */
    public ZipFile(File file)
    throws IOException {
        this(file, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipFile(File, Charset, boolean, boolean)
     * ZipFile(file, charset, true, false)}
     */
    public ZipFile(File file, Charset charset)
    throws IOException {
        this(file, charset, true, false);
    }

    /**
     * Opens the given {@link File} for reading its entries.
     *
     * @param file the file.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code file} cannot get opened for
     *         reading.
     * @throws ZipException if {@code file} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipFile(
            final File file,
            final Charset charset,
            final boolean preambled,
            final boolean postambled)
    throws IOException {
        super(  new DefaultReadOnlyFilePool(file),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = file.toString();
    }

    /**
     * Equivalent to {@link #ZipFile(ReadOnlyFile, Charset, boolean, boolean)
     * ZipFile(rof, DEFAULT_CHARSET, true, false)}
     */
    public ZipFile(ReadOnlyFile rof)
    throws IOException {
        this(rof, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipFile(ReadOnlyFile, Charset, boolean, boolean)
     * ZipFile(rof, charset, true, false)}
     */
    public ZipFile(ReadOnlyFile rof, Charset charset)
    throws IOException {
        this(rof, charset, true, false);
    }

    /**
     * Opens the given {@link ReadOnlyFile} for reading its entries.
     *
     * @param rof the random access read only file.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code rof} cannot get opened for
     *         reading.
     * @throws ZipException if {@code rof} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipFile(
            ReadOnlyFile rof,
            Charset charset,
            boolean preambled,
            boolean postambled)
    throws IOException {
        super(rof, new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = rof.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method is <em>not</em> thread-safe!
     */
    @Override
    public void recoverLostEntries() throws IOException {
        super.recoverLostEntries();
    }

    /**
     * Returns the {@link Object#toString() string representation} of whatever
     * input source object was used to construct this ZIP file.
     * For {@link String} and {@link File} objects, this is a path name.
     */
    public String getName() {
        return name;
    }

    /**
     * Enumerates clones of all entries in this ZIP file.
     *
     * @see #iterator()
     */
    public Enumeration<? extends ZipEntry> entries() {
        class CloneEnumeration implements Enumeration<ZipEntry> {
            final Iterator<ZipEntry> i = ZipFile.super.iterator();

            @Override
            public boolean hasMoreElements() {
                return i.hasNext();
            }

            @Override
            public ZipEntry nextElement() {
                return i.next().clone();
            }
        } // CloneEnumeration

        return new CloneEnumeration();
    }

    /**
     * Iterates through clones for all entries in this ZIP file.
     * The iteration does not support element removal.
     */
    @Override
    public Iterator<ZipEntry> iterator() {
        class EntryIterator implements Iterator<ZipEntry> {
            final Iterator<ZipEntry> i = ZipFile.super.iterator();

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
        } // EntryIterator

        return new EntryIterator();
    }

    /**
     * Returns a clone of the entry for the given name or {@code null} if no
     * entry with this name exists.
     *
     * @param name the name of the ZIP entry.
     */
    @Override
    public ZipEntry getEntry(String name) {
        final ZipEntry ze = super.getEntry(name);
        return ze != null ? ze.clone() : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized InputStream getPreambleInputStream() throws IOException {
        return new de.schlichtherle.truezip.io.SynchronizedInputStream(super.getPreambleInputStream(),
                this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized InputStream getPostambleInputStream() throws IOException {
        return new de.schlichtherle.truezip.io.SynchronizedInputStream(super.getPostambleInputStream(),
                this);
    }

    @Override
    public synchronized boolean busy() {
        return super.busy();
    }

    @Override
    public ZipCryptoParameters getCryptoParameters() {
        return cryptoParameters;
    }

    /**
     * Sets the parameters for encryption or authentication of entries.
     *
     * @param cryptoParameters the parameters for encryption or authentication
     *        of entries.
     * @since TrueZIP 7.3
     */
    public void setCryptoParameters(
            final ZipCryptoParameters cryptoParameters) {
        this.cryptoParameters = cryptoParameters;
    }

    @Override
    @SuppressWarnings({"deprecation", "resource"})
    protected synchronized InputStream getInputStream(
            String name, Boolean check, boolean process)
    throws IOException {
        final InputStream in = super.getInputStream(name, check, process);
        return in == null ? null : new de.schlichtherle.truezip.io.SynchronizedInputStream(in, this);
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
    }

    /**
     * A pool which allocates {@link DefaultReadOnlyFile} objects for the
     * file provided to its constructor.
     */
    private static final class DefaultReadOnlyFilePool
    implements Pool<ReadOnlyFile, IOException> {
        final File file;

        DefaultReadOnlyFilePool(String name) {
            this(new File(name));
        }

        DefaultReadOnlyFilePool(final File file) {
            this.file = file;
        }

        @Override
        public ReadOnlyFile allocate() throws IOException {
            return new DefaultReadOnlyFile(file);
        }

        @Override
        public void release(ReadOnlyFile rof) throws IOException {
            rof.close();
        }
    } // DefaultReadOnlyFilePool
}