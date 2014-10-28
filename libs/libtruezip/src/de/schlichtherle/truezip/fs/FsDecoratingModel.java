/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

/**
 * An abstract decorator for a file system model.
 *
 * @param  <M> the type of the decorated file system model.
 * @author Christian Schlichtherle
 */
public abstract class FsDecoratingModel<M extends FsModel> extends FsModel {

    /** The decorated file system model. */
    protected final M delegate;

    /**
     * Constructs a new decorating file system model.
     *
     * @param delegate the file system model to decorate.
     */
    protected FsDecoratingModel(final M delegate) {
        super(delegate.getMountPoint(), delegate.getParent());
        this.delegate = delegate;
    }

    @Override
    public boolean isMounted() {
        return delegate.isMounted();
    }

    @Override
    public void setMounted(boolean touched) {
        delegate.setMounted(touched);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
