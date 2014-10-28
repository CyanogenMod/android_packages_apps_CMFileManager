/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses a map to hold the safe key providers managed by this instance.
 *
 * @author  Christian Schlichtherle
 */
public abstract class SafeKeyManager<K extends SafeKey<K>, P extends SafeKeyProvider<K>>
implements KeyManager<K> {

    private final Map<URI, P> providers = new HashMap<URI, P>();

    /**
     * Constructs a new safe key manager.
     *
     * @since TrueZIP 7.2
     */
    protected SafeKeyManager() {
    }

    /**
     * Returns a new key provider.
     *
     * @return A new key provider.
     * @since  TrueZIP 7.2
     */
    protected abstract P newKeyProvider();

    @Override
    public synchronized P getKeyProvider(final URI resource) {
        if (null == resource)
            throw new NullPointerException();
        P provider = providers.get(resource);
        if (null == provider) {
            provider = newKeyProvider();
            providers.put(resource, provider);
        }
        return provider;
    }

    /**
     * Returns the key provider which is mapped for the given {@code resource}
     * or {@code null} if no key provider is mapped.
     * <p>
     * TODO: Make this part of the interface {@link KeyManager} in the next
     * major version.
     *
     * @param  resource the nullable URI of the protected resource.
     * @return The key provider mapped for the protected resource.
     */
    public synchronized P getMappedKeyProvider(URI resource) {
        if (null == resource)
            throw new NullPointerException();
        return providers.get(resource);
    }

    @Override
    public synchronized P moveKeyProvider(final URI oldResource, final URI newResource) {
        if (null == newResource)
            throw new NullPointerException();
        if (oldResource.equals(newResource))
            throw new IllegalArgumentException();
        final P provider = providers.remove(oldResource);
        if (null != provider)
            return providers.put(newResource, provider);
        else
            return providers.remove(newResource);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned key provider is invalidated and will behave as if prompting
     * for the secret key had been disabled or cancelled by the user.
     */
    @Override
    public synchronized P removeKeyProvider(final URI resource) {
        if (null == resource)
            throw new NullPointerException();
        final P provider = providers.remove(resource);
        if (null != provider)
            provider.setKey(null);
        return provider;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}