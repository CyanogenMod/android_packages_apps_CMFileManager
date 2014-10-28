/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsSyncOption.ABORT_CHANGES;
import de.schlichtherle.truezip.util.BitField;
import java.util.Iterator;

/**
 * A container which creates {@linkplain FsController} file system controllers
 * and manages their life cycle.
 * <p>
 * Sub-classes should be thread-safe, too.
 *
 * @see    FsController
 * @see    FsModel
 * @author Christian Schlichtherle
 */
public abstract class FsManager implements Iterable<FsController<?>> {

    /**
     * <em>Optional:</em>
     * Returns a new thread-safe archive file system controller.
     * This is a pure function without side effects.
     *
     * @param  <E> the type of the archive entries.
     * @param  driver the archive driver.
     * @param  model the file system model.
     * @param  parent the parent file system controller.
     * @return A new archive file system controller.
     * @throws UnsupportedOperationException if this operation is not supported.
     */
    public <E extends FsArchiveEntry> FsController<?> newController(
            FsArchiveDriver<E> driver,
            FsModel model,
            FsController<?> parent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the thread-safe file system controller for the given mount point.
     * The life cycle of the returned file system controller gets managed by
     * this manager, i.e. it gets remembered for future lookup and
     * {@link #sync synchronization}.
     *
     * @param  mountPoint the mount point of the file system.
     * @param  driver the composite file system driver which shall get used to
     *         create a new file system controller if required.
     * @return The thread-safe file system controller for the given mount point.
     */
    public abstract FsController<?> getController(
            FsMountPoint mountPoint,
            FsCompositeDriver driver);

    /**
     * Returns the number of managed file system controllers.
     *
     * @return The number of managed file system controllers.
     */
    // TODO: Rename this to size().
    public abstract int getSize();

    /**
     * Returns an ordered iterator for the managed file system controllers.
     * The iterated file system controllers are ordered so that all file
     * systems appear before any of their parent file systems.
     * Last, but not least: The iterator must be consistent in multithreaded
     * environments!
     *
     * @return An ordered iterator for the managed file system controllers.
     */
    @Override
    public abstract Iterator<FsController<?>> iterator();

    /**
     * Calls {@link FsController#sync(BitField)} on all managed file system
     * controllers.
     * If sync()ing a file system controller fails with an
     * {@link FsSyncException}, then the exception gets remembered and the loop
     * continues with sync()ing the remaining file system controllers.
     * After the loop, the exception(s) get processed for (re)throwing based
     * on their type and order of appearance.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@link FsSyncOption#ABORT_CHANGES}
     *         is set.
     */
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        if (options.get(ABORT_CHANGES)) throw new IllegalArgumentException();
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        for (final FsController<?> controller : this) {
            try {
                controller.sync(options);
            } catch (final FsSyncException ex) {
                builder.warn(ex);
            }
        }
        builder.check();
    }

    /**
     * Two file system managers are considered equal if and only if they are
     * identical. This can't get overriden.
     *
     * @param that the object to compare.
     */
    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[size=%d]",
                getClass().getName(),
                getSize());
    }
}
