/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.Map;

/**
 * A provider for an immutable map of file system schemes to drivers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface FsDriverProvider {

    /**
     * Returns a dedicated immutable map of file system schemes to drivers.
     * Neither the keys nor the values of the returned map may be {@code null}
     * and subsequent calls must return the same map.
     *
     * @return A dedicated immutable map of file system schemes to drivers.
     */
    Map<FsScheme, FsDriver> get();
}
