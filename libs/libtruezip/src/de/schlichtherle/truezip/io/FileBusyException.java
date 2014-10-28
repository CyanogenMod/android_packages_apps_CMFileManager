/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.FileNotFoundException;

/**
 * Indicates that a file system entry could not get read or written
 * because the entry or its container is busy.
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as the resource is not busy anymore
 * and no other exceptional conditions apply.
 *
 * @author Christian Schlichtherle
 */
public class FileBusyException extends FileNotFoundException {
    private static final long serialVersionUID = 2056108562576389242L;

    public FileBusyException(String message) {
        super(message);
    }

    public FileBusyException(Throwable cause) {
        super(null == cause ? null : cause.toString());
        super.initCause(cause);
    }
}
