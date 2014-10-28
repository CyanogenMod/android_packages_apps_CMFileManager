/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.net.URI;

/**
 * A container for key providers for reading and writing protected resources.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the secret keys.
 * @author  Christian Schlichtherle
 */
public interface KeyManager<K> {

    /**
     * Returns the mapped key provider for the given protected resource.
     * If no key provider is mapped yet, a new key provider is created and
     * returned.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider mapped for the protected resource.
     */
    KeyProvider<K> getKeyProvider(URI resource);

    /**
     * Moves the mapped key provider from the URI {@code oldResource} to
     * {@code newResource}.
     *
     * @param  oldResource the old URI of the protected resource.
     * @param  newResource the new URI of the protected resource.
     * @return The key provider which was previously mapped for the protected
     *         resource {@code newResource}.
     * @throws IllegalArgumentException if {@code oldResource} compares
     *         {@link URI#equals(Object) equal} to {@code newResource}.
     */
    KeyProvider<K> moveKeyProvider(URI oldResource, URI newResource);

    /**
     * Removes the mapped key provider for the given protected resource.
     * It is an error to use the returned key provider.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider which was previously mapped for the protected
     *         resource.
     */
    KeyProvider<K> removeKeyProvider(URI resource);

    /**
     * Returns a priority to help the key manager service locator.
     * The greater number wins!
     * The default value should be {@code 0}.
     *
     * @return A priority to help the key manager service locator.
     */
    public int getPriority();
}