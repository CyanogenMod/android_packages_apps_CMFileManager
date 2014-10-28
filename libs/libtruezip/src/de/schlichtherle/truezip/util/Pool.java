/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * An interface for pooling strategies.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to the implementation of its
 * managed resources.
 *
 * @param   <R> The type of the resources managed by this pool.
 * @param   <X> The type of the exceptions thrown by this pool.
 * @author  Christian Schlichtherle
 */
public interface Pool<R, X extends Exception> {

    /**
     * Allocates a resource from this pool.
     * <p>
     * Mind that a pool implementation should not hold references to its
     * allocated resources because this could cause a memory leak.
     *
     * @return A resource.
     * @throws X if allocating the resource fails for any reason.
     */
    R allocate() throws X;

    /**
     * Releases a previously allocated resource to this pool.
     *
     * @param  resource a resource.
     * @throws IllegalArgumentException if the given resource has not been
     *         allocated by this pool and the implementation cannot tolerate
     *         this.
     * @throws X if releasing the resource fails for any other reason.
     */
    void release(R resource) throws X;

    /**
     * This interface is designed to be used with Pools which enable their
     * resources to release itself.
     * TODO for TrueZIP 8: This should be named "Resource".
     *
     * @param <X> The type of the exceptions thrown by this releasable.
     */
    interface Releasable<X extends Exception> {

        /**
         * Releases this resource to its pool.
         *
         * @throws X if releasing the resource fails for any reason.
         */
        void release() throws X;
    } // Releasable
}