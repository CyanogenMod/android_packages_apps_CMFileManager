/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.READ;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.fs.FsOutputOption.APPEND;
import static de.schlichtherle.truezip.fs.FsOutputOption.CREATE_PARENTS;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.DelegatingInputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract base class for any archive file system controller which
 * provide all the essential services required for accessing a prospective
 * archive file.
 * This base class encapsulates all the code which is not depending on a
 * particular archive update strategy and the corresponding state of this
 * file system controller.
 * <p>
 * Each instance of this class manages an archive file - the <i>target file</i>
 * - in order to allow random access to it as if it were a regular directory in
 * its parent file system.
 * <p>
 * Note that in general all of the methods in this class are reentrant on
 * exceptions.
 * This is important because client applications may repeatedly call them.
 * Of course, depending on the calling context, some or all of the archive
 * file's data may be lost in this case.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
abstract class FsBasicArchiveController<E extends FsArchiveEntry>
extends FsLockModelController {

    private static final Logger logger = Logger.getLogger(FsBasicArchiveController.class.getName());

    private final ThreadLocal<FsOperationContext>
            context = new ThreadLocal<FsOperationContext>();

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    FsBasicArchiveController(final FsLockModel model) {
        super(model);
        if (null == model.getParent())
            throw new IllegalArgumentException();
    }

    /**
     * Returns a context with the original values of selected parameters for
     * the {@linkplain FsContextController file system controller}
     * operation in progress.
     * <p>
     * Note that this is a thread-local property!
     *
     * @return A context with the original values of selected parameters for
     *         the {@linkplain FsContextController file system controller}
     *         operation in progress.
     */
    final FsOperationContext getContext() {
        return context.get();
    }

    /**
     * Sets the JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in progress.
     * This method should only get called by the class
     * {@link FsContextController}.
     * <p>
     * Note that this is a thread-local property!
     *
     * @param context the JavaBean which represents the original values of
     *        selected parameters for the {@link FsContextController}
     *        operation in progress.
     * @see   #getContext()
     */
    final void setContext(final FsOperationContext context) {
        if (null != context)
            this.context.set(context);
        else
            this.context.remove();
    }

    /** Equivalent to {@link #autoMount(boolean) autoMount(false)}. */
    final FsArchiveFileSystem<E> autoMount() throws IOException {
        return autoMount(false);
    }

    /**
     * Returns the (virtual) archive file system mounted from the target
     * archive file. This method is reentrant with respect to any exceptions
     * it may throw.
     *
     * @param  autoCreate If this is {@code true} and the archive file does not
     *         exist, then a new archive file system with only a (virtual)
     *         root directory is created with its last modification time set
     *         to the system's current time.
     * @return An archive file system.
     */
    abstract FsArchiveFileSystem<E> autoMount(boolean autoCreate)
    throws IOException;

    @Override
    public final boolean isReadOnly() throws IOException {
        return autoMount().isReadOnly();
    }

    @Override
    public final FsEntry getEntry(FsEntryName name)
    throws IOException {
        return autoMount().getEntry(name);
    }

    @Override
    public final boolean isReadable(FsEntryName name) throws IOException {
        return autoMount().getEntry(name) != null;
    }

    @Override
    public final boolean isWritable(FsEntryName name) throws IOException {
        return autoMount().isWritable(name);
    }

    @Override
    public final void setReadOnly(FsEntryName name) throws IOException {
        autoMount().setReadOnly(name);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    Map<Access, Long> times,
                                    BitField<FsOutputOption> options)
    throws IOException {
        checkSync(name, null);
        return autoMount().setTime(name, times);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    BitField<Access> types,
                                    long value,
                                    BitField<FsOutputOption> options)
    throws IOException {
        checkSync(name, null);
        return autoMount().setTime(name, types, value);
    }

    @Override
    public final InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        if (null == name) throw new NullPointerException();

        final class Input extends DelegatingInputSocket<FsArchiveEntry> {
            @Override
            public FsArchiveEntry getLocalTarget() throws IOException {
                checkSync(name, READ);
                final FsCovariantEntry<E> ce = autoMount().getEntry(name);
                if (null == ce)
                    throw new FsEntryNotFoundException(getModel(),
                            name, "no such entry");
                final FsArchiveEntry ae = ce.get(FILE);
                if (null == ae)
                    throw new FsEntryNotFoundException(getModel(),
                            name, "expected FILE entry, but is a " + ce.getTypes() + " entry");
                return ae;
            }

            @Override
            protected InputSocket<? extends FsArchiveEntry> getDelegate()
            throws IOException {
                getPeerTarget(); // may sync() if in same target archive file!
                return getInputSocket(getLocalTarget().getName());
            }
        } // Input

        return new Input();
    }

    abstract InputSocket<? extends E> getInputSocket(String name);

    @Override
    public final OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        if (null == name) throw new NullPointerException();
        if (null == options) throw new NullPointerException();

        final class Output extends OutputSocket<FsArchiveEntry> {
            @Override
            public FsArchiveEntry getLocalTarget() throws IOException {
                final E ae = mknod().getTarget().getEntry();
                if (options.get(APPEND)) {
                    // A proxy entry must get returned here in order to inhibit
                    // a peer target to recognize the type of this entry and
                    // change the contents of the transferred data accordingly.
                    // This would not work when APPENDing.
                    return new ProxyEntry(ae);
                }
                return ae;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final FsArchiveFileSystemOperation<E> mknod = mknod();
                final E ae = mknod.getTarget().getEntry();
                InputStream in = null;
                if (options.get(APPEND)) {
                    try {
                        in = getInputSocket(name, FsInputOptions.NONE).newInputStream();
                    } catch (IOException ex) {
                        // When appending, there is no need for the entry to exist,
                        // so we can safely ignore this - fall through!
                    }
                }
                try {
                    final OutputSocket<? extends E> os = getOutputSocket(ae);
                    if (null == in) // do NOT bind when appending!
                        os.bind(this);
                    final OutputStream out = os.newOutputStream();
                    try {
                        mknod.commit();
                        if (in != null)
                            Streams.cat(in, out);
                    } catch (IOException ex) {
                        out.close(); // may throw another exception!
                        throw ex;
                    }
                    return out;
                } finally {
                    if (null != in) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            throw new InputException(ex);
                        }
                    }
                }
            }

            FsArchiveFileSystemOperation<E> mknod() throws IOException {
                checkSync(name, WRITE);
                // Start creating or overwriting the archive entry.
                // This will fail if the entry already exists as a directory.
                return autoMount(!name.isRoot() && options.get(CREATE_PARENTS))
                        .mknod(name, FILE, options, template);
            }
        } // Output

        return new Output();
    }

    private static final class ProxyEntry
    extends DecoratingEntry<FsArchiveEntry>
    implements FsArchiveEntry {
        ProxyEntry(FsArchiveEntry entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return delegate.getType();
        }

        @Override
        public boolean setSize(Size type, long value) {
            return delegate.setSize(type, value);
        }

        @Override
        public boolean setTime(Access type, long value) {
            return delegate.setTime(type, value);
        }
    } // ProxyEntry

    abstract OutputSocket<? extends E> getOutputSocket(E entry);

    @Override
    public final void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        if (name.isRoot()) { // TODO: Is this case differentiation required?
            try {
                autoMount(); // detect false positives!
            } catch (final FsFalsePositiveArchiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true);
                return;
            }
            throw new FsEntryNotFoundException(getModel(),
                    name, "directory entry exists already");
        } else {
            checkSync(name, null);
            autoMount(options.get(CREATE_PARENTS))
                    .mknod(name, type, options, template)
                    .commit();
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        checkSync(name, null);
        final FsArchiveFileSystem<E> fs = autoMount();
        fs.unlink(name);
        if (name.isRoot()) {
            // Check for any archive entries with absolute entry names.
            final int size = fs.getSize() - 1; // mind the ROOT entry
            if (0 != size)
                logger.log(Level.WARNING,
                        "Deleting a file system with {1} absolute entries which is otherwise empty: {0}",
                        new Object[] { getMountPoint(), size });
        }
    }

    /**
     * Checks if the intended access to the named archive entry in the virtual
     * file system is possible without performing a
     * {@link FsController#sync(BitField) sync} operation in advance.
     *
     * @param  name the file system entry name.
     * @param  intention the intended I/O operation on the archive entry.
     *         If {@code null}, then only an update to the archive entry meta
     *         data (i.e. a pure virtual file system operation with no I/O)
     *         is intended.
     * @throws FsNeedsSyncException If a sync operation is required before the
     *         intended access could succeed.
     */
    abstract void checkSync(FsEntryName name, Access intention)
    throws FsNeedsSyncException;
}
