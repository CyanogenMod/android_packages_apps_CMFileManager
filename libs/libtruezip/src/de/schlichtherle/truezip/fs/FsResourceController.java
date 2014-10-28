/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsResourceAccountant.Resources;
import static de.schlichtherle.truezip.fs.FsSyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_OUTPUT;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ControlFlowException;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Accounts input and output resources returned by its decorated controller.
 *
 * @see    FsResourceAccountant
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
final class FsResourceController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private final FsResourceAccountant accountant =
            new FsResourceAccountant(writeLock());

    FsResourceController(FsController<? extends FsLockModel> controller) {
        super(controller);
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
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        assert isWriteLockedByCurrentThread();
        assert !isReadLockedByCurrentThread();

        // HC SVNT DRACONES!
        final Resources beforeWait = accountant.resources();
        if (0 == beforeWait.total) {
            delegate.sync(options);
            return;
        }

        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        {
            final boolean forceCloseIo = forceCloseIo(options);
            try {
                if (0 != beforeWait.local && !forceCloseIo)
                    throw new FsResourceOpenException(beforeWait.total, beforeWait.local);
                accountant.awaitClosingOfOtherThreadsResources(
                        waitCloseIo(options) ? 0 : WAIT_TIMEOUT_MILLIS);
                final Resources afterWait = accountant.resources();
                if (0 != afterWait.total)
                    throw new FsResourceOpenException(afterWait.total, afterWait.local);
            } catch (final FsResourceOpenException ex) {
                if (!forceCloseIo)
                    throw builder.fail(new FsSyncException(getModel(), ex));
                builder.warn(new FsSyncWarningException(getModel(), ex));
            }
        }
        closeResources(builder);
        if (beforeWait.needsWaiting()) {
            // awaitClosingOfOtherThreadsResources(*) has temporarily released
            // the write lock, so the state of the virtual file system may have
            // completely changed and thus we need to restart the sync
            // operation unless an exception occured.
            builder.check();
            throw FsNeedsSyncException.get();
        }
        try { delegate.sync(options); }
        catch (final FsSyncException ex) { throw builder.fail(ex); }
        builder.check();
    }

    private static boolean waitCloseIo(final BitField<FsSyncOption> options) {
        return options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
    }

    private static boolean forceCloseIo(final BitField<FsSyncOption> options) {
        return options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param builder the exception handling strategy.
     */
    private void closeResources(final FsSyncExceptionBuilder builder) {
        final class IOExceptionHandler
        implements ExceptionHandler<IOException, RuntimeException> {
            @Override
            public RuntimeException fail(final IOException ex) {
                throw new AssertionError(ex);
            }

            @Override
            public void warn(final IOException ex) {
                builder.warn(new FsSyncWarningException(getModel(), ex));
            }
        } // IOExceptionHandler
        accountant.closeAllResources(new IOExceptionHandler());
    }

    /**
     * Close()s the given {@code resource} and finally stops accounting for the
     * given {@code account} unless a {@link ControlFlowException} is thrown.
     *
     * @param  delegate the resource to close().
     * @param  thiz the resource to eventually stop accounting for.
     * @throws IOException on any I/O error.
     * @see http://java.net/jira/browse/TRUEZIP-279 .
     */
    private void close(final Closeable delegate, final Closeable thiz)
    throws IOException {
        boolean cfe = false;
        try {
            delegate.close();
        } catch (final ControlFlowException ex) {
            cfe = true;
            throw ex;
        } finally {
            if (!cfe) accountant.stopAccountingFor(thiz);
        }
    }

    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsResourceController controller,
                FsEntryName name,
                BitField<FsInputOption> options);

        abstract OutputSocket<?> newOutputSocket(
                FsResourceController controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template);
    } // SocketFactory

    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsResourceController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new ResourceReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new ResourceInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final Entry template) {
            super(FsResourceController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new ResourceOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class ResourceReadOnlyFile
    extends DecoratingReadOnlyFile {
        ResourceReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            FsResourceController.this.close(delegate, this);
        }
    } // ResourceReadOnlyFile

    private final class ResourceInputStream
    extends DecoratingInputStream {
        ResourceInputStream(InputStream in) {
            super(in);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            FsResourceController.this.close(delegate, this);
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        ResourceOutputStream(OutputStream out) {
            super(out);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            FsResourceController.this.close(delegate, this);
        }
    } // ResourceOutputStream
}
