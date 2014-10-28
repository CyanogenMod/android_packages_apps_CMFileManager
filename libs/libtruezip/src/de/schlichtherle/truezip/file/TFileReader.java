/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * A replacement for the class {@link FileReader} for reading plain old files
 * or entries in an archive file.
 * Mind that applications cannot read archive files directly - just their
 * entries!
 *
 * @see    TFileWriter
 * @author Christian Schlichtherle
 */
public final class TFileReader extends InputStreamReader {

    /**
     * Constructs a new {@code TFile} reader.
     * This reader uses the default character set for decoding bytes to
     * characters.
     *
     * @param  file a file to read.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileReader(File file) throws FileNotFoundException {
	super(new TFileInputStream(file));
    }

    /**
     * Constructs a new {@code TFile} reader.
     * This reader uses the default character set for decoding bytes to
     * characters.
     * <p>
     * TODO: Remove this redundant constructor in TrueZIP 8.
     *
     * @param  file a file to read.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileReader(TFile file) throws FileNotFoundException {
	super(new TFileInputStream(file));
    }

    /**
     * Constructs a new {@code TFile} reader.
     *
     * @param  file a file to read.
     * @param  charset a character set for decoding bytes to characters.
     * @throws FileNotFoundException on any I/O failure.
     * @since  TrueZIP 7.5
     */
    public TFileReader(File file, Charset charset)
    throws FileNotFoundException {
	super(new TFileInputStream(file), charset);
    }

    /**
     * Constructs a new {@code TFile} reader.
     *
     * @param  file a file to read.
     * @param  decoder a decoder for decoding bytes to characters.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileReader(File file, CharsetDecoder decoder)
    throws FileNotFoundException {
	super(new TFileInputStream(file), decoder);
    }

    /**
     * Constructs a new {@code TFile} reader.
     * <p>
     * TODO: Remove this redundant constructor in TrueZIP 8.
     *
     * @param  file a file to read.
     * @param  decoder a decoder for decoding bytes to characters.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileReader(TFile file, CharsetDecoder decoder)
    throws FileNotFoundException {
	super(new TFileInputStream(file), decoder);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
