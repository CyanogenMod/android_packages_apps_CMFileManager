/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

/**
 * An abstract factory for components required to access a file system.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @see    FsCompositeDriver
 * @author Christian Schlichtherle
 */
public abstract class FsDriver {

    /**
     * Returns {@code true} iff this file system driver implements a federated
     * file system type, i.e. if the type of file system must be a member of a
     * parent file system.
     * <p>
     * The implementation in the class {@link FsDriver} returns {@code false}.
     *
     * @return {@code true} iff the type of the file system implemented by this
     *         file system driver is federated, i.e. must be a member of a
     *         parent file system.
     */
    public boolean isFederated() {
        return false;
    }

    /**
     * Returns a priority to help the file system driver service locator.
     * The greater number wins!
     *
     * @return {@code 0}, as by the implementation in the class
     *         {@link FsDriver}.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and nullable parent file system controller.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     * <p>
     * The implementation in the class {@link FsDriver} simply forwards the
     * call to {@link #newController(FsModel, FsController)}.
     *
     * @param  manager the file system manager for the new controller.
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and nullable parent file system controller.
     * @see    FsCompositeDriver#newController
     * @since  TrueZIP 7.6
     */
    public FsController<? extends FsModel> newController(
            FsManager manager,
            FsModel model,
            FsController<? extends FsModel> parent) {
        return newController(model, parent);
    }

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and nullable parent file system controller.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     *
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and nullable parent file system controller.
     * @see    FsCompositeDriver#newController
     */
    public abstract FsController<?>
    newController(FsModel model, FsController<?> parent);

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[federated=%b, priority=%d]",
                getClass().getName(),
                isFederated(),
                getPriority());
    }
}
