/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.charset;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A charset provider that only provides the {@code IBM437} character set,
 * also known as {@code CP437}.
 *
 * @author  Christian Schlichtherle
 */
public final class ZipCharsetProvider extends CharsetProvider {

    public static final CharsetProvider SINGLETON = new ZipCharsetProvider();

    private static final Map<String, Charset> CHARSETS;
    static {
        Logger logger = Logger.getLogger(ZipCharsetProvider.class.getName());
        Map<String, Charset> charsets = new HashMap<String, Charset>();
        for (Charset charset : new Charset[] {
            new Ibm437Charset(),
        }) {
            charsets.put(lowerCase(charset.name()), charset);
            for (String alias : charset.aliases())
                charsets.put(lowerCase(alias), charset);
            logger.log(Level.CONFIG, "Providing charset {0} with aliases {1}.",
                    new Object[] { charset.displayName(), charset.aliases() });
        }
        CHARSETS = Collections.unmodifiableMap(charsets);
    }

    private static String lowerCase(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    @Override
    public Iterator<Charset> charsets() {
        return CHARSETS.values().iterator();
    }

    @Override
    public Charset charsetForName(String charset) {
        return CHARSETS.get(lowerCase(charset));
    }
}