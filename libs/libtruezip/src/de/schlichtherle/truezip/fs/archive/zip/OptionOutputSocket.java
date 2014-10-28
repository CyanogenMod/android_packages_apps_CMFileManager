/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output socket which provides a property for its output options.
 *
 * @author  Christian Schlichtherle
 */
public final class OptionOutputSocket
extends DecoratingOutputSocket<Entry> {
    final BitField<FsOutputOption> options;

    public OptionOutputSocket(
            final OutputSocket<?> output,
            final BitField<FsOutputOption> options) {
        super(output);
        this.options = options;
    }

    public BitField<FsOutputOption> getOptions() {
        return options;
    }

    @Override
    public Entry getLocalTarget() throws IOException {
        return getBoundSocket().getLocalTarget();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getBoundSocket().newOutputStream();
    }
}