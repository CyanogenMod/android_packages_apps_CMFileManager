/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import java.io.Closeable;
import java.net.URI;

/**
 * A "safe" key provider which prompts the user for a key for its protected
 * resource.
 * The user is prompted via an instance of the {@link View} interface.
 * The view may then display the resource URI by calling {@link #getResource}
 * on this instance and set the key by using the given {@link Controller}.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyManager
 * @author  Christian Schlichtherle
 */
public final class PromptingKeyProvider<K extends SafeKey<K>>
extends SafeKeyProvider<K> {

    private final View<K> view;

    private volatile State state = State.RESET;

    /** The resource identifier for the protected resource. */
    private volatile URI resource;

    private volatile boolean changeRequested;

    PromptingKeyProvider(PromptingKeyManager<K> manager) {
        this.view = manager.getView();
    }

    private View<K> getView() {
        return view;
    }

    private State getState() {
        return state;
    }

    private void setState(final State state) {
        assert null != state;
        this.state = state;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    public URI getResource() {
        return resource;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    void setResource(final URI resource) {
        this.resource = resource;
    }

    @Override
    protected void retrieveWriteKey() throws UnknownKeyException {
        getState().retrieveWriteKey(this);
    }

    @Override
    protected void retrieveReadKey(boolean invalid)
    throws UnknownKeyException {
        getState().retrieveReadKey(this, invalid);
    }

    @Override
    protected K getKey() {
        return getState().getKey(this);
    }

    private K getKey0() {
        return super.getKey();
    }

    @Override
    public void setKey(final K key) {
        getState().setKey(this, key);
    }

    private void setKey0(final K key) {
        super.setKey(key);
    }

    /**
     * Returns whether or not the user shall get prompted for a new key upon
     * the next call to {@link #getWriteKey()}, provided that the key
     * has been {@link #setKey set} before.
     *
     * @return Whether or not the user shall get prompted for a new key upon
     *         the next call to {@link #getWriteKey()}, provided that the key
     *         has been {@link #setKey set} before.
     */
    private boolean isChangeRequested() {
        return changeRequested;
    }

    private void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    /**
     * Resets the state of this key provider, its current key and the value of
     * its {@code changeRequested} property
     * if and only if prompting for a key has been cancelled.
     */
    public void resetCancelledKey() {
        getState().resetCancelledKey(this);
    }

    /**
     * Resets the state of this key provider, its current key and the value of
     * its {@code changeRequested} property
     * unconditionally.
     */
    public void resetUnconditionally() {
        reset();
    }

    private void reset() {
        setKey0(null);
        setChangeRequested(false);
        setState(State.RESET);
    }

    /** Implements the behavior strategy of its enclosing class. */
    private enum State {
        RESET {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                State state;
                try {
                    PromptingKeyProvider<K>.BaseController controller
                            = provider.new WriteController(this);
                    try {
                        provider.getView().promptWriteKey(controller);
                    } finally {
                        controller.close();
                    }
                } finally {
                    if (this == (state = provider.getState()))
                        provider.setState(state = CANCELLED);
                }
                state.retrieveWriteKey(provider);
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                State state;
                do {
                    try {
                        PromptingKeyProvider<K>.BaseController controller
                                = provider.new ReadController(this);
                        try {
                            provider.getView().promptReadKey(controller, invalid);
                        } finally {
                            controller.close();
                        }
                    } catch (CacheableUnknownKeyException ex) {
                        setException(provider, ex);
                    }
                    state = provider.getState();
                } while (state == this);
                state.retrieveReadKey(provider, false);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        SET {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                if (provider.isChangeRequested()) {
                    provider.setChangeRequested(false);
                    RESET.retrieveWriteKey(provider); // DON'T change state!
                } else {
                    // FIXME -- CMFM -- Always ask for password. Caching is the view side.
                    // This allow to reset the password (a bad way)
                    PromptingKeyProvider<K>.BaseController controller
                            = provider.new WriteController(this);
                    try {
                        provider.getView().promptWriteKey(controller);
                    } finally {
                        controller.close();
                    }
                }
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    RESET.retrieveReadKey(provider, true);
                }
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                RESET.retrieveWriteKey(provider);
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                RESET.retrieveReadKey(provider, true);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends SafeKey<K>> void
        retrieveWriteKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        resetCancelledKey(PromptingKeyProvider<K> provider);

        final <K extends SafeKey<K>> K
        getKey(PromptingKeyProvider<K> provider) {
            return provider.getKey0();
        }

        final <K extends SafeKey<K>> void
        setKey(PromptingKeyProvider<K> provider, K key) {
            provider.setKey0(key);
            provider.setState(null != key ? State.SET : State.CANCELLED);
        }

        <K extends SafeKey<K>> void
        setChangeRequested(PromptingKeyProvider<K> provider, boolean changeRequested) {
            provider.setChangeRequested(changeRequested);
        }

        <K extends SafeKey<K>> URI
        getResource(PromptingKeyProvider<K> provider) {
            return provider.getResource();
        }

        final <K extends SafeKey<K>> void
        setException(PromptingKeyProvider<K> provider, CacheableUnknownKeyException ex) {
            provider.setState(CANCELLED);
        }
    } // State

    /**
     * Used for the actual prompting of the user for a key (a password for
     * example) which is required to access a protected resource.
     * This interface is not depending on any particular techology, so
     * prompting could be implemented using Swing, the console, a web page or
     * no user interface technology at all.
     * <p>
     * Implementations of this interface are maintained by a
     * {@link PromptingKeyManager}.
     * <p>
     * Implementations of this interface must be thread safe
     * and should have no side effects!
     */
    public interface View<K extends SafeKey<K>> {

        /**
         * Prompts the user for the key for (over)writing the contents of a
         * new or existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called {@link Controller#setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key.
         * <p>
         * Otherwise, prompting for a key is permanently disabled and each
         * subsequent call to {@link #getWriteKey} or {@link #getReadKey}
         * results in a {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         *
         * @param  controller The key controller for storing the result.
         * @throws UnknownKeyException if key prompting fails for any reason.
         */
        void promptWriteKey(Controller<K> controller)
        throws UnknownKeyException;

        /**
         * Prompts the user for the key for reading the contents of an
         * existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called {@link Controller#setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key.
         * <p>
         * Otherwise, if the implementation has called {@link Controller#setKey}
         * with a {@code null} parameter or throws a
         * {@link KeyPromptingCancelledException}, then prompting for the key
         * is permanently disabled and each subsequent call to
         * {@link #getWriteKey} or {@link #getReadKey} results in a
         * {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         * <p>
         * Otherwise, the state of the key provider is not changed and this
         * method gets called again.
         *
         * @param  controller The key controller for storing the result.
         * @param  invalid {@code true} iff a previous call to this method
         *         resulted in an invalid key.
         * @throws KeyPromptingCancelledException if key prompting has been
         *         cancelled by the user.
         * @throws UnknownKeyException if key prompting fails for any other
         *         reason.
         */
        void promptReadKey(Controller<K> controller, boolean invalid)
        throws UnknownKeyException;
    } // View

    /** Proxies access to the key for {@link View} implementations. */
    public interface Controller<K extends SafeKey<K>> {

        /**
         * Returns the unique resource identifier (resource ID) of the
         * protected resource for which this controller is used.
         *
         * @throws IllegalStateException if getting this property is not legal
         *         in the current state.
         */
        URI getResource();

        /**
         * Returns the protected resource's key.
         *
         * @return The protected resource's key.
         * @throws IllegalStateException if getting key is not legal in the
         *         current state.
         */
        K getKey();

        /**
         * Sets the protected resource's key to a clone of the given key.
         *
         * @param  key The key to clone to use for the protected resource.
         * @throws IllegalStateException if setting key is not legal in the
         *         current state.
         */
        void setKey(K key);

        /**
         * Requests to prompt the user for a new key upon the next call to
         * {@link #getWriteKey()}, provided that the key is
         * {@link #setKey set} by then.
         *
         * @param  changeRequested whether or not the user shall get prompted
         *         for a new key upon the next call to {@link #getWriteKey()},
         *         provided that the key is {@link #setKey set} then.
         * @throws IllegalStateException if setting this property is not legal
         *         in the current state.
         */
        void setChangeRequested(boolean changeRequested);
    } // Controller

    /** Proxies access to the secret key for {@link View} implementations. */
    private abstract class BaseController implements Controller<K>, Closeable {
        private State state;

        BaseController(final State state) {
            this.state = state;
        }

        private State getState() {
            final State state = this.state;
            if (null == state)
                throw new IllegalStateException();
            return state;
        }

        @Override
        public void close() {
            this.state = null;
        }

        @Override
        public URI getResource() {
            final URI resource = getState().getResource(PromptingKeyProvider.this);
            if (null == resource)
                throw new IllegalStateException();
            return resource;
        }

        @Override
        public K getKey() {
            return getState().getKey(PromptingKeyProvider.this);
        }

        @Override
        public void setKey(K key) {
            getState().setKey(PromptingKeyProvider.this, key);
        }

        @Override
        public void setChangeRequested(boolean changeRequested) {
            getState().setChangeRequested(PromptingKeyProvider.this, changeRequested);
        }
    } // BaseController

    /**
     * The controller to use when promting for a secret key to encrypt a
     * protected resource.
     */
    private final class WriteController extends BaseController {
        WriteController(State state) {
            super(state);
        }

        @Override
        public void setChangeRequested(boolean changeRequested) {
            throw new IllegalStateException();
        }
    } // WriteController

    /**
     * The controller to use when promting for a secret key to decrypt a
     * protected resource.
     */
    private final class ReadController extends BaseController {
        ReadController(State state) {
            super(state);
        }

        @Override
        public K getKey() {
            throw new IllegalStateException();
        }
    } // ReadController
}