/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

/**
 * Defines the common properties of a file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @see    FsController
 * @see    FsManager
 * @author Christian Schlichtherle
 */
public abstract class FsModel {

    private final FsMountPoint mountPoint;
    private final FsModel parent;

    protected FsModel(
            final FsMountPoint mountPoint,
            final FsModel parent) {
        if (!equals(mountPoint.getParent(),
                    (null == parent ? null : parent.getMountPoint())))
            throw new IllegalArgumentException("Parent/Member mismatch!");
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    private static boolean equals(Object o1, Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    /**
     * Returns the mount point of the file system.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system meta data which is stored outside the file system,
     * e.g. in-memory stored passwords for RAES encrypted ZIP files.
     *
     * @return The mount point of the file system.
     */
    public final FsMountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if the file system is not federated, i.e. if it's not a member of
     * a parent file system.
     *
     * @return The nullable parent file system model.
     */
    public final FsModel getParent() {
        return parent;
    }

    /**
     * Returns {@code true} if and only if some state associated with the
     * federated file system has been modified so that the
     * corresponding {@link FsController} must not get discarded until
     * the next call to {@link FsController#sync sync}.
     * <p>
     * An implementation may always return {@code false} if the associated
     * file system controller is stateless.
     *
     * @return {@code true} if and only if some state associated with the
     *         federated file system has been modified so that the
     *         corresponding {@link FsController} must not get discarded until
     *         the next {@link FsController#sync sync}.
     */
    public abstract boolean isMounted();

    /**
     * Sets the value of the property {@link #isMounted() mounted}.
     * Only file system controllers should call this method in order to
     * register themselves for a call their {@link FsController#sync} method.
     * <p>
     * An implementation may ignore calls to this method if the associated
     * file system controller is stateless.
     *
     * @param mounted the new value of this property.
     */
    public abstract void setMounted(boolean mounted);

    /**
     * Two file system models are considered equal if and only if they are
     * identical.
     * This can't get overriden.
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
        return String.format("%s[mountPoint=%s, parent=%s, mounted=%b]",
                getClass().getName(),
                getMountPoint(),
                getParent(),
                isMounted());
    }
}
