/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.key.AuthenticationFailedOperation;

/**
 * Thrown to indicate that a RAES file has been tampered with.
 *
 * @author  Christian Schlichtherle
 */
public class RaesAuthenticationException extends RaesException
        implements AuthenticationFailedOperation {
    private static final long serialVersionUID = 8336517092033362293L;

    /**
     * Constructs a RAES exception with
     * a detail message indicating that a RAES file has been tampered with.
     */
    public RaesAuthenticationException() {
        super("Authenticated file content has been tampered with!");
    }
}