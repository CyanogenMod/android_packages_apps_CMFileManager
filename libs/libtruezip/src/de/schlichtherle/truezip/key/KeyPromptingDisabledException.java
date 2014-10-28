/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been disabled.
 * This is normally caused by the client application, but will also happen
 * if the JVM is running in headless mode.
 *
 * @author  Christian Schlichtherle
 */
public class KeyPromptingDisabledException extends CacheableUnknownKeyException  {
    private static final long serialVersionUID = 7656348649239172586L;

    public KeyPromptingDisabledException() {
        super("Key prompting has been disabled!");
    }

    public KeyPromptingDisabledException(Throwable cause) {
        super("Key prompting has been disabled!");
        super.initCause(cause);
    }
}