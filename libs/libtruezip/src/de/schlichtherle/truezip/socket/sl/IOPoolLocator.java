/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket.sl;

import de.schlichtherle.truezip.fs.file.TempFilePoolService;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.IOPoolService;

/**
 * Locates an I/O buffer pool service.
 *
 * @see     IOPoolService
 * @author  Christian Schlichtherle
 */
public final class IOPoolLocator implements IOPoolProvider {

    /** The singleton instance of this class. */
    public static final IOPoolLocator SINGLETON = new IOPoolLocator();

    /** Can't touch this - hammer time! */
    private IOPoolLocator() { }

    @Override
    public IOPool<?> get() {
        return Boot.pool;
    }

    /** A static data utility class used for lazy initialization. */
    @SuppressWarnings({"rawtypes"})
    private static final class Boot {
        static final IOPool<?> pool;
        static {
            pool = (IOPool) create();
        }

        private static IOPool<?> create() {
            IOPoolService service = new TempFilePoolService();
            final IOPool<?> pool = service.get();
            return pool;
        }
    } // Boot
}
