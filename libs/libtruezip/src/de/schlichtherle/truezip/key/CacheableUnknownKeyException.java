/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed and that this exception is cacheable.
 * The subclass provides more information.
 *
 * @author  Christian Schlichtherle
 */
public class CacheableUnknownKeyException extends UnknownKeyException {
    private static final long serialVersionUID = 2463586348235337265L;

    CacheableUnknownKeyException() {
    }

    CacheableUnknownKeyException(String message) {
        super(message);
    }
}