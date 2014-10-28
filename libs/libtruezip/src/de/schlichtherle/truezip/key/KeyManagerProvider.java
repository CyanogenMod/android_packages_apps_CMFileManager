/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.util.ServiceConfigurationError;

/**
 * A service for key managers for secret key classes.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface KeyManagerProvider {

    /**
     * Returns the key manager for the given secret key class.
     * Subsequent calls must return the same key manager for the same secret
     * key class.
     *
     * @param  <K> the type of the secret keys.
     * @param  type the class for the secret key type.
     * @return the key manager for the given secret key class.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    <K> KeyManager<K> get(Class<K> type);
}
