/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * Static utility methods for {@link Throwable}s.
 *
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public class Throwables {

    /* Can't touch this - hammer time! */
    private Throwables() { }

    /**
     * Wraps the given throwable in a new instance of the same class if
     * possible.
     *
     * @param  <T> the compile time type of the throwable to return.
     * @param  ex the throwable to wrap in a new instance of the same class.
     * @return a new instance of the same class as the throwable {@code t}
     *         with its {@linkplain Throwable#getCause() cause} initialized to
     *         {@code t} or simply {@code t} if the wrapping fails due to
     *         another exception.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T wrap(final T ex) {
        try {
            return (T) ex.getClass()
                        .getConstructor(String.class)
                        .newInstance(ex.toString())
                        .initCause(ex);
        } catch (final Exception ex2) {
            return ex;
        }
    }

    /**
     * Returns {@code true} if and only if {@code thiz} is identical to
     * {@code that} or has been (recursively)
     * {@linkplain Throwable#initCause caused} by it.
     *
     * @param  thiz the throwable to search for {@code that} throwable.
     * @param  that the throwable to look up.
     * @return {@code true} if and only if {@code thiz} is identical to
     *         {@code that} or has been (recursively) caused by it.
     */
    public static boolean contains(Throwable thiz, final Throwable that) {
        do {
            if (thiz == that)
                return true;
        } while (null != (thiz = thiz.getCause()));
        return false;
    }
}
