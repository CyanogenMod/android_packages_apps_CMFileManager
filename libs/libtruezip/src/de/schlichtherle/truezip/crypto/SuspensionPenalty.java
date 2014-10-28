/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto;

import de.schlichtherle.truezip.util.Threads;

/**
 * A utility class for enforcing a suspension penalty of at least
 * {@link #MIN_KEY_RETRY_DELAY} milliseconds between two subsequent key
 * verifications.
 *
 * @author Christian Schlichtherle
 */
public class SuspensionPenalty {

    /**
     * The minimum delay between subsequent attempts to verify a key
     * in milliseconds.
     */
    public static final int MIN_KEY_RETRY_DELAY = 250;

    /* Can't touch this - hammer time! */
    private SuspensionPenalty() { }

    /**
     * Call this method in a key verification loop in order to enforce a
     * suspension penalty for providing a wrong key of at least
     * {@link #MIN_KEY_RETRY_DELAY} milliseconds.
     * Interrupting the current thread does not show any effect on this method.
     *
     * @param  last the last try time.
     *         This should be zero upon the first call.
     *         Subsequent calls should provide the return value of the last
     *         call.
     * @return The new try time.
     */
    public static long enforce(final long last) {
        final long start = System.currentTimeMillis();
        final long elapsed = start - last;
        final long delay = MIN_KEY_RETRY_DELAY - elapsed;
        if (0 < delay) {
            Threads.pause(delay);
            return start + delay; // ~ System.currentTimeMillis()
        } else {
            return start; // ~ System.currentTimeMillis()
        }
    }

    public static void apply() {
        Threads.pause(MIN_KEY_RETRY_DELAY);
    }
}
