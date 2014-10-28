/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.crypto.SuspensionPenalty;

/**
 * Provides the base functionality required to implement a "safe" key provider.
 * Each instance of this class maintains a single instance of the interface
 * {@link SafeKey}).
 * A clone of this key is returned on each call to {@link #getWriteKey}
 * and {@link #getReadKey}.
 *
 * @param   <K> The type of the keys.
 * @author  Christian Schlichtherle
 */
public abstract class SafeKeyProvider<K extends SafeKey<K>>
implements KeyProvider<K> {

    /**
     * The minimum delay between subsequent attempts to verify a key in
     * milliseconds.
     * More specifically, this is the minimum delay between two calls to
     * {@link #getReadKey} by the same thread if the parameter {@code invalid}
     * is {@code true}.
     */
    public static final int MIN_KEY_RETRY_DELAY = SuspensionPenalty.MIN_KEY_RETRY_DELAY;

    private volatile K key;

    private final ThreadLocal<Long> invalidated = new ThreadLocal<Long>();

    /**
     * Constructs a new safe key provider.
     */
    protected SafeKeyProvider() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #retrieveWriteKey}.
     *
     * @throws UnknownKeyException If {@code retrieveWriteKey} throws
     *         this exception or the secret key is still {@code null}.
     */
    @Override
    public final K getWriteKey() throws UnknownKeyException {
        retrieveWriteKey();
        return getNonNullKey();
    }

    /**
     * Retrieves the secret key for the encryption of a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the secret key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getWriteKey
     */
    protected abstract void retrieveWriteKey()
    throws UnknownKeyException;

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #retrieveReadKey} and enforces a three seconds suspension penalty
     * if {@code invalid} is {@code true} before returning.
     * Because this method is final, this qualifies the implementation in
     * this class as a "safe" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @throws UnknownKeyException If {@code retrieveReadKey} throws
     *         this exception or the secret key is still {@code null}.
     */
    @Override
    public final K getReadKey(boolean invalid) throws UnknownKeyException {
        if (invalid)
            invalidated.set(System.currentTimeMillis());
        try {
            retrieveReadKey(invalid);
        } finally {
            final Long invalidated = this.invalidated.get();
            SuspensionPenalty.enforce(null == invalidated ? 0 : invalidated);
        }
        return getNonNullKey();
    }

    /**
     * Retrieves the secret key for the decryption of a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the secret key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getReadKey
     */
    protected abstract void retrieveReadKey(boolean invalid)
    throws UnknownKeyException;

    private K getNonNullKey() throws UnknownKeyException {
        final K key = getKey();
        if (null == key)
            throw new UnknownKeyException();
        return key;
    }

    protected K getKey() {
        final K key = this.key;
        return null == key ? null : key.clone();
    }

    @Override
    public void setKey(final K newKey) {
        final K oldKey = this.key;
        this.key = null == newKey ? null : newKey.clone();
        if (null != oldKey)
            oldKey.reset();
    }
}