/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.util.AbstractExceptionBuilder;
import java.lang.reflect.Constructor;

/**
 * Assembles a {@link SequentialIOException} from one or more
 * {@link Exception}s by
 * {@link SequentialIOException#initPredecessor(SequentialIOException) chaining}
 * them.
 * When the assembly is thrown or returned later, it is sorted by
 * {@link SequentialIOException#sortPriority() priority}.
 *
 * @param  <C> the type of the cause exceptions.
 * @param  <X> the type of the assembled exceptions.
 * @author Christian Schlichtherle
 */
public class SequentialIOExceptionBuilder<  C extends Exception,
                                            X extends SequentialIOException>
extends AbstractExceptionBuilder<C, X> {

    private final Class<X> assemblyClass;
    private volatile Constructor<X> assemblyConstructor;

    /**
     * Static constructor provided for comforting the most essential use case.
     *
     * @return A new sequential I/O exception builder.
     */
    public static SequentialIOExceptionBuilder<Exception, SequentialIOException>
    create() {
        return create(Exception.class, SequentialIOException.class);
    }

    public static <C extends Exception> SequentialIOExceptionBuilder<C, SequentialIOException>
    create(Class<C> clazz) {
        return create(clazz, SequentialIOException.class);
    }

    public static <C extends Exception, X extends SequentialIOException> SequentialIOExceptionBuilder<C, X>
    create(Class<C> cause, Class<X> assembly) {
        return new SequentialIOExceptionBuilder<C, X>(cause, assembly);
    }

    public SequentialIOExceptionBuilder(
            final Class<C> causeClass,
            final Class<X> assemblyClass) {
        this.assemblyClass = assemblyClass;
        try {
            if (!assemblyClass.isAssignableFrom(causeClass))
                wrap(null); // fail-fast test
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getCause());
        }
    }

    private Class<X> assemblyClass() { return assemblyClass; }

    private Constructor<X> assemblyConstructor() {
        final Constructor<X> ctor = this.assemblyConstructor;
        return null != ctor
                ? ctor
                : (this.assemblyConstructor = newAssemblyConstructor());
    }

    private Constructor<X> newAssemblyConstructor() {
        try {
            return assemblyClass().getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Chains the given exceptions and returns the result.
     *
     * @throws IllegalStateException if
     *         {@code cause.}{@link SequentialIOException#getPredecessor()} is
     *         already initialized by a previous call to
     *         {@link SequentialIOException#initPredecessor(SequentialIOException)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final X update(final C cause, final X previous) {
        X assembly = null;
        if (assemblyClass().isInstance(cause)) {
            assembly = (X) cause;
            if (assembly.isInitPredecessor()) {
                if (null == previous) return assembly;
                assembly = null;
            }
        }
        if (null == assembly) assembly = wrap(cause);
        assembly.initPredecessor(previous);
        return assembly;
    }

    private X wrap(C cause) {
        final X assembly = newAssembly(toString(cause));
        assembly.initCause(cause);
        return assembly;
    }

    private static String toString(Object obj) {
        return null == obj ? "" : obj.toString();
    }

    private X newAssembly(final String message) {
        final Constructor<X> ctor = assemblyConstructor();
        try {
            try {
                return ctor.newInstance(message);
            } catch (final IllegalAccessException ex) {
                ctor.setAccessible(true);
                return ctor.newInstance(message);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sorts the given exception chain by
     * {@link SequentialIOException#sortPriority() priority}
     * and returns the result.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final X post(X assembly) { return (X) assembly.sortPriority(); }
}
