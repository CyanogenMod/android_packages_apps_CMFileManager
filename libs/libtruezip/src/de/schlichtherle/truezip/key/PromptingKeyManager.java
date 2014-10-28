/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import java.net.URI;

/**
 * A key manager which prompts the user for a key if required.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyProvider
 * @author  Christian Schlichtherle
 */
public final class PromptingKeyManager<K extends SafeKey<K>>
extends SafeKeyManager<K, PromptingKeyProvider<K>> {

    private final View<K> view;

    /**
     * Constructs a new prompting key manager.
     *
     * @param view the view instance for prompting for keys.
     */
    public PromptingKeyManager(final View<K> view) {
        if (null == view)
            throw new NullPointerException();
        this.view = view;
    }

    final View<K> getView() {
        return view;
    }

    /**
     * Returns a new prompting key provider.
     *
     * @return A new prompting key provider.
     * @since  TrueZIP 7.2
     */
    @Override
    protected PromptingKeyProvider<K> newKeyProvider() {
        return new PromptingKeyProvider<K>(this);
    }

    @Override
    public synchronized PromptingKeyProvider<K> getKeyProvider(URI resource) {
        final PromptingKeyProvider<K> provider = super.getKeyProvider(resource);
        provider.setResource(resource);
        return provider;
    }

    @Override
    public synchronized PromptingKeyProvider<K> getMappedKeyProvider(URI resource) {
        final PromptingKeyProvider<K> provider = super.getMappedKeyProvider(resource);
        if (null != provider)
            provider.setResource(resource);
        return provider;
    }

    @Override
    public synchronized PromptingKeyProvider<K> moveKeyProvider(URI oldResource, URI newResource) {
        final PromptingKeyProvider<K>
                oldProvider = super.moveKeyProvider(oldResource, newResource);
        if (null != oldProvider)
            oldProvider.setResource(null);
        final PromptingKeyProvider<K>
                newProvider = super.getMappedKeyProvider(newResource);
        if (null != newProvider)
            newProvider.setResource(newResource);
        return oldProvider;
    }

    @Override
    public synchronized PromptingKeyProvider<K> removeKeyProvider(URI resource) {
        final PromptingKeyProvider<K>
                provider = super.removeKeyProvider(resource);
        if (null != provider)
            provider.setResource(null);
        return provider;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[view=%s, priority=%d]",
                getClass().getName(),
                getView(),
                getPriority());
    }
}