/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;

/**
 * An inheritable thread local stack of elements.
 * This class works pretty much like a {@link java.util.Stack}, except that
 * each call is forwarded to a private {@link InheritableThreadLocal} object.
 * <p>
 * Whenever a child thread gets started, it inherits the top level element
 * of the parent's thread local stack unless the parent's thread local stack
 * is empty.
 * However, it's not possible to {@link #pop} this inherited top level element
 * off the stack - any attempt to do so will result in a
 * {@link NoSuchElementException}.
 * <p>
 * <b>Disclaimer</b>: Although this classes internally uses an
 * {@link InheritableThreadLocal}, it does not leak memory in multi class
 * loader environments when used appropriately.
 *
 * @param  <T> The type of the elements in the inheritable thread local stack.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public final class InheritableThreadLocalStack<T> {
    private final InheritableThreadLocal<Node<T>> nodes
            = new InheritableThreadLocal<Node<T>>();

    /**
     * Returns {@code true} if this stack is empty.
     *
     * @return {@code true} if this stack is empty.
     */
    public boolean isEmpty() {
        return null == nodes.get();
    }

    /**
     * Returns the top element on this stack or {@code null} if it's empty.
     *
     * @return The top element of this stack or {@code null} if it's empty.
     */
    public T peek() {
        final Node<T> node = nodes.get();
        return null != node ? node.element : null;
    }

    /**
     * Returns the nullable top element on this stack unless it's empty,
     * in which case {@code elze} gets returned.
     *
     * @param  elze the nullable default element.
     * @return The nullable top element on this stack unless it's empty,
     *         in which case {@code elze} gets returned.
     */
    public T peekOrElse(final T elze) {
        final Node<T> node = nodes.get();
        return null != node ? node.element : elze;
    }

    /**
     * Pushes the given element onto this stack.
     *
     * @param  element the nullable element to push onto this stack.
     * @return {@code element} - for fluent programming.
     */
    public T push(final T element) {
        final Node<T> previous = nodes.get();
        final Node<T> next = new Node<T>(previous, element);
        nodes.set(next);
        return element;
    }

    /**
     * Removes and returns the nullable top element on this stack.
     *
     * @return The (then no more) nullable top element on this stack.
     * @throws NoSuchElementException if this stack is empty.
     */
    public T pop() {
        final Node<T> node = nodes.get();
        if (null == node)
            throw new NoSuchElementException();
        if (!Thread.currentThread().equals(node.get()))
            throw new NoSuchElementException();
        nodes.set(node.previous); // may be null!
        return node.element;
    }

    /**
     * Removes and returns the nullable top element on this stack
     * if it's identical to the given element.
     *
     * @param  expected The expected top element on this stack.
     * @throws IllegalStateException If the given element is not the top
     *         element on this stack.
     */
    public void popIf(final T expected) {
        try {
            final T got = pop();
            if (got != expected) {
                push(got);
                throw new IllegalStateException(got + " (expected " + expected + " as the top element of the inheritable thread local stack)");
            }
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("The inheritable thread local stack is empty!", ex);
        }
    }

    private static class Node<T> extends WeakReference<Thread> {
        final Node<T> previous;
        T element;

        Node(   final Node<T> previous,
                final T element) {
            super(Thread.currentThread());
            this.previous = previous;
            this.element = element;
        }
    } // Node
}
