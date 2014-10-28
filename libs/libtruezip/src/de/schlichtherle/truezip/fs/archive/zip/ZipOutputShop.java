/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.io.*;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.JointIterator;
import de.schlichtherle.truezip.zip.RawZipOutputStream;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.zip.ZipEntry.UNKNOWN;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

/**
 * An output shop for writing ZIP files.
 * This output shop can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link MultiplexedOutputShop} to overcome this limitation.
 *
 * @see    ZipInputShop
 * @author Christian Schlichtherle
 */
public class ZipOutputShop
extends RawZipOutputStream<ZipDriverEntry>
implements OutputShop<ZipDriverEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private IOPool.Entry<?> postamble;
    private ZipDriverEntry bufferedEntry;
    private ZipCryptoParameters param;

    public ZipOutputShop(   final ZipDriver driver,
                            final FsModel model,
                            final OutputStream out,
                            final ZipInputShop source)
    throws IOException {
        super(  out,
                null != source && source.isAppendee() ? source : null,
                driver);
        if (null == model)
            throw new NullPointerException();
        this.driver = driver;
        this.model = model;
        if (null != source) {
            if (!source.isAppendee()) {
                // Retain comment and preamble of input ZIP archive.
                super.setComment(source.getComment());
                if (0 < source.getPreambleLength()) {
                    final InputStream in = source.getPreambleInputStream();
                    try {
                        Streams.cat(in,
                                source.offsetsConsiderPreamble() ? this : out);
                    } finally {
                        in.close();
                    }
                }
            }
            // Retain postamble of input ZIP file.
            if (0 < source.getPostambleLength()) {
                this.postamble = getIOPool().allocate();
                Streams.copy(   source.getPostambleInputStream(),
                                this.postamble.getOutputSocket().newOutputStream());
            }
        }
    }

    /**
     * Returns the file system model provided to the constructor.
     *
     * @return The file system model provided to the constructor.
     * @since  TrueZIP 7.3
     */
    public FsModel getModel() {
        return model;
    }

    private IOPool<?> getIOPool() {
        return driver.getPool();
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        ZipCryptoParameters param = this.param;
        if (null == param)
            this.param = param = driver.zipCryptoParameters(this);
        return param;
    }

    @Override
    public int getSize() {
        return super.size() + (null != this.bufferedEntry ? 1 : 0);
    }

    @Override
    public Iterator<ZipDriverEntry> iterator() {
        final ZipDriverEntry tempEntry = this.bufferedEntry;
        if (null == tempEntry)
            return super.iterator();
        return new JointIterator<ZipDriverEntry>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public ZipDriverEntry getEntry(final String name) {
        ZipDriverEntry entry = super.getEntry(name);
        if (null != entry)
            return entry;
        entry = this.bufferedEntry;
        return null != entry && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipDriverEntry> getOutputSocket(final ZipDriverEntry local) { // local target
        if (null == local)
            throw new NullPointerException();

        final class Output extends OutputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry getLocalTarget() {
                return local;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(local.getName());
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local, false);
                }
                final boolean process = updateProperties(local, getPeerTarget());
                if (STORED == local.getMethod()) {
                    if (UNKNOWN == local.getCrc()
                            || UNKNOWN == local.getSize()
                            || UNKNOWN == local.getCompressedSize()) {
                        assert process : "The CRC-32, size and compressed size properties must be set when using RDC!";
                        return new BufferedEntryOutputStream(local);
                    }
                }
                return new EntryOutputStream(local, process);
            }
        } // Output

        return new Output();
    }

    boolean updateProperties(
            final ZipDriverEntry local,
            final Entry peer) {
        boolean process = true;
        if (UNKNOWN == local.getTime())
            local.setTime(System.currentTimeMillis());
        if (peer != null) {
            if (UNKNOWN == local.getSize())
                local.setSize(peer.getSize(DATA));
            if (peer instanceof ZipDriverEntry) {
                // Set up entry attributes for Raw Data Copying (RDC).
                final ZipDriverEntry zpeer = (ZipDriverEntry) peer;
                process = driver.process(this, local, zpeer);
                if (!process) {
                    local.setPlatform(zpeer.getPlatform());
                    local.setEncrypted(zpeer.isEncrypted());
                    local.setMethod(zpeer.getMethod());
                    local.setCrc(zpeer.getCrc());
                    local.setSize(zpeer.getSize());
                    local.setCompressedSize(zpeer.getCompressedSize());
                    local.setExtra(zpeer.getExtra());
                }
            }
        }
        if (0 == local.getSize()) {
            process = true;
            local.clearEncryption();
            local.setMethod(STORED);
            local.setCrc(0);
            local.setCompressedSize(0);
        }
        return process;
    }

    private static final class DirectoryTemplate implements Entry {
        static final DirectoryTemplate INSTANCE = new DirectoryTemplate();

        @Override
        public String getName() {
            return "/";
        }

        @Override
        public long getSize(Size type) {
            return 0;
        }

        @Override
        public long getTime(Access type) {
            return UNKNOWN;
        }
    } // DirectoryTemplate

    /**
     * Returns whether this ZIP output shop is busy writing an archive entry
     * or not.
     *
     * @return Whether this ZIP output shop is busy writing an archive entry
     *         or not.
     */
    @Override
    public final boolean isBusy() {
        return super.isBusy() || null != this.bufferedEntry;
    }

    /**
     * Retains the postamble of the source source ZIP file, if any.
     */
    @Override
    public void close() throws IOException {
        super.finish();
        final IOPool.Entry<?> postamble = this.postamble;
        if (null != postamble) {
            this.postamble = null;
            final InputSocket<?> input = postamble.getInputSocket();
            IOException ex = null;
            try {
                final InputStream in = input.newInputStream();
                try {
                    // If the output ZIP file differs in length from the
                    // input ZIP file then pad the output to the next four
                    // byte boundary before appending the postamble.
                    // This might be required for self extracting files on
                    // some platforms, e.g. Windows x86.
                    final long ol = length();
                    final long ipl = input.getLocalTarget().getSize(DATA);
                    if ((ol + ipl) % 4 != 0)
                        write(new byte[4 - (int) (ol % 4)]);

                    Streams.cat(in, this);
                } catch (final IOException ex2) {
                    ex = ex2;
                    throw ex2;
                } finally {
                    try {
                        in.close();
                    } catch (final IOException ex2) {
                        if (null == ex)
                            throw ex2;
                    }
                }
            } catch (final IOException ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    postamble.release();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                }
            }
       }
        super.close();
    }

    /**
     * This entry output stream writes directly to this output shop.
     * It can only be used if this output shop is not currently busy with
     * writing another entry and the entry holds enough information to write
     * the entry header.
     * These preconditions are checked by
     * {@link #getOutputSocket(ZipDriverEntry)}.
     */
    private final class EntryOutputStream extends DisconnectingOutputStream {
        boolean closed;

        EntryOutputStream(final ZipDriverEntry entry, final boolean process)
        throws IOException {
            super(ZipOutputShop.this);
            putNextEntry(entry, process);
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            closeEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the ZIP archive entry to an
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted.
     */
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IOPool.Entry<?> buffer;
        final ZipDriverEntry local;
        boolean closed;

        BufferedEntryOutputStream(final ZipDriverEntry local)
        throws IOException {
            super(null);
            assert STORED == local.getMethod();
            this.local = local;
            final IOPool.Entry<?> buffer = this.buffer = getIOPool().allocate();
            try {
                this.delegate = new CheckedOutputStream(
                        buffer.getOutputSocket().newOutputStream(),
                        new CRC32());
            } catch (final IOException ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    // Ignore
                }
                throw ex;
            }
            bufferedEntry = local;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            bufferedEntry = null;
            delegate.close();
            updateProperties();
            storeBuffer();
        }

        void updateProperties() {
            final ZipDriverEntry local = this.local;
            final IOPool.Entry<?> buffer = this.buffer;
            local.setCrc(((CheckedOutputStream) delegate).getChecksum().getValue());
            final long length = buffer.getSize(DATA);
            local.setSize(length);
            local.setCompressedSize(length);
            ZipOutputShop.this.updateProperties(local, buffer);
        }

        void storeBuffer() throws IOException {
            final IOPool.Entry<?> buffer = this.buffer;
            final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                    = SequentialIOExceptionBuilder.create(IOException.class, SequentialIOException.class);
            try {
                final InputStream in = buffer.getInputSocket().newInputStream();
                try {
                    final ZipOutputShop zos = ZipOutputShop.this;
                    zos.putNextEntry(local);
                    try {
                        Streams.cat(in, zos);
                    } catch (final InputException ex) { // NOT IOException!
                        builder.warn(ex);
                    }
                    try {
                        zos.closeEntry();
                    } catch (final IOException ex) {
                        builder.warn(ex);
                    }
                } catch (final IOException ex) {
                    builder.warn(ex);
                } finally {
                    try {
                        in.close();
                    } catch (final IOException ex) {
                        builder.warn(ex);
                    }
                }
            } finally {
                try {
                    buffer.release();
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            }
            builder.check();
        }
    } // BufferedEntryOutputStream
}
