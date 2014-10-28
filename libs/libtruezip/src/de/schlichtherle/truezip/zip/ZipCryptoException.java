/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.ZipException;

/**
 * Thrown if there is an issue when reading or writing an encrypted ZIP file
 * or entry.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public class ZipCryptoException extends ZipException {
    private static final long serialVersionUID = 2783693745683625471L;

    /**
     * Constructs a ZIP crypto exception with
     * no detail message.
     */
    public ZipCryptoException() {
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipCryptoException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public ZipCryptoException(String msg, Throwable cause) {
        super(msg);
        super.initCause(cause);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipCryptoException(Throwable cause) {
        super(null != cause ? cause.toString() : null);
        super.initCause(cause);
    }
}