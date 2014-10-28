/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * A factory for {@link ZipEntry}s.
 *
 * @param   <E> The type of the created ZIP entries.
 * @author  Christian Schlichtherle
 */
public interface ZipEntryFactory<E extends ZipEntry> extends ZipParameters {

    /**
     * Returns a new ZIP entry with the given {@code name}.
     *
     * @param  name the entry name.
     * @return A new ZIP entry with the given {@code name}.
     */
    E newEntry(String name);
}