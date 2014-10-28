/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsInputOption.CACHE;
import de.schlichtherle.truezip.util.BitField;

/**
 * Provides common input options.
 *
 * @see    FsInputOption
 * @see    FsOutputOptions
 * @since  TrueZIP 7.1.1
 * @author Christian Schlichtherle
 */
public final class FsInputOptions {

    /**
     * A bit field with no input options set.
     *
     * @since TrueZIP 7.5
     */
    public static final BitField<FsInputOption>
            NONE = BitField.noneOf(FsInputOption.class);

    /**
     * @deprecated
     * @since TrueZIP 7.2
     * @see #NONE
     */
    @Deprecated
    public static final BitField<FsInputOption> NO_INPUT_OPTIONS = NONE;

    /**
     * @deprecated
     * @since TrueZIP 7.1.1
     * @see #NONE
     */
    @Deprecated
    public static final BitField<FsInputOption> NO_INPUT_OPTION = NONE;

    /**
     * The mask of input preferences, which is
     * <code>{@link BitField}.of({@link FsInputOption#CACHE})</code>.
     *
     * @since TrueZIP 7.3
     */
    public static final BitField<FsInputOption> INPUT_PREFERENCES_MASK
            = BitField.of(CACHE);

    /**
     * Converts the given array to a bit field of input options.
     *
     * @param  options an array of input options.
     * @return A bit field of input options.
     * @since  TrueZIP 7.5
     */
    public static BitField<FsInputOption> of(FsInputOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsInputOptions() { }
}
