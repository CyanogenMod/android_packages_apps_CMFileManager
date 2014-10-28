/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto;

import libtruezip.lcrypto.crypto.BlockCipher;
import libtruezip.lcrypto.crypto.CipherParameters;
import libtruezip.lcrypto.crypto.DataLengthException;
import libtruezip.lcrypto.crypto.modes.SICBlockCipher;
import libtruezip.lcrypto.crypto.params.ParametersWithIV;

/**
 * Implements Counter (CTR) mode (alias Segmented Integer Counter - SIC)
 * on top of a simple block cipher.
 * This code is based on bouncy castle's {@link SICBlockCipher} class,
 * but allows random access to a block, too.
 * Like the {@link SICBlockCipher} class, the block counter is incremented
 * <em>after</em> updating the cipher input in <em>big endian</em> order.
 *
 * @author  The Legion of the Bouncy Castle (majority of the code)
 * @author  Christian Schlichtherle (optimizations and extension to support seeking)
 */
public class SICSeekableBlockCipher implements SeekableBlockCipher {

    protected final BlockCipher cipher;
    protected final int blockSize;
    protected long blockCounter;
    protected final byte[] IV;
    protected final byte[] cipherIn;
    protected final byte[] cipherOut;

    /**
     * Constructs a new big endian SIC seekable block cipher mode.
     *
     * @param cipher The underlying block cipher to use.
     */
    public SICSeekableBlockCipher(final BlockCipher cipher) {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();
        this.IV = new byte[blockSize];
        this.cipherIn = new byte[blockSize];
        this.cipherOut = new byte[blockSize];
    }

    /**
     * Returns the underlying block cipher which we are decorating.
     *
     * @return The underlying block cipher which we are decorating.
     */
    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    @Override
    public void init(
            boolean forEncryption, // not used for CTR mode
            CipherParameters params) {
        ParametersWithIV ivParams = (ParametersWithIV) params;
        byte[] iv = ivParams.getIV();
        System.arraycopy(iv, 0, IV, 0, IV.length);
        reset();
        this.cipher.init(true, ivParams.getParameters());
    }

    @Override
    public String getAlgorithmName() {
        // Must add "/SIC" in order to make decorating BufferedBlockCipher work
        // correctly.
        return this.cipher.getAlgorithmName() + "/SIC";
    }

    @Override
    public int getBlockSize() {
        assert this.blockSize == this.cipher.getBlockSize();
        return this.blockSize;
    }

    @Override
    public int processBlock(
            final byte[] in,
            int inOff,
            final byte[] out,
            int outOff)
    throws DataLengthException, IllegalStateException {
        incCounter();
        this.cipher.processBlock(this.cipherIn, 0, this.cipherOut, 0);

        // XOR the cipherOut with the plaintext producing the cipher text.
        final int blockSize = this.blockSize;
        {
            int i = blockSize;
            inOff += i;
            outOff += i;
            while (i > 0)
                out[--outOff] = (byte) (in[--inOff] ^ this.cipherOut[--i]);
        }

        return blockSize;
    }

    private void incCounter() {
        final int blockSize = this.blockSize;
        long blockCounter = this.blockCounter++; // post-increment the block counter!
        for (int i = blockSize; --i >= 0; ) { // big endian order!
            blockCounter += IV[i] & 0xff;
            this.cipherIn[i] = (byte) blockCounter;
            blockCounter >>>= 8;
        }
    }

    @Override
    public void setBlockCounter(final long blockCounter) {
        this.blockCounter = blockCounter;
    }

    @Override
    public long getBlockCounter() {
        return this.blockCounter;
    }

    @Override
    public void reset() {
        this.cipher.reset();
        this.blockCounter = 0;
    }
}