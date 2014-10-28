/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.crypto.SICSeekableBlockCipher;
import de.schlichtherle.truezip.crypto.SeekableBlockCipher;
import de.schlichtherle.truezip.crypto.SuspensionPenalty;
import static de.schlichtherle.truezip.crypto.raes.Constants.AES_BLOCK_SIZE_BITS;
import static de.schlichtherle.truezip.crypto.raes.Constants.ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.ArrayHelper;
import java.io.EOFException;
import java.io.IOException;
import libtruezip.lcrypto.crypto.Mac;
import libtruezip.lcrypto.crypto.PBEParametersGenerator;
import static libtruezip.lcrypto.crypto.PBEParametersGenerator.PKCS12PasswordToBytes;
import libtruezip.lcrypto.crypto.digests.SHA256Digest;
import libtruezip.lcrypto.crypto.engines.AESFastEngine;
import libtruezip.lcrypto.crypto.generators.PKCS12ParametersGenerator;
import libtruezip.lcrypto.crypto.macs.HMac;
import libtruezip.lcrypto.crypto.params.KeyParameter;
import libtruezip.lcrypto.crypto.params.ParametersWithIV;

/**
 * Reads a type 0 RAES file.
 *
 * @author  Christian Schlichtherle
 */
final class Type0RaesReadOnlyFile extends RaesReadOnlyFile {

    /** The key strength. */
    private final KeyStrength keyStrength;

    /**
     * The key parameter required to init the SHA-256 Message Authentication
     * Code (HMAC).
     */
    private final KeyParameter sha256MacParam;

    private final Type0RaesParameters type0Params;

    /**
     * The footer of the data envelope containing the authentication codes.
     */
    private final byte[] footer;

    Type0RaesReadOnlyFile(
            final ReadOnlyFile rof,
            final Type0RaesParameters param)
    throws IOException {
        super(rof);

        assert null != param;
        type0Params = param;

        // Init read only file.
        rof.seek(0);
        final long fileLength = rof.length();

        // Load header data.
        final byte[] header = new byte[ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT];
        rof.readFully(header);

        // Check key size and iteration count
        final int keyStrengthOrdinal = readUByte(header, 5);
        final KeyStrength keyStrength;
        try {
            keyStrength = KeyStrength.values()[keyStrengthOrdinal];
            assert keyStrength.ordinal() == keyStrengthOrdinal;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new RaesException(
                    "Unknown index for cipher key strength: "
                    + keyStrengthOrdinal);
        }
        final int keyStrengthBytes = keyStrength.getBytes();
        final int keyStrengthBits = keyStrength.getBits();
        this.keyStrength = keyStrength;

        final int iCount = readUShort(header, 6);
        if (1024 > iCount)
            throw new RaesException(
                    "Iteration count must be 1024 or greater, but is "
                    + iCount
                    + "!");

        // Load salt.
        final byte[] salt = new byte[keyStrengthBytes];
        rof.readFully(salt);

        // Init KLAC and footer.
        final Mac klac = new HMac(new SHA256Digest());
        this.footer = new byte[klac.getMacSize()];

        // Init start, end and length of encrypted data.
        final long start = header.length + salt.length;
        final long end = fileLength - this.footer.length;
        final long length = end - start;
        if (length < 0) {
            // Wrap an EOFException so that a caller can identify this issue.
            throw new RaesException("False positive Type 0 RAES file is too short!",
                    new EOFException());
        }

        // Load footer data.
        rof.seek(end);
        rof.readFully(this.footer);
        if (-1 != rof.read()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new RaesException(
                    "Expected end of file after data envelope trailer!");
        }

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator
                gen = new PKCS12ParametersGenerator(new SHA256Digest());
        ParametersWithIV aesCtrParam;
        KeyParameter sha256MacParam;
        byte[] buf;

        final char[] passwd = param.getReadPassword(false);
        assert null != passwd;
        final byte[] pass = PKCS12PasswordToBytes(passwd);
        for (int i = passwd.length; --i >= 0; ) // nullify password parameter
            passwd[i] = 0;

        gen.init(pass, salt, iCount);
        aesCtrParam = (ParametersWithIV) gen.generateDerivedParameters(
                keyStrengthBits, AES_BLOCK_SIZE_BITS);
        sha256MacParam = (KeyParameter) gen.generateDerivedMacParameters(
                keyStrengthBits);
        paranoidWipe(pass);

        // Compute and verify KLAC.
        klac.init(sha256MacParam);
       
        // Update the KLAC with the cipher key.
        // This is actually redundant, but it's part of the spec, so it
        // cannot get changed anymore.
        final byte[] cipherKey = ((KeyParameter) aesCtrParam.getParameters()).getKey();
        klac.update(cipherKey, 0, cipherKey.length);
        buf = new byte[klac.getMacSize()];
        RaesOutputStream.klac(klac, length, buf);

        if (!ArrayHelper.equals(this.footer, 0, buf, 0, buf.length / 2)) {
            // Invalid password or corrupted file. The former is the one notified to
            //the user. Suspend validation for a milliseconds to avoid brute force
            param.invalidate();
            SuspensionPenalty.apply();
            throw new RaesAuthenticationException();
        }

        // Init parameters for authenticate().
        this.sha256MacParam = sha256MacParam;

        // Init cipher.
        final SeekableBlockCipher
                cipher = new SICSeekableBlockCipher(new AESFastEngine());
        cipher.init(false, aesCtrParam);
        init(cipher, start, length);

        // Commit key strength to parameters.
        param.setKeyStrength(keyStrength);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] pwd) {
        for (int i = pwd.length; --i >= 0; )
            pwd[i] = 0;
    }

    @Override
    public KeyStrength getKeyStrength() {
        return keyStrength;
    }

    @Override
    public void authenticate() throws IOException {
        final Mac mac = new HMac(new SHA256Digest());
        mac.init(sha256MacParam);
        final byte[] buf = computeMac(mac);
        assert buf.length == mac.getMacSize();
        if (!ArrayHelper.equals(buf, 0, footer, footer.length / 2, footer.length / 2)) {
            // Invalid password or corrupted file. The former is the one notified to
            //the user. Suspend validation for a milliseconds to avoid brute force
            type0Params.invalidate();
            SuspensionPenalty.apply();
            throw new RaesAuthenticationException();
        }
    }
}