/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import java.io.CharConversionException;
import java.io.IOException;

/**
 * An abstract factory for components required for accessing archive files.
 * Implementations of this abstract base class are used to access specific
 * archive formats like ZIP, JAR, TZP, TAR, TAR.GZ, TAR.BZ2 etc.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriver<E extends FsArchiveEntry>
extends FsDriver {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} always returns
     * {@code true}.
     */
    @Override
    public final boolean isFederated() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given file system manager after asserting that
     * {@code model.getParent().equals(parent.getModel())} is {@code true}.
     */
    @Override
    public final FsController<? extends FsModel> newController(
            final FsManager manager,
            final FsModel model,
            final FsController<? extends FsModel> parent) {
        assert parent.getModel().equals(model.getParent());
        return manager.newController(this, model, parent);
    }

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and parent file system controller.
     * <p>
     * When called, you may assert the following precondition:
     * {@code model.getParent().equals(parent.getModel())}
     * Note that the parent file system controller of an archive file system is
     * never {@code null}.
     *
     * @param  model the file system model.
     * @param  parent the non-null parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    @Override
    public FsController<?> newController(
            final FsModel model,
            final FsController<?> parent) {
        assert model.getParent().equals(parent.getModel());
        assert !(model instanceof FsLockModel);
        // HC SVNT DRACONES!
        return  new FsLockController(
                    new FsSyncController(
                        new FsCacheController(getPool(),
                            new FsResourceController(
                                new FsContextController(
                                    new FsTargetArchiveController<E>(
                                        this, new FsLockModel(model), parent))))));
    }

    /**
     * Returns the I/O buffer pool to use for allocating temporary I/O buffers.
     * <p>
     * Multiple invocations should return the same I/O buffer pool.
     * However, callers should cache the return value for subsequent use in
     * case it isn't always the same.
     *
     * @return The I/O buffer pool to use for allocating temporary I/O buffers.
     */
    protected abstract IOPool<?> getPool();

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry contents.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry contents, but only the last contents written
     * should get used when reading the archive file.
     *
     * @return The implementation in the class {@link FsArchiveDriver} returns
     *         {@code false} for backwards compatibility.
     * @since  TrueZIP 7.3
     */
    public boolean getRedundantContentSupport() {
        return false;
    }

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry meta data.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry meta data, but only the last meta data written
     * should get used when reading the archive file.
     * This usually implies the existence of a central directory in the
     * resulting archive file.
     *
     * @return The implementation in the class {@link FsArchiveDriver} returns
     *         {@code false} for backwards compatibility.
     * @since  TrueZIP 7.3
     */
    public boolean getRedundantMetaDataSupport() {
        return false;
    }

    /**
     * Called to prepare reading an archive file artifact of this driver from
     * {@code name} in {@code controller} using {@code options}.
     * <p>
     * This method may get overridden in order to modify the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     *
     * @param  controller the controller to use for reading an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @return An input socket for reading an artifact of this driver.
     * @since  TrueZIP 7.1
     */
    public InputSocket<?> getInputSocket(   FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsInputOption> options) {
        return controller.getInputSocket(name, options);
    }

    /**
     * Creates a new input shop for reading the archive entries for the
     * given {@code model} from the given {@code input} socket's target.
     * <p>
     * Note that the returned input shop does <em>not</em> need to be
     * thread-safe.
     *
     * @param  model the file system model.
     * @param  input the input socket for reading the contents of the
     *         archive file from its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getInputSocket} method.
     * @return A new input shop.
     * @throws IOException on any I/O error.
     *         If the file system entry for the given model exists in the
     *         parent file system and is <em>not</em> a {@link Type#SPECIAL}
     *         type, then this exception is deemed to indicate a
     *         <em>persistent false positive</em> archive file and gets cached
     *         until the file system controller for the given model is
     *         {@linkplain FsController#sync(de.schlichtherle.truezip.util.BitField) synced}
     *         again.
     *         Otherwise, this exception is deemed to indicate a
     *         <em>transient false positive</em> archive file and does not
     *         get cached.
     */
    public abstract InputShop<E>
    newInputShop(   FsModel model,
                    InputSocket<?> input)
    throws IOException;

    /**
     * Called to prepare writing an archive file artifact of this driver to
     * the entry {@code name} in {@code controller} using {@code options} and
     * the nullable {@code template}.
     * <p>
     * This method may get overridden in order to modify the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     *
     * @param  controller the controller to use for writing an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @param  template the template to use.
     * @return An output socket for writing an artifact of this driver.
     * @since  TrueZIP 7.1
     */
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return controller.getOutputSocket(name, options, template);
    }

    /**
     * Creates a new output shop for writing archive entries for the
     * given {@code model} to the given {@code output} socket's target.
     * <p>
     * Note that the returned output shop does <em>not</em> need to be
     * thread-safe.
     *
     * @param  model the file system model.
     * @param  output the output socket for writing the contents of the
     *         archive file to its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getOutputSocket} method.
     * @param  source the {@link InputShop} if {@code archive} is going to get
     *         updated.
     *         If not {@code null}, this is guaranteed to be the product
     *         of this driver's {@link #newInputShop} factory method.
     *         This feature could get used to copy some meta data which is
     *         specific to the type of archive this driver supports,
     *         e.g. the comment of a ZIP file.
     * @return A new output shop.
     * @throws IOException on any I/O error.
     */
    public abstract OutputShop<E>
    newOutputShop(  FsModel model,
                    OutputSocket<?> output,
                    InputShop<E> source)
    throws IOException;

    /**
     * Equivalent to {@link #newEntry(java.lang.String, de.schlichtherle.truezip.entry.Entry.Type, de.schlichtherle.truezip.entry.Entry, de.schlichtherle.truezip.util.BitField)
     * newEntry(name, type, template, FsOutputOptions.NONE)}.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     */
    public final E newEntry(String name, Type type, Entry template)
    throws CharConversionException {
        return newEntry(name, type, template, FsOutputOptions.NONE);
    }

    /**
     * Returns a new entry for the given name.
     * The implementation may need to fix this name in order to
     * form a valid {@link Entry#getName() entry name} for their
     * particular requirements.
     * <p>
     * If {@code template} is not {@code null}, then the returned entry shall
     * inherit as much properties from this template as possible - with the
     * exception of its name and type.
     * Furthermore, if {@code name} and {@code type} are equal to the name and
     * type of this template, then the returned entry shall be a (deep) clone
     * of the template which shares no mutable state with the template.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @param  options when called from {@link FsController#mknod}, this is its
     *         {@code options} parameter, otherwise it's typically an empty set.
     * @return A new entry for the given name.
     * @throws CharConversionException TODO: This has been deprecated and
     *         should get removed.
     */
    public abstract E newEntry(
            String name,
            Type type,
            Entry template,
            BitField<FsOutputOption> options)
    throws CharConversionException;

    /**
     * Ensures that the given entry name can get encoded by this driver's
     * character set.
     * <p>
     * The implementation in the class {@link FsArchiveDriver} is just a stub
     * and does nothing.
     *
     * @param  name an entry name.
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     */
    // TODO: Consider naming this checkEncodable because of throws declaration.
    protected void assertEncodable(String name) throws CharConversionException {
    }
}
