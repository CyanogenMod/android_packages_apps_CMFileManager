/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * An abstract set of the canonical string representation of objects in
 * natural sort order.
 * An object is canonicalized by the idempotent function
 * {@link Canonicalizer#map}.
 * <p>
 * Canonical string sets can be converted from and to string lists by using
 * {@link #addAll(String)} and {@link #toString()}.
 * A <i>string list</i> is a string which consists of zero or more elements
 * which are separated by the <i>separator character</i> provided to the
 * constructor.
 * Note that in general, a string list is just a sequence of strings elements.
 * In particular, a string list may be empty (but not {@code null}) and
 * its elements don't have to be in canonical form, may be duplicated in the
 * list and may be listed in arbitrary order.
 * However, string lists have a canonical form, too:
 * A string list in canonical form (or <i>canonical string list</i> for short)
 * is a string list which contains only canonical strings in natural sort order
 * and does not contain any duplicates (so it's actually a set).
 * <p>
 * Unless otherwise documented, all {@link java.util.Set} methods work on the
 * canonical form of the string elements in this set.
 * <p>
 * Null elements are not permitted in this set.
 *
 * @author Christian Schlichtherle
 */
public class CanonicalStringSet extends AbstractSet<String> {

    /**
     * An idempotent function which maps an arbitrary object to its canonical
     * string representation.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Idempotence>Idempotence</a>
     */
    public interface Canonicalizer {
        /**
         * Returns the canonical string representation of {@code o}.
         * This method is expected to be an idempotent function, i.e. it shall
         * have no side effects and the result of calling the function for its
         * result again at least compares {@link Object#equals} to its initial
         * result.
         * E.g. the method {@link Object#toString} is a simple implementation
         * of this method because you could call it several times on its result:
         * The first call results in a string and each subsequent call would
         * return the same string again.
         *
         * @param  o The Object to map to its canonical string representation.
         * @return The canonical string representation of {@code o}.
         */
        String map(Object o);
    } // Canonicalizer

    /** The canonicalizer for strings. */
    private final Canonicalizer canonicalizer;

    /** The separator for string lists. */
    private final char separator;

    /** The sorted map which implements the behaviour of this class. */
    private final Set<String> set = new TreeSet<String>();

    /**
     * Constructs a new, empty set of canonical strings.
     *
     * @param canonicalizer the idempotent function to use for canonicalizing
     *        strings.
     * @param separator The separator character to use for string lists.
     */
    public CanonicalStringSet(  final Canonicalizer canonicalizer,
                                final char separator) {
        if (null == canonicalizer)
            throw new NullPointerException();
        this.canonicalizer = canonicalizer;
        this.separator = separator;
    }

    @Override
    public boolean isEmpty() { return set.isEmpty(); }

    @Override
    public int size() { return set.size(); }

    @Override
    public Iterator<String> iterator() { return set.iterator(); }

    @Override
    public Object[] toArray() { return set.toArray(); }

    @Override
    public <T> T[] toArray(T[] array) { return set.toArray(array); }

    /**
     * Returns the string list in canonical form for this canonical string set.
     * If this canonical string set is empty, an empty string is returned.
     */
    @Override
    public String toString() {
        final int capacity = size() * 11;
        if (0 >= capacity)
            return "";
        final StringBuilder s = new StringBuilder(capacity);
        for (final String string : this) {
            if (s.length() > 0)
                s.append(separator);
            s.append(string);
        }
        return s.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override public boolean contains(Object o) {
        return set.contains(canonicalizer.map(o));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override
    public boolean add(String s) { return set.add(canonicalizer.map(s)); }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override
    public boolean remove(Object o) { return set.remove(canonicalizer.map(o)); }

    @Override public void clear() { set.clear(); }

    /**
     * Tests if all canonical strings in the given set are contained in this
     * set.
     * An empty set is considered to be a true subset of this set.
     *
     * @param set A set of canonical strings.
     * @return {@code true} Iff all strings in the given set are contained
     *         in this set.
     */
    public boolean containsAll(final CanonicalStringSet set) {
        return this.set.containsAll(set.set);
    }

    /**
     * Tests if the canonical form of all strings in the given string list
     * is contained in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * This implies that if the list is empty or entirely consists of strings
     * which do not have a canonical form, {@code true} is returned.
     * In other words, an empty set is considered to be a true subset of this
     * set.
     *
     * @param  list a string list.
     * @return {@code true} Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     */
    public boolean containsAll(final String list) {
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            if (!set.contains(i.next()))
                return false;
        return true;
    }

    /**
     * Adds all canonical strings in the given set to this set after they have
     * been canonicalized by this set again.
     *
     * @param  set a set of canonical strings.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     */
    public boolean addAll(final CanonicalStringSet set) {
        boolean changed = false;
        for (String s : set.set)
            changed |= add(s);
        return changed;
    }

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a string list.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     */
    public boolean addAll(final String list) {
        boolean changed = false;
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            changed |= set.add(i.next());
        return changed;
    }

    /**
     * Retains all canonical strings in the given set in this set.
     *
     * @param  set a set of canonical strings.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean retainAll(CanonicalStringSet set) {
        return this.set.retainAll(set.set);
    }

    /**
     * Retains the canonical form of all strings in the given list in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a string list.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean retainAll(final String list) {
        final CanonicalStringSet
                set = new CanonicalStringSet(canonicalizer, separator);
        set.addAll(list);
        return this.set.retainAll(set);
    }

    /**
     * Removes all canonical strings in the given set from this set.
     *
     * @param  set a set of strings.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean removeAll(CanonicalStringSet set) {
        return this.set.removeAll(set.set);
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a string list.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean removeAll(final String list) {
        boolean changed = false;
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            changed |= set.remove(i.next());
        return changed;
    }

    private class CanonicalStringIterator implements Iterator<String> {
        private final StringTokenizer tokenizer;
        private String canonical;

        private CanonicalStringIterator(final String list) {
            tokenizer = new StringTokenizer(list, "" + separator); // NOI18N
            advance();
        }

        private void advance() {
            while (tokenizer.hasMoreTokens())
                if (null != (canonical = canonicalizer.map(tokenizer.nextToken())))
                    return;
            canonical = null; // no such element
        }

        @Override
        public boolean hasNext() { return null != canonical; }

        @Override
        public String next() {
            final String canonical = this.canonical;
            if (null == canonical) throw new NoSuchElementException();
            advance();
            return canonical;
        }

        @Override
        public void remove() { throw new UnsupportedOperationException(); }
    } // CanonicalStringIterator
}
