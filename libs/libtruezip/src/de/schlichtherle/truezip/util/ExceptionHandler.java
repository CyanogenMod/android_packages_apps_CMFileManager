/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * A generic callback interface designed to be implemented by <i>client
 * applications</i> in order to inject an exception handling strategy into a
 * <i>cooperative algorithm</i> as its dependency.
 * In the event of an exceptional condition, the cooperative algorithm would
 * then call this interface (with an exception provided as the cause) in order
 * to let the implementation decide how to proceed.
 * The implementation would then make the following decisions:
 * <ol>
 * <li>If possible, shall the control be returned to the cooperative algorithm
 *     in order to proceed its task?
 * <li>If not, shall the cause exception be wrapped in another exception before
 *     throwing it?
 * </ol>
 * <p>
 * Optionally, the implementation may perform additional operations such as
 * logging the cause exception or storing it for later use.
 * This implies that the implementation may be stateful and mutable, which
 * in turn implies that the cooperative algorithm should not bypass this
 * interface, i.e. it should never simply create and throw an exception without
 * calling this exception handler.
 * Otherwise some information, such as previous cause exceptions for example,
 * would get lost.
 * <p>
 * In general, the type parameter for the cause exception is determined by the
 * cooperative algorithm, whereas the type parameter for the thrown exception
 * is determined by the client application.
 * Where possible, the cooperative algorithm should declare generic method
 * signatures in order to enable the client application to select the type of
 * the thrown exception as desired (see below).
 * <p>
 * As an example for a cooperative algorithm which may benefit from using
 * this interface, consider the recursive copying of a directory tree:
 * Among many others, the copy algorithm may encounter the following
 * exceptional conditions:
 * <ul>
 * <li>A source file cannot get opened because of insufficient access
 *     privileges.
 * <li>A destination file cannot get written because the file system is full.
 * </ul>
 * <p>
 * Now the implementation may decide whether the copy algorithm shall proceed
 * with the remaining files and directories in the tree or if not, if the cause
 * exception should be wrapped in another exception in order to enable
 * diagnosing the situation by its client application.
 * <p>
 * Ideally, the copy algorithm could use two different exception handlers:
 * One for any exception when reading from a source file and another one for
 * any exception when writing to a destination file.
 * If these exception handlers would map any cause to a different exception
 * type, this would enable the client application to analyze the situation and
 * take appropriate action.
 * <p>
 * However, if the client doesn't care, it could simply provide the same
 * exception handler for both input and output exceptions to the copy algorithm.
 * <p>
 * Here's how a generic method declaration for a copy algorithm could look like:
 * <pre>{@code
 * public <IE extends Exception, OE extends Exception>
 * copy(   File src, ExceptionHandler<IOException, IE> inputHandler,
 *         File dst, ExceptionHandler<IOException, OE> outputHandler)
 * throws IE, OE {
 *     // ...
 * }
 * }</pre>
 * <p>
 * Note that the first type parameter for both handlers is an
 * {@link java.io.IOException} for the cause exception.
 * The second type parameter determines the type of exception which may be
 * thrown by the exception handlers themselves and is freely selectable.
 * <p>
 * TODO: Consider allowing {@link Throwable} as type parameters.
 *
 * @param  <C> the type of the cause exceptions.
 * @param  <X> the type of the thrown exceptions.
 * @author Christian Schlichtherle
 */
public interface ExceptionHandler<C extends Exception, X extends Exception> {

    /**
     * Called to handle an exceptional condition which
     * does <em>not</em>
     * allow the caller to proceed its task.
     * The implementation must return an appropriate exception to be thrown
     * by the cooperative algorithm.
     * Finally, if the implementation maintains a state, it must be updated
     * so that this instance can be reused to handle more exceptions!
     *
     * @param  cause the exception to handle.
     * @return The exception to throw.
     */
    X fail(C cause);

    /**
     * Called to handle an exceptional condition which
     * <em>does</em>
     * allow the caller to proceed its task.
     * The implementation may throw an exception of the parameter type
     * {@code T} or return from the call.
     * If the implementation maintains a state, it must be updated
     * so that this instance can be reused to handle more exceptions.
     *
     * @param  cause the exception to handle.
     * @throws X if the implementation wants the caller to abort its task.
     */
    void warn(C cause) throws X;
}
