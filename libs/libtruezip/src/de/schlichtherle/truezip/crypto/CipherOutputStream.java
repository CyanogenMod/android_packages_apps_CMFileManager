/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libtruezip.lcrypto.crypto.BufferedBlockCipher;
import libtruezip.lcrypto.crypto.InvalidCipherTextException;

/**
 * Similar to {@code javax.crypto.CipherOutputStream} with some exceptions:
 * <ul>
 * <li>This implementation is based on Bouncy Castle's lightweight crypto API
 *     and uses a {@link BufferedBlockCipher} for ciphering.
 * <li>The {@link #cipher} used for encryption or decryption is accessible to
 *     subclasses.
 * <li>The {@code flush()} method just flushes the underlying output stream
 *     and has no effect on the cipher.
 * <li>A {@link #finish()} method has been added to allow finishing the output
 *     (probably producing padding bytes) without closing the output.
 *     This could be used in a subclass to produce a trailer with additional
 *     information about the ciphered data (e.g. a MAC).
 * </ul>
 *
 * @see    CipherReadOnlyFile
 * @author Christian Schlichtherle
 */
public class CipherOutputStream extends DecoratingOutputStream {

    /** The buffered block cipher used for preprocessing the output. */
    protected BufferedBlockCipher cipher;

    /**
     * The cipher output buffer used for preprocessing the output
     * to the decorated stream.
     * This buffer is autosized to the largest buffer written to this stream.
     */
    private byte[] cipherOut = new byte[0];

    /**
     * Creates a new cipher output stream.
     * Please note that unlike {@code javax.crypto.CipherOutputStream},
     * the cipher does not need to be initialized before calling this
     * constructor.
     * However, the cipher must be initialized before anything is actually
     * written to this stream or before this stream is closed.
     *
     * @param out The output stream to write the encrypted or decrypted data to.
     *        Maybe {@code null} for subsequent initialization by a sub-class.
     * @param cipher The cipher to use for encryption or decryption.
     *        Maybe {@code null} for subsequent initialization by a sub-class.
     */
    public CipherOutputStream(
            final OutputStream out,
            final BufferedBlockCipher cipher) {
        super(out);
        this.cipher = cipher;
    }

    /**
     * Asserts that this cipher output stream is in open state, which requires
     * that {@link #cipher} is not {@code null}.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private void checkOpen() throws IOException {
        if (null == cipher)
            throw new IOException("cipher output stream is not in open state");
    }

    /**
     * Ciphers and writes the given byte to the underlying output stream.
     *
     * @param  b The byte to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final int b)
    throws IOException {
        checkOpen();

        int cipherLen = cipher.getUpdateOutputSize(1);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            this.cipherOut = cipherOut = new byte[cipherLen];
        cipherLen = cipher.processByte((byte) b, cipherOut, 0);
        if (cipherLen > 0)
            delegate.write(cipherOut, 0, cipherLen);
    }

    /**
     * Ciphers and writes the contents of the given byte array to the
     * underlying output stream.
     *
     * @param  buf The buffer holding the data to cipher and write.
     * @param  off The start offset of the data in the buffer.
     * @param  len The number of bytes to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final byte[] buf, final int off, final int len)
    throws IOException {
        checkOpen();

        int cipherLen = cipher.getUpdateOutputSize(len);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            this.cipherOut = cipherOut = new byte[cipherLen];
        cipherLen = cipher.processBytes(buf, off, len, cipherOut, 0);
        delegate.write(cipherOut, 0, cipherLen);
    }

    /**
     * Finishes and voids this cipher output stream.
     * Calling this method causes all remaining buffered bytes to get written
     * and padding to get added if necessary.
     * <p>
     * Note that after a call to this method only {@link #close()} may get
     * called on this cipher output stream
     * The result of calling any other method (including this one) is undefined!
     *
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed, an I/O error occured the cipher
     *         text is invalid, i.e. required padding information is missing.
     */
    protected void finish() throws IOException {
        checkOpen();

        int cipherLen = cipher.getOutputSize(0);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            this.cipherOut = cipherOut = new byte[cipherLen];
        try {
            cipherLen = cipher.doFinal(cipherOut, 0);
        } catch (InvalidCipherTextException ex) {
            throw new IOException(ex);
        }
        delegate.write(cipherOut, 0, cipherLen);
    }

    /**
     * Closes this output stream and releases any resources associated with it.
     * Upon the first call to this method, {@link #finish()} gets called and
     * {@link #cipher} gets set to {@code null} upon success.
     * Next, the {@link #delegate} gets unconditionally
     * {@linkplain #close() closed}.
     *
     * @throws IOException On any I/O failure.
     */
    @Override
    public void close() throws IOException {
        if (null != cipher) {
            finish();
            cipher = null;
        }
        delegate.close();
    }
}
