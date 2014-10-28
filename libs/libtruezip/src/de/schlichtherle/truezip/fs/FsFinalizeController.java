/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 *
 * @param  <M> the type of the file system model.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
final class FsFinalizeController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final Logger logger = Logger.getLogger(FsFinalizeController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private static final IOException OK = new IOException((Throwable) null);

    /**
     * Constructs a new file system finalize controller.
     *
     * @param controller the decorated file system controller.
     */
    FsFinalizeController(FsController<? extends M> controller) {
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

    static void finalize(   final Closeable delegate,
                            final IOException close) {
        if (OK == close) {
            logger.log(Level.FINEST, "Finalizing stream or channel where the last call to close() has succeeded.");
        } else if (null != close) {
            logger.log(Level.FINEST, "Finalizing stream or channel where the last call to close() has failed:", close);
        } else {
            try {
                delegate.close();
                logger.log(Level.INFO, "Successfully close()d a finalizable open stream or channel.");
            } catch (final ControlFlowException ex) {  // log and swallow!
                logger.log(Level.SEVERE, "Failed to close() a finalizable open stream or channel:",
                        new IOException("Unexpected control flow exception!", ex));
            } catch (final Throwable ex) {              // log and swallow!
                logger.log(Level.WARNING, "Failed to close() a finalizable open stream or channel:", ex);
            }
        }
    }

    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsFinalizeController<?> controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsFinalizeController<?> controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsFinalizeController<?> controller,
                FsEntryName name,
                BitField<FsInputOption> options);

        abstract OutputSocket<?> newOutputSocket(
                FsFinalizeController<?> controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template);
    } // SocketFactory

    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsFinalizeController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new FinalizeReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new FinalizeInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final Entry template) {
            super(FsFinalizeController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new FinalizeOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private static final class FinalizeReadOnlyFile
    extends DecoratingReadOnlyFile {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                close = OK;
            } catch (final IOException ex) {
                throw close = ex;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                FsFinalizeController.finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeReadOnlyFile

    private static final class FinalizeInputStream
    extends DecoratingInputStream {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                close = OK;
            } catch (final IOException ex) {
                throw close = ex;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                FsFinalizeController.finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeInputStream

    private static final class FinalizeOutputStream
    extends DecoratingOutputStream {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                close = OK;
            } catch (final IOException ex) {
                throw close = ex;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                FsFinalizeController.finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeOutputStream
}
