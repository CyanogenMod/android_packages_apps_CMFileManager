/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * A replacement for the class {@link FileWriter} for writing plain old files
 * or entries in an archive file.
 * Mind that applications cannot write archive files directly - just their
 * entries!
 *
 * @see    TConfig#setLenient
 * @see    TFileReader
 * @author Christian Schlichtherle
 */
public final class TFileWriter extends OutputStreamWriter {

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     *
     * @param  file a file to write.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(File file) throws FileNotFoundException {
        super(new TFileOutputStream(file));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     * <p>
     * TODO: Remove this redundant constructor in TrueZIP 8.
     *
     * @param  file a file to write.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(TFile file) throws FileNotFoundException {
        super(new TFileOutputStream(file));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     *
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(File file, boolean append) throws FileNotFoundException {
        super(new TFileOutputStream(file, append));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     * <p>
     * TODO: Remove this redundant constructor in TrueZIP 8.
     *
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(TFile file, boolean append) throws FileNotFoundException {
        super(new TFileOutputStream(file, append));
    }

    /**
     * Constructs a new {@code TFile} writer.
     *
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  charset a character set for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O failure.
     * @since  TrueZIP 7.5
     */
    public TFileWriter(File file, boolean append, Charset charset)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), charset);
    }

    /**
     * Constructs a new {@code TFile} writer.
     *
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  encoder an encoder for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(File file, boolean append, CharsetEncoder encoder)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), encoder);
    }

    /**
     * Constructs a new {@code TFile} writer.
     * <p>
     * TODO: Remove this redundant constructor in TrueZIP 8.
     *
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  encoder an encoder for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(TFile file, boolean append, CharsetEncoder encoder)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), encoder);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
