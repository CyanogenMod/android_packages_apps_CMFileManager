/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.util.BitField;
import java.util.Formatter;
import java.util.Set;

/**
 * An abstract file system entry is an entry which can implement multiple types
 * and list directory members.
 *
 * @author  Christian Schlichtherle
 */
public abstract class FsEntry implements Entry {

    /**
     * Returns a string representation of the
     * {@link FsEntryName file system entry name}.
     *
     * @return A string representation of the
     *         {@link FsEntryName file system entry name}.
     */
    @Override
    public abstract String getName();

    /**
     * Returns a set of types implemented by this entry.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     * <p>
     * Some file system types allow an entry to implement multiple entry types.
     * For example, a ZIP or TAR file may contain a file entry with the name
     * {@code foo} and a directory entry with the name {@code foo/}.
     * Yes, this is strange, but shit happens!
     * In this case then, a virtual file system should collapse this into one
     * file system entry which returns {@code true} for both
     * {@code isType(FILE)} and {@code isType(DIRECTORY)}.
     *
     * @return An unmodifiable set of types implemented by this entry.
     */
    public abstract Set<Type> getTypes();

    /**
     * Returns {@code true} if and only if this file system entry implements
     * the given type.
     *
     * @param  type the type to test.
     * @return {@code true} if and only if this file system entry implements
     *         the given type.
     * @see    #getTypes()
     */
    public boolean isType(Type type) {
        return getTypes().contains(type);
    }

    /**
     * Returns a set of strings with the base names of the members of this
     * directory entry or {@code null} if and only if this is not a directory
     * entry.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     *
     * @return A set of strings with the base names of the members of this
     *         directory entry or {@code null} if and only if this is not a
     *         directory entry.
     */
    public abstract Set<String> getMembers();

    /**
     * Two file system entries are considered equal if and only if they are
     * identical.
     * This can't get overriden.
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
        // Cannot make copy of empty BitField, so we need to work around this.
        final Set<Type> typesSet = getTypes();
        final BitField<Type> typesBitField = typesSet.isEmpty()
                ? BitField.noneOf(Type.class)
                : BitField.copyOf(typesSet);

        final StringBuilder s = new StringBuilder(256);
        final Formatter f = new Formatter(s);
        try {
            f.format("%s[name=%s, types=%s",
                getClass().getName(), getName(), typesBitField);
            for (Size type : ALL_SIZE_SET) {
                final long size = getSize(type);
                if (UNKNOWN != size)
                    f.format(", size(%s)=%d", type, size);
            }
            for (Access type : ALL_ACCESS_SET) {
                final long time = getTime(type);
                if (UNKNOWN != time)
                    f.format(", time(%s)=%tc", type, time);
            }
            return f.format(", members=%s]", getMembers()).toString();
        } finally {
            f.close();
        }
    }
}
