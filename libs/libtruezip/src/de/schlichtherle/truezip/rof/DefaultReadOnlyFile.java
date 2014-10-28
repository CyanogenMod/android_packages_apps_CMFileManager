/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/**
 * A {@link ReadOnlyFile} implementation derived from {@link RandomAccessFile}.
 *
 * @author  Christian Schlichtherle
 */
public class DefaultReadOnlyFile
extends RandomAccessFile
implements ReadOnlyFile {

    public DefaultReadOnlyFile(File file) throws FileNotFoundException {
        super(file, "r");
    }
}