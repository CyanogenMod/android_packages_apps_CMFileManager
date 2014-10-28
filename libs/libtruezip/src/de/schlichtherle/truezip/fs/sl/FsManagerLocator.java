/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsManagerProvider;
import de.schlichtherle.truezip.fs.spi.FsManagerService;

/**
 * Locates a file system manager service.
 *
 * @see    FsDefaultManager
 * @see    FsManagerService
 * @author Christian Schlichtherle
 */
public final class FsManagerLocator implements FsManagerProvider {

    /** The singleton instance of this class. */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    /** Can't touch this - hammer time! */
    private FsManagerLocator() { }

    @Override
    public FsManager get() {
        return Boot.manager;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final FsManager manager;
        static {
            manager = create();
        }

        private static FsManager create() {
            FsManagerService service = new DefaultManagerService();
            return service.get();
        }
    } // Boot

    private static final class DefaultManagerService extends FsManagerService {
        @Override
        public FsManager get() {
            return new FsDefaultManager();
        }
    } // DefaultManagerService
}
