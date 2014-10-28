/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.crypto.CipherReadOnlyFile;
import static de.schlichtherle.truezip.crypto.raes.Constants.LEAD_IN_LENGTH;
import static de.schlichtherle.truezip.crypto.raes.Constants.SIGNATURE;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class implements a {@link de.schlichtherle.truezip.rof.ReadOnlyFile}
 * in order to provide transparent random read only access to the plain text
 * data which has been encrypted and stored in a file according to the
 * Random Access Encryption Specification (RAES).
 * <p>
 * To accomodate the transparent random read access feature, RAES specifies
 * a multistep authentication process:
 * <p>
 * The first step is mandatory and implemented in the constructor of the
 * concrete implementation of this abstract class.
 * For this step only the cipher key and the file length is authenticated,
 * which is fast to process (O(1)).
 * <p>
 * The second step is optional and must be initiated by the client by calling
 * {@link #authenticate}.
 * For this step the entire cipher text is authenticated, which is comparably
 * slow (O(n)).
 * Please note that this step does not require the cipher text to be
 * decrypted first, which features comparably fast processing.
 * <p>
 * So it is up to the application which level of security it needs to
 * provide:
 * Most applications should always call {@code authenticate()} in
 * order to guard against integrity attacks.
 * However, some applications may provide additional (faster) methods for
 * authentication of the pay load, in which case the authentication
 * provided by this class may be safely skipped.
 * <p>
 * Note that this class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying {@code ReadOnlyFile}
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in the base class {@link DecoratingReadOnlyFile}.
 *
 * @see    RaesOutputStream
 * @author Christian Schlichtherle
 */
public abstract class RaesReadOnlyFile extends CipherReadOnlyFile {

    RaesReadOnlyFile(ReadOnlyFile rof) {
        super(rof);
    }

    static short readUByte(final byte[] b, final int off) {
        return (short) (b[off] & 0xff);
    }

    static int readUShort(final byte[] b, final int off) {
        return ((b[off + 1] & 0xff) << 8) | (b[off] & 0xff);
    }

    static long readUInt(final byte[] b, int off) {
        off += 3;
        long v = b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off] & 0xffL;
        return v;
    }

    /**
     * Returns a new {@code RaesReadOnlyFile}.
     *
     * @param  file The file to open for reading the ciphered data.
     * @param  param The {@link RaesParameters} required to access the
     *         RAES type actually found in the file.
     *         If the run time class of this parameter does not match the
     *         required parameter interface according to the RAES type found
     *         in the file, but is an instance of the
     *         {@link RaesParametersProvider} interface, it is used to find
     *         the required RAES parameters.
     *         This is applied recursively.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws RaesParametersException If {@code param} is {@code null} or
     *         no suitable RAES parameters can get found.
     * @throws RaesException If the file is not RAES compatible.
     * @throws IOException on any I/O error.
     */
    public static RaesReadOnlyFile getInstance(
            final File file,
            final RaesParameters param)
    throws IOException {
        final ReadOnlyFile rof = new DefaultReadOnlyFile(file);
        try {
            return getInstance(rof, param);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    /**
     * Returns a new {@code RaesReadOnlyFile}.
     *
     * @param  rof the read only file to decorate for reading the ciphered data.
     * @param  param the {@link RaesParameters} required to access the RAES
     *         type actually found in the file.
     *         If the run time class of this parameter does not match the
     *         required parameter interface according to the RAES type found
     *         in the file, but is an instance of the
     *         {@link RaesParametersProvider} interface, it's queried to find
     *         the required RAES parameters.
     *         This algorithm is recursively applied.
     * @return A new {@code RaesReadOnlyFile}.
     * @throws RaesParametersException If {@code param} is {@code null} or
     *         no suitable RAES parameters can get found.
     * @throws RaesException If the file is not RAES compatible.
     * @throws IOException on any I/O error.
     */
    public static RaesReadOnlyFile getInstance(
            final ReadOnlyFile rof,
            RaesParameters param)
    throws IOException {
        // Load header data.
        final byte[] leadIn = new byte[LEAD_IN_LENGTH];
        rof.seek(0);
        rof.readFully(leadIn);

        // Check header data.
        if (readUInt(leadIn, 0) != SIGNATURE)
            throw new RaesException("No RAES signature!");
        final int type = readUByte(leadIn, 4);
        switch (type) {
            case 0:
                return new Type0RaesReadOnlyFile(rof,
                        parameters(Type0RaesParameters.class, param));
            default:
                throw new RaesException("Unknown RAES type: " + type);
        }
    }

    private static <P extends RaesParameters> P parameters(
            final Class<P> type,
            RaesParameters param)
    throws RaesParametersException {
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (type.isInstance(param)) {
                return type.cast(param);
            } else if (param instanceof RaesParametersProvider) {
                param = ((RaesParametersProvider) param).get(type);
            } else {
                break;
            }
        }
        throw new RaesParametersException("No suitable RAES parameters available!");
    }

    /**
     * Returns the key strength which is actually used to decrypt the data
     * of the RAES file.
     */
    public abstract KeyStrength getKeyStrength();

    /**
     * Authenticates all encrypted data in this read only file.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     * <p>
     * This is the second, optional step of authentication.
     * The first, mandatory step is to compute the cipher key and cipher text
     * length only and must already have been successfully completed in the
     * constructor.
     *
     * @throws RaesAuthenticationException If the computed MAC does not match
     *         the MAC declared in the RAES file.
     * @throws IOException On any I/O related issue.
     */
    public abstract void authenticate() throws IOException;
}
