/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.sl;

import de.schlichtherle.truezip.key.AbstractKeyManagerProvider;
import de.schlichtherle.truezip.key.KeyManager;
import java.util.*;

/**
 * Empty implementation of key managers.
 *
 * @author  Christian Schlichtherle
 */
public final class KeyManagerLocator extends AbstractKeyManagerProvider {

    /** The singleton instance of this class. */
    public static final KeyManagerLocator SINGLETON = new KeyManagerLocator();

    /** You cannot instantiate this class. */
    private KeyManagerLocator() {
    }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return Boot.MANAGERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<Class<?>, KeyManager<?>> MANAGERS;
        static {
            final Map<Class<?>, KeyManager<?>> fast = new LinkedHashMap<Class<?>, KeyManager<?>>();
            MANAGERS = Collections.unmodifiableMap(fast);
        }
    } // class Boot
}