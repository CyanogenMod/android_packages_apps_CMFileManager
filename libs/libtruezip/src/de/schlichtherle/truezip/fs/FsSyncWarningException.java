/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * An exception of this class implies that no or only insignificant parts
 * of the data of the federated file system has been lost.
 *
 * @author Christian Schlichtherle
 */
public class FsSyncWarningException extends FsSyncException {

    private static final long serialVersionUID = 2302357394858347366L;

    public FsSyncWarningException(FsModel model, IOException cause) {
        super(model, cause, -1);
    }
}
