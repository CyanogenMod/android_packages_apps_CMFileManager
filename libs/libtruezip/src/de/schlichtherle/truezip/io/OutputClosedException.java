/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

/**
 * Indicates that an output resource (output stream etc.) has been closed.
 *
 * @see     InputClosedException
 * @author  Christian Schlichtherle
 */
// TODO: Remove this class and just use its super class.
public class OutputClosedException extends ClosedException {
    private static final long serialVersionUID = 4563928734723923649L;
}