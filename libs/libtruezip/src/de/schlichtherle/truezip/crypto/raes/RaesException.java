/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import java.io.IOException;

/**
 * Thrown if there is an issue when reading or writing a RAES file which is
 * specific to the RAES file format.
 *
 * @author  Christian Schlichtherle
 */
public class RaesException extends IOException {
    private static final long serialVersionUID = 8564203786508562247L;

    /**
     * Constructs a RAES exception with
     * no detail message.
     */
    public RaesException() {
    }

    /**
     * Constructs a RAES exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public RaesException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a RAES exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesException(Throwable cause) {
        super(cause);
    }
}