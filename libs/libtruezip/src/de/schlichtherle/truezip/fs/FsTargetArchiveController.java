/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.ALL_SIZE_SET;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.READ;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import de.schlichtherle.truezip.entry.Entry.Size;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.SPECIAL;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.fs.FsArchiveFileSystem.newEmptyFileSystem;
import static de.schlichtherle.truezip.fs.FsArchiveFileSystem.newPopulatedFileSystem;
import static de.schlichtherle.truezip.fs.FsOutputOption.CACHE;
import static de.schlichtherle.truezip.fs.FsOutputOption.GROW;
import static de.schlichtherle.truezip.fs.FsOutputOptions.OUTPUT_PREFERENCES_MASK;
import static de.schlichtherle.truezip.fs.FsSyncOption.ABORT_CHANGES;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.key.AuthenticationFailedOperation;
import de.schlichtherle.truezip.key.CancelledOperation;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ControlFlowException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;

/**
 * Manages I/O to the entry which represents the target archive file in its
 * parent file system and resolves archive entry collisions by performing a
 * full update of the target archive file.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
final class FsTargetArchiveController<E extends FsArchiveEntry>
extends FsFileSystemArchiveController<E>
implements FsArchiveFileSystem.TouchListener {

    private static final BitField<FsInputOption>
            MOUNT_INPUT_OPTIONS = BitField.of(FsInputOption.CACHE);

    private final FsArchiveDriver<E> driver;
   
    /** The parent file system controller. */
    private final FsController<?> parent;

    /** The entry name of the target archive file in the parent file system. */
    private final FsEntryName name;

    /**
     * An {@link InputArchive} object used to mount the (virtual) archive file system
     * and read the entries from the archive file.
     */
    private InputArchive<E> inputArchive;

    /**
     * The (possibly temporary) {@link OutputArchive} we are writing newly
     * created or modified entries to.
     */
    private OutputArchive<E> outputArchive;

    /**
     * Constructs a new default archive file system controller.
     *
     * @param model the file system model.
     * @param parent the parent file system
     * @param driver the archive driver.
     */
    FsTargetArchiveController(
            final FsArchiveDriver<E> driver,
            final FsLockModel model,
            final FsController<? extends FsModel> parent) {
        super(model);
        if (null == driver)
            throw new NullPointerException();
        if (model.getParent() != parent.getModel())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.driver = driver;
        this.parent = parent;
        this.name = getMountPoint().getPath().getEntryName();
        assert invariants();
    }

    private boolean invariants() {
        assert null != driver;
        assert null != parent;
        assert null != name;
        final FsArchiveFileSystem<E> fs = getFileSystem();
        final InputArchive<E> ia = inputArchive;
        final OutputArchive<E> oa = outputArchive;
        assert null == ia || null != fs : "null != ia => null != fs";
        assert null == oa || null != fs : "null != oa => null != fs";
        assert null == fs || null != ia || null != oa : "null != fs => null != ia || null != oa";
        // This is effectively the same than the last three assertions, but is
        // harder to trace in the field on failure.
        //assert null != fs == (null != ia || null != oa);
        return true;
    }

    InputArchive<E> getInputArchive()
    throws FsNeedsSyncException {
        final InputArchive<E> ia = inputArchive;
        if (null != ia && ia.isClosed()) throw FsNeedsSyncException.get();
        return ia;
    }

    private void setInputArchive(final InputArchive<E> ia) {
        assert null == ia || null == this.inputArchive;
        this.inputArchive = ia;
        if (null != ia) setMounted(true);
    }

    OutputArchive<E> getOutputArchive()
    throws FsNeedsSyncException {
        final OutputArchive<E> oa = outputArchive;
        if (null != oa && oa.isClosed()) throw FsNeedsSyncException.get();
        return oa;
    }

    private void setOutputArchive(final OutputArchive<E> oa) {
        assert null == oa || null == this.outputArchive;
        this.outputArchive = oa;
        if (null != oa) setMounted(true);
    }

    @Override
    public void preTouch() throws IOException {
        makeOutputArchive();
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Override
    void mount(final boolean autoCreate) throws IOException {
        try {
            mount0(autoCreate);
        } finally {
            assert invariants();
        }
    }

    private void mount0(final boolean autoCreate) throws IOException {
        // HC SVNT DRACONES!
       
        // Check parent file system entry.
        final FsEntry pe; // parent entry
        try {
            pe = parent.getEntry(name);
        } catch (final FsFalsePositiveArchiveException ex) {
            throw new AssertionError(ex);
        } catch (final IOException inaccessibleEntry) {
            if (autoCreate) throw inaccessibleEntry;
            throw new FsFalsePositiveArchiveException(inaccessibleEntry);
        }

        // Obtain file system by creating or loading it from the parent entry.
        final FsArchiveFileSystem<E> fs;
        if (null == pe) {
            if (autoCreate) {
                // This may fail e.g. if the container file is an RAES
                // encrypted ZIP file and the user cancels password prompting.
                makeOutputArchive();
                fs = newEmptyFileSystem(driver);
            } else {
                throw new FsFalsePositiveArchiveException(
                        new FsEntryNotFoundException(parent.getModel(),
                            name, "no such entry"));
            }
        } else {
            try {
                // readOnly must be set first because the parent archive controller
                // could be a FileController and on Windows this property changes
                // to TRUE once a file is opened for reading!
                final boolean ro = !parent.isWritable(name);
                final InputSocket<?> is = driver.getInputSocket(
                        parent, name, MOUNT_INPUT_OPTIONS);
                final InputArchive<E> ia = new InputArchive<E>(
                        driver.newInputShop(getModel(), is));
                // TODO: Remove try-catch
                try {
                    fs = newPopulatedFileSystem(driver, ia.getArchive(), pe, ro);
                } catch (final IOException ex) {
                    ia.close();
                    throw ex;
                }
                setInputArchive(ia);
                assert isMounted();
            } catch (final FsFalsePositiveArchiveException ex) {
                throw new AssertionError(ex);
            } catch (final IOException ex) {
                if (ex instanceof CancelledOperation
                        || ex instanceof AuthenticationFailedOperation) {
                    throw new IOException(ex);
                }
                Throwable cause = ex.getCause();
                if (cause != null && (cause instanceof CancelledOperation
                        || cause instanceof AuthenticationFailedOperation)) {
                    throw ex;
                }
                throw pe.isType(SPECIAL)
                        ? new FsFalsePositiveArchiveException(ex)
                        : new FsPersistentFalsePositiveArchiveException(ex);
            }
        }

        // Register file system.
        fs.setTouchListener(this);
        setFileSystem(fs);
    }

    /**
     * Ensures that {@link #getOutputArchive} does not return {@code null}.
     * This method will use
     * <code>{@link #getContext()}.{@link FsOperationContext#getOutputOptions()}</code>
     * to obtain the output options to use for writing the entry in the parent
     * file system.
     *
     * @return The output archive.
     */
    OutputArchive<E> makeOutputArchive() throws IOException {
        OutputArchive<E> oa = getOutputArchive();
        if (null != oa) {
            assert isMounted();
            return oa;
        }
        final BitField<FsOutputOption> options = getContext()
                .getOutputOptions()
                .and(OUTPUT_PREFERENCES_MASK)
                .set(CACHE);
        final OutputSocket<?> os = driver.getOutputSocket(
                parent, name, options, null);
        final InputArchive<E> ia = getInputArchive();
        try {
            oa = new OutputArchive<E>(driver.newOutputShop(
                    getModel(), os, null == ia ? null : ia.getArchive()));
        } catch (final FsFalsePositiveArchiveException ex) {
            throw new AssertionError(ex);
        } catch (final ControlFlowException ex) {
            assert ex instanceof FsNeedsLockRetryException : ex;
            throw ex;
        }
        setOutputArchive(oa);
        assert isMounted();
        return oa;
    }

    @Override
    InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends ClutchInputSocket<E> {
            @Override
            protected InputSocket<? extends E> getLazyDelegate()
            throws IOException {
                return getInputArchive().getInputSocket(name);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                try {
                    return getBoundSocket().newReadOnlyFile();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public InputStream newInputStream() throws IOException {
                try {
                    return getBoundSocket().newInputStream();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            ControlFlowException map(InputClosedException ex) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get();
            }
        } // Input

        return new Input();
    }

    @Override
    OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends ClutchOutputSocket<E> {
            @Override
            protected OutputSocket<? extends E> getLazyDelegate()
            throws IOException {
                return makeOutputArchive().getOutputSocket(entry);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (OutputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                try {
                    return getBoundSocket().newOutputStream();
                } catch (OutputClosedException ex) {
                    throw map(ex);
                }
            }

            ControlFlowException map(OutputClosedException ex) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get();
            }
        } // Output

        return new Output();
    }

    @Override
    void checkSync(   final FsEntryName name,
                        final Access intention)
    throws FsNeedsSyncException {
        // HC SVNT DRACONES!

        // If no file system exists, then pass the test.
        final FsArchiveFileSystem<E> fs = getFileSystem();
        if (null == fs) return;

        // If GROWing and the driver supports the respective access method,
        // then pass the test.
        if (getContext().get(GROW)) {
            if (null == intention) {
                if (driver.getRedundantMetaDataSupport()) return;
            } else if (WRITE == intention) {
                if (driver.getRedundantContentSupport()) {
                    getOutputArchive();
                    return;
                }
            }
        }

        // If the file system does not contain an entry with the given name,
        // then pass the test.
        final FsCovariantEntry<E> fse = fs.getEntry(name);
        if (null == fse) return;

        // If the entry name addresses the file system root, then pass the test
        // because the root entry cannot get input or output anyway.
        if (name.isRoot()) return;

        String aen; // archive entry name

        // Check if the entry is already written to the output archive.
        {
            final OutputArchive<E> oa = getOutputArchive();
            if (null != oa) {
                aen = fse.getEntry().getName();
                if (null != oa.getEntry(aen)) throw FsNeedsSyncException.get();
            } else {
                aen = null;
            }
        }

        // If our intention is not reading the entry then pass the test.
        if (READ != intention) return;

        // Check if the entry is present in the input archive.
        final E iae; // input archive entry
        {
            final InputArchive<E> ia = getInputArchive();
            if (null != ia) {
                if (null == aen) aen = fse.getEntry().getName();
                iae = ia.getEntry(aen);
            } else {
                iae = null;
            }
        }
        if (null == iae) throw FsNeedsSyncException.get();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        assert isWriteLockedByCurrentThread();
        try {
            final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
            if (!options.get(ABORT_CHANGES))
                copy(builder);
            close(options, builder);
            builder.check();
        } finally {
            assert invariants();
        }
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the
     * (temporary) output archive file.
     *
     * @param handler the strategy for assembling sync exceptions.
     */
    private void copy(final FsSyncExceptionBuilder handler)
    throws FsSyncException {
        // Skip (In|Out)putArchive for better performance.
        // This is safe because the FsResourceController has already shut down
        // all concurrent access by closing the respective resources (streams,
        // channels etc).
        // The Disconnecting(In|Out)putShop should not get skipped however:
        // If these would throw an (In|Out)putClosedException, then this would
        // be an artifact of a bug.
        final OutputService<E> os;
        {
            final OutputArchive<E> oa = outputArchive;
            if (null == oa || oa.isClosed())
                return;
            assert !oa.isClosed();
            os = oa.getClutch();
        }

        final InputService<E> is;
        {
            final InputArchive<E> ia = inputArchive;
            if (null != ia && ia.isClosed())
                return;
            assert null == ia || !ia.isClosed();
            is = null != ia  ? ia.getClutch() : new DummyInputService<E>();
        }

        IOException warning = null;
        for (final FsCovariantEntry<E> fse : getFileSystem()) {
            for (final E ae : fse.getEntries()) {
                final String aen = ae.getName();
                if (null != os.getEntry(aen))
                    continue; // entry has already been output
                try {
                    if (DIRECTORY == ae.getType()) {
                        if (!fse.isRoot()) // never output the root directory!
                            if (UNKNOWN != ae.getTime(Access.WRITE)) // never write a ghost directory!
                                os.getOutputSocket(ae).newOutputStream().close();
                    } else if (null != is.getEntry(aen)) {
                        IOSocket.copy(  is.getInputSocket(aen),
                                        os.getOutputSocket(ae));
                    } else {
                        // The file system entry is a newly created
                        // non-directory entry which hasn't received any
                        // content yet, e.g. as a result of mknod()
                        // => output an empty file system entry.
                        for (final Size size : ALL_SIZE_SET)
                            ae.setSize(size, UNKNOWN);
                        ae.setSize(DATA, 0);
                        os.getOutputSocket(ae).newOutputStream().close();
                    }
                } catch (final IOException ex) {
                    if (null != warning || !(ex instanceof InputException))
                        throw handler.fail(new FsSyncException(getModel(), ex));
                    warning = ex;
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        }
    }

    /**
     * Discards the file system, closes the input archive and finally the
     * output archive.
     * Note that this order is critical: The parent file system controller is
     * expected to replace the entry for the target archive file with the
     * output archive when it gets closed, so this must be done last.
     * Using a finally block ensures that this is done even in the unlikely
     * event of an exception when closing the input archive.
     * Note that in this case closing the output archive is likely to fail and
     * override the IOException thrown by this method, too.
     *
   * @param handler the strategy for assembling sync exceptions.
     */
    private void close(
            final BitField<FsSyncOption> options,
            final FsSyncExceptionBuilder handler) {
        // HC SVNT DRACONES!
        final InputArchive<E> ia = inputArchive;
        if (null != ia) {
            try {
                ia.close();
            } catch (final ControlFlowException ex) {
                assert ex instanceof FsNeedsLockRetryException;
                throw ex;
            } catch (final IOException ex) {
                handler.warn(new FsSyncWarningException(getModel(), ex));
            }
            setInputArchive(null);
        }
        final OutputArchive<E> oa = outputArchive;
        if (null != oa) {
            try {
                oa.close();
            } catch (final ControlFlowException ex) {
                assert ex instanceof FsNeedsLockRetryException;
                throw ex;
            } catch (final IOException ex2) {
                handler.warn(new FsSyncException(getModel(), ex2));
            }
            setOutputArchive(null);
        }
        setFileSystem(null);
        if (options.get(ABORT_CHANGES)) setMounted(false);
    }

    /**
     * A dummy input archive to substitute for {@code null} when copying.
     *
     * @param <E> The type of the entries.
     */
    private static final class DummyInputService<E extends Entry>
    implements InputService<E> {
        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.<E>emptyList().iterator();
        }

        @Override
        public E getEntry(String name) {
            return null;
        }

        @Override
        public InputSocket<? extends E> getInputSocket(String name) {
            throw new UnsupportedOperationException();
        }
    } // DummyInputService

    private static final class InputArchive<E extends FsArchiveEntry>
    extends LockInputShop<E> {
        final InputShop<E> archive;

        InputArchive(final InputShop<E> input) {
            super(new DisconnectingInputShop<E>(input));
            this.archive = input;
        }

        boolean isClosed() {
            return getClutch().isClosed();
        }

        DisconnectingInputShop<E> getClutch() {
            return (DisconnectingInputShop<E>) delegate;
        }

        /**
         * Publishes the product of the archive driver this input archive is
         * decorating.
         */
        InputShop<E> getArchive() {
            assert !isClosed();
            return archive;
        }
    } // InputArchive

    private static final class OutputArchive<E extends FsArchiveEntry>
    extends LockOutputShop<E> {
        OutputArchive(final OutputShop<E> output) {
            super(new DisconnectingOutputShop<E>(output));
        }

        boolean isClosed() {
            return getClutch().isClosed();
        }

        DisconnectingOutputShop<E> getClutch() {
            return (DisconnectingOutputShop<E>) delegate;
        }
    } // OutputArchive
}
