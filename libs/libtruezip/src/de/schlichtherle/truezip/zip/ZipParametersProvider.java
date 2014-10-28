/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * A provider of {@link ZipParameters} for a given type.
 * The implementation of this interface enables to retrieve ZIP parameters on
 * demand rather than providing them upfront for any possible type.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public interface ZipParametersProvider extends ZipParameters {

    /**
     * Returns {@link ZipParameters} of the given {@code type}
     * or {@code null} if not available.
     *
     * @param  type the {@link ZipParameters} interface class which's
     *         implementation is required.
     * @return {@link ZipParameters} of the given {@code type}
     *         or {@code null} if not available.
     */
    <P extends ZipParameters> P get(Class<P> type);
}