/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been cancelled.
 * This is normally caused by user input, for example if the user has closed
 * the prompting dialog.
 *
 * @author  Christian Schlichtherle
 */
public class KeyPromptingCancelledException extends UnknownKeyException
        implements CancelledOperation {
    private static final long serialVersionUID = 7616200629250616595L;

    public KeyPromptingCancelledException() {
        super("Key prompting has been cancelled!");
    }
}