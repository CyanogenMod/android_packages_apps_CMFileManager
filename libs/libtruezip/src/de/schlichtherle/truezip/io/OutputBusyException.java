/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

/**
 * Indicates that a file system entry could not get written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see    InputBusyException
 * @author Christian Schlichtherle
 */
public class OutputBusyException extends FileBusyException {
    private static final long serialVersionUID = 962318648273654198L;
   
    public OutputBusyException(String message) {
        super(message);
    }

    public OutputBusyException(Throwable cause) {
        super(cause);
    }
}
