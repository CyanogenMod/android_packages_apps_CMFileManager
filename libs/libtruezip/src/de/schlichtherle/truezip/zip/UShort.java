/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Provides constants and static utility methods for unsigned short integer
 * values ({@value SIZE} bits).
 *
 * @author  Christian Schlichtherle
 */
final class UShort {

    /**
     * The minimum value of an unsigned short integer,
     * which is {@value MIN_VALUE}.
     */
    public static final int MIN_VALUE = 0x0000;

    /**
     * The maximum value of an unsigned short integer,
     * which is {@value MAX_VALUE}.
     */
    public static final int MAX_VALUE = 0xffff;

    /**
     * The number of bits used to represent an unsigned short integer in
     * binary form, which is {@value SIZE}.
     */
    public static final int SIZE = 16;

    /** This class cannot get instantiated. */
    private UShort() {
    }

    /**
     * Checks the parameter range.
     *
     * @param  i The integer to check to be in the range of an unsigned short
     *         integer ({@value SIZE} bits).
     * @param  subject The subject of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @param  error First sentence of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @return {@code true}
     * @throws IllegalArgumentException If {@code i} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(
            final int i,
            final String subject,
            final String error) {
        if (MIN_VALUE <= i && i <= MAX_VALUE)
            return true;

        final StringBuilder message = new StringBuilder();
        if (null != subject) {
            message.append(subject);
            message.append(": ");
        }
        if (null != error) {
            message.append(error);
            message.append(": ");
        }
        message.append(i);
        message.append(" is not within ");
        message.append(MIN_VALUE);
        message.append(" and ");
        message.append(MAX_VALUE);
        message.append(" inclusive.");
        throw new IllegalArgumentException(message.toString());
    }

    /**
     * Checks the parameter range.
     *
     * @param  i The integer to check to be in the range of an unsigned short
     *         integer ({@value SIZE} bits).
     * @return {@code true}
     * @throws IllegalArgumentException If {@code i} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(final int i) {
        return check(i, "Integer out of range", null);
    }
}