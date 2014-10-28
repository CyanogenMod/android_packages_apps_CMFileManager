/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.util.BitField;

/**
 * Represents the original values of selected parameters for the
 * {@linkplain FsContextController file system controller} operation in
 * progress.
 *
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
final class FsOperationContext {

    private final BitField<FsOutputOption> outputOptions;

    FsOperationContext(final BitField<FsOutputOption> outputOptions) {
        assert null != outputOptions;
        this.outputOptions = outputOptions;
    }

    /**
     * Returns the options for the output operation in progress.
     * If the operation in progress is not an output operation or does not
     * accept output options, then an empty bit field is returned.
     * Otherwise, a bit field with the options for the output operation in
     * progress is returned.
     *
     * @return The options for the output operation in progress.
     * @see    FsContextController#getOutputSocket(FsEntryName, BitField, Entry)
     * @see    FsContextController#mknod(FsEntryName, Entry.Type, BitField, Entry)
     */
    BitField<FsOutputOption> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Returns {@code true} if and only if the given output option is set.
     *
     * @param option The output option to test.
     */
    boolean get(final FsOutputOption option) {
        return outputOptions.get(option);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[outputOptions=%s]",
                getClass().getName(),
                getOutputOptions());
    }
}