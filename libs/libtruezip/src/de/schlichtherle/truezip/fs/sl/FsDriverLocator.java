/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import java.util.*;

/**
 * Empty implementation of system drivers
 *
 * @author Christian Schlichtherle
 */
public final class FsDriverLocator implements FsDriverProvider {

    /** The singleton instance of this class. */
    public static final FsDriverLocator SINGLETON = new FsDriverLocator();

    /** You cannot instantiate this class. */
    private FsDriverLocator() {
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return Boot.DRIVERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<FsScheme, FsDriver> DRIVERS;
        static {
            final Map<FsScheme, FsDriver> fast = new LinkedHashMap<FsScheme, FsDriver>();
            DRIVERS = Collections.unmodifiableMap(fast);
        }
    } // Boot
}
