/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.util.BitField;

/**
 * Provides common output options.
 *
 * @see    FsOutputOption
 * @see    FsInputOptions
 * @since  TrueZIP 7.1.1
 * @author Christian Schlichtherle
 */
public final class FsOutputOptions {

    /**
     * A bit field with no output options set.
     *
     * @since TrueZIP 7.5
     */
    public static final BitField<FsOutputOption>
            NONE = BitField.noneOf(FsOutputOption.class);

    /**
     * @deprecated
     * @since TrueZIP 7.2
     * @see #NONE
     */
    @Deprecated
    public static final BitField<FsOutputOption> NO_OUTPUT_OPTIONS = NONE;

    /**
     * @deprecated
     * @since TrueZIP 7.1.1
     * @see #NONE
     */
    @Deprecated
    public static final BitField<FsOutputOption> NO_OUTPUT_OPTION = NONE;

    /**
     * The mask of output preferences, which is
     * <code>{@link BitField}.of({@link FsOutputOption#CACHE}, {@link FsOutputOption#CREATE_PARENTS}, {@link FsOutputOption#STORE}, {@link FsOutputOption#COMPRESS}, {@link FsOutputOption#GROW}, {@link FsOutputOption#ENCRYPT})</code>.
     *
     * @since TrueZIP 7.3
     */
    public static final BitField<FsOutputOption> OUTPUT_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT);

    /**
     * Converts the given array to a bit field of output options.
     *
     * @param  options an array of output options.
     * @return A bit field of output options.
     * @since  TrueZIP 7.5
     */
    public static BitField<FsOutputOption> of(FsOutputOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsOutputOptions() { }
}
