/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.fs.FsSyncOptions.RESET;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Performs a {@link FsController#sync(BitField) sync} operation on the
 * file system if and only if any decorated file system controller throws an
 * {@link FsNeedsSyncException}.
 *
 * @see    FsNeedsSyncException
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
final class FsSyncController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private static final BitField<FsSyncOption> NOT_WAIT_CLOSE_IO
            = BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT).not();

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    FsSyncController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    void sync(final FsNeedsSyncException trigger)
    throws FsSyncException {
        checkWriteLockedByCurrentThread();
        try {
            sync(SYNC);
        } catch (final FsSyncException ex) {
            throw ex;
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return delegate.isReadOnly();
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return delegate.getEntry(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isReadable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isWritable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isExecutable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        while (true) {
            try {
                delegate.setReadOnly(name);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, times, options);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, types, value, options);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this, name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this, name, options, template);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        while (true) {
            try {
                delegate.mknod(name, type, options, template);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                // HC SVNT DRACONES!
                delegate.unlink(name, options);
                if (name.isRoot()) {
                    // Make the file system controller chain eligible for GC.
                    delegate.sync(RESET);
                }
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        assert isWriteLockedByCurrentThread();
        assert !isReadLockedByCurrentThread();

        // HC SVNT DRACONES!
        final BitField<FsSyncOption> modified = modify(options);
        while (true) {
            try {
                delegate.sync(modified);
                return;
            } catch (final FsSyncException ex) {
                if (ex.getCause() instanceof FsResourceOpenException
                        && modified != options) {
                    assert(!(ex instanceof FsSyncWarningException));
                    // Swallow ex.
                    throw FsNeedsLockRetryException.get();
                } else {
                    throw ex;
                }
            } catch (FsNeedsSyncException yeahIKnow_IWasActuallyDoingThat) {
                // This exception was thrown by the resource controller in
                // order to indicate that the state of the virtual file
                // system may have completely changed as a side effect of
                // temporarily releasing its write lock.
                // The sync operation needs to get repeated.
            }
        }
    }

    /**
     * Modify the sync options so that no dead lock can appear due to waiting
     * for I/O resources in a recursive file system operation.
     *
     * @param  options the sync options
     * @return the potentially modified sync options.
     */
    static BitField<FsSyncOption> modify(final BitField<FsSyncOption> options) {
        final boolean isRecursive = 1 < FsLockController.getLockCount();
        final BitField<FsSyncOption> result = isRecursive
                ? options.and(NOT_WAIT_CLOSE_IO)
                : options;
        assert result == options == result.equals(options) : "Broken contract in BitField.and()!";
        assert result == options || isRecursive;
        return result;
    }

    void close(final Closeable closeable) throws IOException {
        while (true) {
            try {
                closeable.close();
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsSyncController controller,
                FsEntryName name,
                BitField<FsInputOption> options);

        abstract OutputSocket<?> newOutputSocket(
                FsSyncController controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template);
    } // SocketFactory

    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsSyncController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            while (true) {
                try {
                    return new SyncReadOnlyFile(
                            getBoundSocket().newReadOnlyFile());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            while (true) {
                try {
                    return new SyncInputStream(
                            getBoundSocket().newInputStream());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Input

    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final Entry template) {
            super(FsSyncController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            while (true) {
                try {
                    return new SyncOutputStream(
                            getBoundSocket().newOutputStream());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Output

    private final class SyncReadOnlyFile
    extends DecoratingReadOnlyFile {
        SyncReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            FsSyncController.this.close(delegate);
        }
    } // SyncReadOnlyFile

    private final class SyncInputStream
    extends DecoratingInputStream {
        SyncInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            FsSyncController.this.close(delegate);
        }
    } // SyncInputStream

    private final class SyncOutputStream
    extends DecoratingOutputStream {
        SyncOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            FsSyncController.this.close(delegate);
        }
    } // SyncOutputStream
}
