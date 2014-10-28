/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * An abstract key manager provider.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class AbstractKeyManagerProvider implements KeyManagerProvider {

    @Override
    @SuppressWarnings("unchecked")
    public final <K> KeyManager<K> get(final Class<K> type) {
        final KeyManager<?> manager = get().get(type);
        if (null == manager)
            throw new ServiceConfigurationError("No key manager available for " + type);
        return (KeyManager<K>) manager;
    }

    /**
     * Returns an unmodifiable map of secret key classes to key managers.
     * Neither the keys nor the values of the returned map may be {@code null}
     * and subsequent calls must return a map which compares at least
     * {@link Map#equals(Object) equal} with the previously returned map.
     *
     * @return an unmodifiable map of secret key classes to key managers.
     * @since  TrueZIP 7.2
     */
    public abstract Map<Class<?>, KeyManager<?>> get();
}
