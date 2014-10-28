/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Concatenates two enumerations.
 *
 * @author  Christian Schlichtherle
 */
public final class JointEnumeration<E> implements Enumeration<E> {
    private Enumeration<? extends E> e1;
    private final Enumeration<? extends E> e2;

    public JointEnumeration(
            final Enumeration<? extends E> e1,
            final Enumeration<? extends E> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
	public boolean hasMoreElements() {
        return e1.hasMoreElements()
           || (e1 != e2 && (e1 = e2).hasMoreElements());
    }

    @Override
	public E nextElement() {
        try {
            return e1.nextElement();
        } catch (NoSuchElementException ex) {
            if (e1 == e2)
                throw ex;
            return (e1 = e2).nextElement();
        }
    }
}