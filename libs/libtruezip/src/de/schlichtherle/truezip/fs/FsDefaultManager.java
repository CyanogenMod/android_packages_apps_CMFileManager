/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Link.Type;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;
import static de.schlichtherle.truezip.util.Links.getTarget;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
public final class FsDefaultManager extends FsManager {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     */
    private final Map<FsMountPoint, Link<FsController<?>>> controllers
            = new WeakHashMap<FsMountPoint, Link<FsController<?>>>();

    private final Type optionalScheduleType;

    private final ReadLock readLock;
    private final WriteLock writeLock;

    public FsDefaultManager() {
        this(WEAK);
    }

    /** Solely provided for unit testing. */
    FsDefaultManager(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public <E extends FsArchiveEntry> FsController<?> newController(
            final FsArchiveDriver<E> driver,
            final FsModel model,
            final FsController<?> parent) {
        assert !(model instanceof FsLockModel);
        // HC SVNT DRACONES!
        return new FsFalsePositiveArchiveController(
                    new FsFinalizeController<FsModel>(
                        driver.newController(model, parent)));
    }

    @Override
    public FsController<?> getController(
            final FsMountPoint mp,
            final FsCompositeDriver d) {
        try {
            readLock.lock();
            try {
                return getController0(mp, d);
            } finally {
                readLock.unlock();
            }
        } catch (final FsNeedsWriteLockException ex) {
            writeLock.lock();
            try {
                return getController0(mp, d);
            } finally {
                writeLock.unlock();
            }
        }
    }

    private FsController<?> getController0(
            final FsMountPoint mp,
            final FsCompositeDriver d) {
        FsController<?> c = getTarget(controllers.get(mp));
        if (null != c) return c;
        if (!writeLock.isHeldByCurrentThread())
            throw FsNeedsWriteLockException.get();
        final FsMountPoint pmp = mp.getParent();
        final FsController<?> p = null == pmp ? null : getController0(pmp, d);
        final ManagedModel m = new ManagedModel(mp, null == p ? null : p.getModel());
        c = d.newController(this, m, p);
        m.init(c);
        return c;
    }

    /**
     * A model which schedules its controller for
     * {@link #sync(BitField) synchronization} by &quot;observing&quot; its
     * {@code touched} property.
     * Extending its sub-class to register for updates to the {@code touched}
     * property is simpler, faster and requires a smaller memory footprint than
     * the alternative observer pattern.
     */
    private final class ManagedModel extends FsModel {
        FsController<?> controller;
        volatile boolean mounted;

        ManagedModel(FsMountPoint mountPoint, FsModel parent) {
            super(mountPoint, parent);
        }

        void init(final FsController<? extends FsModel> controller) {
            assert null != controller;
            assert !mounted;
            this.controller = controller;
            schedule(false);
        }

        @Override
        public boolean isMounted() {
            return mounted;
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given mount status.
         */
        @Override
        public void setMounted(final boolean mounted) {
            writeLock.lock();
            try {
                if (this.mounted != mounted) {
                    if (mounted)
                        FsSyncShutdownHook.register(FsDefaultManager.this);
                    schedule(mounted);
                    this.mounted = mounted;
                }
            } finally {
                writeLock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        void schedule(boolean mandatory) {
            assert(writeLock.isHeldByCurrentThread());
            final Type type = mandatory ? STRONG : optionalScheduleType;
            final Object link = type.newLink(controller);
            controllers.put(getMountPoint(), (Link<FsController<?>>) link);
        }
    } // ManagedModel

    @Override
    public int getSize() {
        readLock.lock();
        try {
            return controllers.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return sortedControllers().iterator();
    }

    private Set<FsController<?>> sortedControllers() {
        readLock.lock();
        try {
            final Set<FsController<?>> snapshot
                    = new TreeSet<FsController<?>>(ReverseControllerComparator.INSTANCE);
            for (final Link<FsController<? extends FsModel>> link : controllers.values()) {
                final FsController<?> controller = getTarget(link);
                if (null != controller)
                    snapshot.add(controller);
            }
            return snapshot;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        FsSyncShutdownHook.cancel();
        super.sync(options);
    }

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final class ReverseControllerComparator
    implements Comparator<FsController<?>> {
        static final ReverseControllerComparator INSTANCE
                = new ReverseControllerComparator();

        @Override
        public int compare(FsController<?> o1, FsController<?> o2) {
            return o2.getModel().getMountPoint().toHierarchicalUri()
                    .compareTo(o1.getModel().getMountPoint().toHierarchicalUri());
        }
    } // ReverseControllerComparator
}
