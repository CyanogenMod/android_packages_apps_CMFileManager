/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Thrown to indicate that an authenticated ZIP entry has been tampered with.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public class ZipAuthenticationException extends ZipCryptoException {
    private static final long serialVersionUID = 2403462923846291232L;

    /**
     * Constructs a ZIP authentication exception with the given detail message.
     */
    public ZipAuthenticationException(String msg) {
        super(msg);
    }
}