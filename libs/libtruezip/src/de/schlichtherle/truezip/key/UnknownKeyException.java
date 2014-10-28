/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.security.GeneralSecurityException;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed.
 * The subclass provides more information.
 *
 * @author Christian Schlichtherle
 */
public class UnknownKeyException extends GeneralSecurityException {
    private static final long serialVersionUID = 6092786348232837265L;

    UnknownKeyException() {
    }

    protected UnknownKeyException(String msg) {
        super(msg);
    }

    public UnknownKeyException(Throwable cause) {
        super(cause);
    }
}