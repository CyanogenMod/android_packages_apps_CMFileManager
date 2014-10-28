/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;

/**
 * A file system driver for the FILE scheme.
 *
 * @author Christian Schlichtherle
 */
public final class FileDriver extends FsDriver {

    @Override
    public FsController<?> newController(
            final FsModel model,
            final FsController<?> parent) {
        assert null == parent;
        return new FileController(model);
    }
}
