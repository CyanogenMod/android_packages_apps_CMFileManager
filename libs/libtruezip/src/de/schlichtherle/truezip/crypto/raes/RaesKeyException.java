/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt some pay load
 * data in an RAES file has failed for some reason.
 *
 * @author  Christian Schlichtherle
 */
public class RaesKeyException extends RaesParametersException {
    private static final long serialVersionUID = 1375629384612351398L;
   
    /**
     * Constructs a RAES key exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesKeyException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES key exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesKeyException(Throwable cause) {
        super(cause);
    }
}