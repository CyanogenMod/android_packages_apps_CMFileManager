/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

/**
 * A memory efficient base class for simple 8 bit (octet) character sets.
 *
 * @author Christian Schlichtherle
 */
public abstract class OctetCharset extends Charset {

    /**
     * Use this character in the lookup table provided to the constructor for
     * every character that does not have a replacement in 16 bit Unicode.
     */
    protected static final char REPLACEMENT = 0xFFFD;

    private final char[] byte2char;
    private final char[][] char2byte;

    protected OctetCharset( final String cname,
                            String[] aliases,
                            char[] byte2char) {
        super(cname, aliases = aliases.clone());
        byte2char = byte2char.clone();

        // Construct sparse inverse lookup table.
        final char[][] char2byte = new char[256][];
        for (char i = 0; i < 256; i++) {
            final char c = byte2char[i];
            if (c == REPLACEMENT)
                continue;

            final int hi = c >>> 8;
            final int lo = c & 0xFF;
            char[] table = char2byte[hi];
            if (table == null) {
                table = new char[256];
                Arrays.fill(table, REPLACEMENT);
                char2byte[hi] = table;
            }
            table[lo] = i;
        }

        this.byte2char = byte2char;
        this.char2byte = char2byte;
    }

    @Override
    public boolean contains(Charset cs) {
        return this.getClass().isInstance(cs);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder();
    }

    private class Encoder extends CharsetEncoder {
        Encoder() {
            super(OctetCharset.this, 1, 1);
        }

        @Override
        protected CoderResult encodeLoop(   final CharBuffer in,
                                            final ByteBuffer out) {
            final char[][] c2b = char2byte;
            while (in.hasRemaining()) {
                if (!out.hasRemaining())
                    return CoderResult.OVERFLOW;
                final char c = in.get();
                final int hi = c >>> 8;
                final int lo = c & 0xFF;
                final char[] table = c2b[hi];
                final char b;
                if (table == null || (b = table[lo]) == REPLACEMENT) { // char is unsigned!
                    in.position(in.position() - 1); // push back
                    return CoderResult.unmappableForLength(1);
                }
                out.put((byte) b); // char is unsigned!
            }
            return CoderResult.UNDERFLOW;
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new Decoder();
    }

    private class Decoder extends CharsetDecoder {
        Decoder() {
            super(OctetCharset.this, 1, 1);
        }

        @Override
        protected CoderResult decodeLoop(   final ByteBuffer in,
                                            final CharBuffer out) {
            final char[] b2c = byte2char;
            while (in.hasRemaining()) {
                if (!out.hasRemaining())
                    return CoderResult.OVERFLOW;
                final char c = b2c[in.get() & 0xFF];
                if (c == REPLACEMENT) {
                    in.position(in.position() - 1); // push back
                    return CoderResult.unmappableForLength(1);
                }
                out.put(c);
            }
            return CoderResult.UNDERFLOW;
        }
    }
}