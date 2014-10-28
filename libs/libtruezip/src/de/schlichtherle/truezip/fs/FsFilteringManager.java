/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.entry.EntryName.SEPARATOR_CHAR;
import de.schlichtherle.truezip.util.FilteringIterator;
import java.net.URI;
import java.util.Iterator;

/**
 * Filters the list of federated file systems managed by the decorated file
 * system manager so that their mount point starts with the prefix provided
 * to the {@link #FsFilteringManager constructor}.
 *
 * @author Christian Schlichtherle
 */
public final class FsFilteringManager extends FsDecoratingManager<FsManager> {

    private final URI prefix;

    /**
     * Constructs a new prefix filter file system manager from the given file
     * system manager and mount point prefix.
     *
     * @param manager the decorated file system manager.
     * @param prefix the prefix of the mount point used to filter all federated
     *        file systems of the decorated file system manager.
     */
    public FsFilteringManager(
            final FsManager manager,
            final FsMountPoint prefix) {
        super(manager);
        this.prefix = prefix.toHierarchicalUri();
    }

    @Override
    public int getSize() {
        int size = 0;
        for (@SuppressWarnings("unused") FsController<?> controller : this)
            size++;
        return size;
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return new FilteredControllerIterator();
    }

    private final class FilteredControllerIterator
    extends FilteringIterator<FsController<?>> {
        final String ps = prefix.getScheme();
        final String pp = prefix.getPath();
        final int ppl = pp.length();
        final boolean pps = SEPARATOR_CHAR == pp.charAt(ppl - 1);

        FilteredControllerIterator() {
            super(delegate.iterator());
        }

        @Override
        protected boolean accept(final FsController<?> controller) {
            assert null != controller : "null elements are not allowed in this collection!";
            final URI mp = controller.getModel().getMountPoint().toHierarchicalUri();
            final String mpp;
            return mp.getScheme().equals(ps)
                    && (mpp = mp.getPath()).startsWith(pp)
                    && (pps
                        || mpp.length() == ppl
                        || SEPARATOR_CHAR == mpp.charAt(ppl));
        }
    }
}
