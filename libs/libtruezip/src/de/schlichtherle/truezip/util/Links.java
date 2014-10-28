/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import de.schlichtherle.truezip.util.Link.Type;

/**
 * Static utility methods for links.
 *
 * @author Christian Schlichtherle
 */
public class Links {

    /* Can't touch this - hammer time! */
    private Links() { }

    /**
     * Returns a nullable (strong) link to the given target.
     * The returned link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable (strong) link to the given target.
     */
    public static <T> Link<T> newLink(T target) {
        return newLink(Type.STRONG, target);
    }

    /**
     * Returns a nullable typed link to the given target.
     * The returned typed link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable typed link to the given target.
     */
    public static <T> Link<T> newLink(Type type,
                                                    T target) {
        return null == target ? null : type.newLink(target);
    }

    /**
     * Returns the nullable {@link Link#getTarget() target} of the given link.
     * The returned target is {@code null} if and only if either the given
     * link is {@code null} or its target is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  link a nullable link.
     * @return The nullable {@link Link#getTarget() target} of the given link.
     */
    public static <T> T getTarget(Link<T> link) {
        return null == link ? null : link.getTarget();
    }
}
