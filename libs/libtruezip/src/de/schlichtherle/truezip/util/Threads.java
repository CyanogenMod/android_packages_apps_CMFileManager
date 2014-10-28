/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * Static utility methods for {@link Thread}s.
 *
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public class Threads {

    /* Can't touch this - hammer time! */
    private Threads() { }

    /**
     * Uninterruptibly pauses the current thread for the given time interval.
     *
     * @param  millis the milliseconds to pause the current thread.
     * @throws IllegalArgumentException if {@code millis} is negative.
     */
    public static void pause(long millis) {
        final long start = System.currentTimeMillis();
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupt) {
                interrupted = true;
            }
        } while (0 < (millis -= System.currentTimeMillis() - start));
        if (interrupted) Thread.currentThread().interrupt(); // restore
    }
}
