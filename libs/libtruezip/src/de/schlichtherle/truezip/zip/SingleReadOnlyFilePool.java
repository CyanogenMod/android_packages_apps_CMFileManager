/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * A pool with a single read only file provided to its constructor.
 *
 * @author  Christian Schlichtherle
 */
final class SingleReadOnlyFilePool implements Pool<ReadOnlyFile, IOException> {

    final ReadOnlyFile rof;

    SingleReadOnlyFilePool(final ReadOnlyFile rof) { this.rof = rof; }

    @Override public ReadOnlyFile allocate() { return rof; }

    @Override public void release(ReadOnlyFile rof) { assert this.rof == rof; }
}