/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 *
 * @param  <M> the type of the file system model.
 * @since  TrueZIP 7.6
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractController<M extends FsModel>
extends FsController<M>  {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     *
     * @param model the file system model.
     */
    protected FsAbstractController(final M model) {
        if (null == (this.model = model))
            throw new NullPointerException();
    }

    @Override
    public final M getModel() {
        return model;
    }

    /**
     * Returns the mount point of this (virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     *
     * @return The mount point of this (virtual) file system as
     *         defined by the {@linkplain #getModel() model}.
     */
    public final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    /**
     * Returns the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     *
     * @return the {@code mounted} property of the
     *         {@linkplain #getModel() file system model}.
     */
    public final boolean isMounted() {
        return model.isMounted();
    }

    /**
     * Sets the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     *
     * @param mounted the {@code mounted} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final void setMounted(boolean mounted) {
        model.setMounted(mounted);
    }
}
