/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Injects the original values of selected parameters of the operation in
 * progress into its decorated controller.
 *
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
final class FsContextController
extends FsLockModelDecoratingController<FsTargetArchiveController<?>> {

    private static final FsOperationContext
            NULL = new FsOperationContext(FsOutputOptions.NONE);

    /**
     * Constructs a new file system context controller.
     *
     * @param controller the decorated file system controller.
     */
    FsContextController(FsTargetArchiveController<?> controller) {
        super(controller);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadOnly();
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.getEntry(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isWritable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isExecutable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            delegate.setReadOnly(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            return delegate.setTime(name, times, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            return delegate.setTime(name, types, value, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return new Input(delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return new Output(delegate.getOutputSocket(name, options, template),
                options);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            delegate.unlink(name, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            delegate.sync(options);
        } finally {
            delegate.setContext(context);
        }
    }

    /**
     * Returns an operation context holding the given output options.
     *
     * @param  options the options for the output operation in progress.
     * @return An operation context holding the given output options.
     */
    private static FsOperationContext makeContext(
            final BitField<FsOutputOption> options) {
        return new FsOperationContext(options);
    }

    private final class Input extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().newReadOnlyFile();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().newInputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Input

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsOperationContext operation;

        Output( OutputSocket<?> output,
                BitField<FsOutputOption> options) {
            super(output);
            this.operation = makeContext(options);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundSocket().newOutputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Output
}
