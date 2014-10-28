/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import static java.nio.charset.CoderResult.OVERFLOW;
import static java.nio.charset.CoderResult.UNDERFLOW;

/**
 * Decodes quoted characters in URI components according to
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
 * and its updates in
 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
 * for IPv6 addresses.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *      RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *      RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @see UriEncoder
 * @author Christian Schlichtherle
 */
public final class UriDecoder {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final CharsetDecoder decoder;

    private StringBuilder stringBuilder;

    /**
     * Constructs a new URI decoder which uses the UTF-8 character set to
     * decode non-US-ASCII characters.
     */
    public UriDecoder() {
        this(null);
    }

    /**
     * Constructs a new URI decoder which uses the given character set to
     * decode non-US-ASCII characters.
     *
     * @param charset the character set to use for encoding non-US-ASCII
     *        characters.
     *        If this parameter is {@code null}, then it defaults to
     *        {@code UTF-8}.
     *        Note that providing any other value than {@code null} or
     *        {@code UTF-8} will void interoperability with most applications.
     */
    public UriDecoder(Charset charset) {
        if (null == charset)
            charset = UTF8;
        this.decoder = charset.newDecoder();
    }

    private static int dequote(final CharBuffer eC) {
        if (eC.hasRemaining()) {
            final char ec0 = eC.get();
            if (eC.hasRemaining()) {
                final char ec1 = eC.get();
                return (dequote(ec0) << 4) | dequote(ec1);
            }
        }
        return -1;
    }

    private static int dequote(char ec) {
	if ('0' <= ec && ec <= '9')
	    return ec - '0';
        ec &= ~(2 << 4); // toUpperCase for 'a' to 'z'
	if ('A' <= ec && ec <= 'F')
	    return ec - 'A' + 10;
	return -1;
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded using the character set provided to the constructor.
     *
     * @param  eS the encoded string to decode.
     * @return The decoded string.
     * @throws IllegalArgumentException on any decoding error with a
     *         {@link URISyntaxException} as its
     *         {@link IllegalArgumentException#getCause() cause}.
     */
    public String decode(String eS) {
        try {
            StringBuilder dS = decode(eS, null);
            return null != dS ? dS.toString() : eS;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded to the string builder {@code dS} using the character
     * set provided to the constructor.
     *
     * @param  eS the encoded string to decode.
     * @param  dS the string builder to which all decoded characters shall get
     *         appended.
     * @return If {@code eS} contains no escape sequences, then {@code null}
     *         gets returned.
     *         Otherwise, if {@code dS} is not {@code null}, then it gets
     *         returned with all decoded characters appended to it.
     *         Otherwise, a temporary string builder gets returned which solely
     *         contains all decoded characters.
     *         This temporary string builder may get cleared and reused upon
     *         the next call to <em>any</em> method of this object.
     * @throws URISyntaxException on any decoding error.
     *         This exception will leave {@code eS} in an undefined state.
     */
    public StringBuilder decode(
            final String eS,
            StringBuilder dS)
    throws URISyntaxException {
        final CharBuffer eC = CharBuffer.wrap(eS);  // encoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharBuffer dC = null;                       // decoded characters
        CharsetDecoder dec = null;
        while (true) {
            eC.mark();
            final int ec = eC.hasRemaining() ? eC.get() : -1; // char is unsigned!
            if ('%' == ec) {
                if (null == eB) {
                    if (null == dS) {
                        if (null == (dS = stringBuilder))
                            dS = stringBuilder = new StringBuilder();
                        else
                            dS.setLength(0);
                        dS.append(eS, 0, eC.position() - 1); // prefix until current character
                    }
                    int l = eC.remaining();
                    l = (l + 1) / 3;
                    eB = ByteBuffer.allocate(l);
                    dC = CharBuffer.allocate(l);
                    dec = decoder;
                }
                final int eb = dequote(eC);         // encoded byte
                if (eb < 0)
                    throw new URISyntaxException(
                            eS,
                            "illegal escape sequence",
                            eC.reset().position());
                eB.put((byte) eb);
            } else {
                if (null != eB && 0 < eB.position()) {
                    eB.flip();
                    { // Decode eB -> dC.
                        CoderResult cr;
                        if (UNDERFLOW != (cr = dec.reset().decode(eB, dC, true))
                                || UNDERFLOW != (cr = dec.flush(dC))) {
                            assert OVERFLOW != cr;
                            throw new QuotedUriSyntaxException(eS, cr.toString());
                        }
                    }
                    eB.clear();
                    dC.flip();
                    dS.append(dC);
                    dC.clear();
                }
                if (0 > ec)
                    break;
                if (null != dS)
                    dS.append((char) ec);
            }
        }
        return null == eB ? null : dS;
    }
}