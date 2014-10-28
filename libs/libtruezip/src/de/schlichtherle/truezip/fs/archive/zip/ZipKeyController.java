/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.zip.ZipKeyException;
import java.io.IOException;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key(s) required for accessing its
 * WinZip AES encrypted ZIP file.
 *
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
final class ZipKeyController<M extends FsModel>
extends KeyController<M, ZipDriver> {

    ZipKeyController(FsController<? extends M> controller, ZipDriver driver) {
        super(controller, driver);
    }

    @Override
    protected Class<?> getKeyType() {
        return AesPbeParameters.class;
    }

    @Override
    protected Class<? extends IOException> getKeyExceptionType() {
        return ZipKeyException.class;
    }
}
