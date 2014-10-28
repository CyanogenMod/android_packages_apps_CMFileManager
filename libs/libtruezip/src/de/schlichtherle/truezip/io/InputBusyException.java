/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

/**
 * Indicates that a file system entry could not get read
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see    OutputBusyException
 * @author Christian Schlichtherle
 */
public class InputBusyException extends FileBusyException {
    private static final long serialVersionUID = 1983745618753823654L;

    public InputBusyException(String message) {
        super(message);
    }

    public InputBusyException(Throwable cause) {
        super(cause);
    }
}
