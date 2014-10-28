/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thrown if an error happened on the input side rather than the output side
 * when copying an {@link InputStream} to an {@link OutputStream}.
 *
 * @see     Streams#cat(InputStream, OutputStream)
 * @author  Christian Schlichtherle
 */
public class InputException extends IOException {
    private static final long serialVersionUID = 1287654325546872424L;

    /**
     * Constructs a new {@code InputException}.
     *
     * @param cause the nullable cause for this exception.
     */
    public InputException(Throwable cause) {
        super(cause);
    }
}