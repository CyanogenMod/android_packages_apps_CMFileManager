/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * Indicates a condition which requires non-local control flow.
 * Note that this class is an {@code Error} rather than a
 * {@link RuntimeException} just to prevent it from being accidentally catched.
 *
 * @since  TrueZIP 7.6 (renamed from {@code FsControllerException} and changed
 *         super class from {@code IOException}.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial") // serializing control flow exceptions is nonsense!
public abstract class ControlFlowException extends Error {

    private static final String TRACEABLE_PROPERTY_KEY
            = ControlFlowException.class.getName() + ".traceable";

    public ControlFlowException() { this(null); }

    public ControlFlowException(Throwable cause) { super(cause); }

    /**
     * Returns {@code true} if and only if a control flow exception should have
     * a full stack trace instead of an empty stack trace.
     * If and only if the system property with the key string
     * {@code de.schlichtherle.truezip.util.ControlFlowException.traceable}
     * is set to {@code true} (whereby case is ignored), then instances of this
     * class will have a regular stack trace, otherwise their stack trace will
     * be empty.
     * Note that this should be set to {@code true} for debugging purposes only.
     *
     * @return {@code true} if and only if a control flow exception should have
     *         a full stack trace instead of an empty stack trace.
     */
    public static boolean isTraceable() {
        return Boolean.getBoolean(TRACEABLE_PROPERTY_KEY);
    }

    /**
     * Fills in an empty stack trace for optimum performance.
     * <em>Warning:</em> This method is called from the constructors in the
     * super class {@code Throwable}!
     *
     * @return {@code this}
     * @see <a href="http://blogs.oracle.com/jrose/entry/longjumps_considered_inexpensive">Longjumps Considered Inexpensive</a>
     */
    @Override
    public Throwable fillInStackTrace() {
        return isTraceable() ? super.fillInStackTrace() : this;
    }
}
