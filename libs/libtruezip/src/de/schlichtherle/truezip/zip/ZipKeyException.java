/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt or
 * authenticate a ZIP entry has failed for some reason.
 *
 * @author Christian Schlichtherle
 */
public class ZipKeyException extends ZipParametersException {
    private static final long serialVersionUID = 5762312735142938698L;
   
    /**
     * Creates a ZIP key exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipKeyException(String msg) {
        super(msg);
    }

    /**
     * Creates a ZIP key exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipKeyException(Throwable cause) {
        super(cause);
    }
}