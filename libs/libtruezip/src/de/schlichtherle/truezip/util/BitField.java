/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * A type-safe, immutable set of enums which emulates the concept of a bit
 * field, i.e. a set of predefined bits.
 * As an immutable class, it's inherently thread-safe.
 * All modifying methods return a modified clone of this instance.
 *
 * @param   <E> The type of {@link Enum} objects contained in this set.
 * @author  Christian Schlichtherle
 */
// TODO: Add more modifying methods.
public final class BitField<E extends Enum<E>>
implements Iterable<E>, Serializable {

    private static final long serialVersionUID = 3203876204846746524L;

    private final EnumSet<E> bits;

    /**
     * Returns a bit field which can contain the given element type and is
     * initially empty.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.noneOf(Option.class);
     * }</pre>
     * where {@code Option} is an arbitrary enum type.
     */
    public static <E extends Enum<E>> BitField<E>
    noneOf(Class<E> elementType) {
        return new BitField<E>(elementType, false);
    }

    /**
     * Returns a bit field which contains all enums of the given element type.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.allOf(Option.class);
     * }</pre>
     * where {@code Option} is an arbitrary enum type.
     */
    public static <E extends Enum<E>> BitField<E>
    allOf(Class<E> elementType) {
        return new BitField<E>(elementType, true);
    }

    /**
     * Returns a bit field which contains the given bit.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(Option.ONE);
     * }</pre>
     * where {@code Option.ONE} is an arbitrary enum.
     */
    public static <E extends Enum<E>> BitField<E>
    of(E bit) {
        return new BitField<E>(bit);
    }

    /**
     * Returns a bit field which contains the given bits.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(Option.ONE, Option.TWO);
     * }</pre>
     * where {@code Option.ONE} and {@code Option.TWO} are arbitrary enums.
     */
    @SafeVarargs
    public static <E extends Enum<E>> BitField<E>
    of(E bit, E... bits) {
        return new BitField<E>(bit, bits);
    }

    /**
     * Returns a bit field which contains the same bits as the given collection
     * of enums.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(bits);
     * }</pre>
     * where {@code bits} is an {@code EnumSet<Option>}.
     */
    public static <E extends Enum<E>> BitField<E>
    copyOf(Collection<E> bits) {
        return new BitField<E>(EnumSet.copyOf(bits));
    }

    public static <E extends Enum<E>> BitField<E>
    of(final Class<E> elementType, final String list) {
        final EnumSet<E> bits = EnumSet.noneOf(elementType);
        for (final String bit : list.split("\\|"))
            bits.add(Enum.valueOf(elementType, bit));
        return new BitField<E>(bits);
    }

    /**
     * Constructs a new bit field which contains all or none of the enums
     * of the given element type.
     */
    private BitField(Class<E> elementType, boolean allOf) {
        this.bits = allOf   ? EnumSet.allOf (elementType)
                            : EnumSet.noneOf(elementType);
    }

    /** Constructs a new bit field which contains the given bit. */
    private BitField(E bit) {
        this.bits = EnumSet.of(bit);
    }

    /** Constructs a new bit field which contains the given bits. */
    @SafeVarargs
    private BitField(E bit, E... bits) {
        this.bits = EnumSet.of(bit, bits);
    }

    /**
     * Constructs a new bit field by sharing the given set of enums.
     * Note that this constructor does <em>not</em> make a protective copy
     * - use with care!
     *
     * @param bits the set of enums to share with this instance.
     */
    private BitField(final EnumSet<E> bits) {
        assert null != bits;
        this.bits = bits;
    }

    /**
     * Returns {@code true} if and only if all bits are cleared in this bit
     * field.
     */
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    /** Returns the number of bits set in this field. */
    public int cardinality() {
        return bits.size();
    }

    /**
     * Returns {@code true} if and only if the given bit is set.
     *
     * @param bit The bit to test.
     */
    public boolean get(E bit) {
        return bits.contains(bit);
    }

    /** Equivalent to {@link #get(Enum) get(bit)}. */
    public boolean is(E bit) {
        return bits.contains(bit);
    }

    /**
     * Sets or clears the given bit.
     * <p>
     * Subclasses could override this method in order to cache frequently used
     * results, such as a null bit field, a single bit field etc.
     *
     * @param bit The bit to set or clear.
     * @param set Whether the bit shall get set or cleared.
     */
    public BitField<E> set(final E bit, final boolean set) {
        final EnumSet<E> bits;
        if (set) {
            if (this.bits.contains(bit))
                return this;
            bits = this.bits.clone();
            bits.add(bit);
        } else {
            if (!this.bits.contains(bit))
                return this;
            bits = this.bits.clone();
            bits.remove(bit);
        }
        return new BitField<E>(bits);
    }

    /** Sets the given bit. */
    public BitField<E> set(E bit) {
        return set(bit, true);
    }

    /** Clears the given bit. */
    public BitField<E> clear(E bit) {
        return set(bit, false);
    }

    public BitField<E> not() {
        return new BitField<E>(EnumSet.complementOf(bits));
    }

    public BitField<E> and(BitField<E> that) {
        final EnumSet<E> bits = this.bits.clone();
        return bits.retainAll(that.bits) ? new BitField<E>(bits) : this;
    }

    public BitField<E> or(BitField<E> that) {
        final EnumSet<E> bits = this.bits.clone();
        return bits.addAll(that.bits) ? new BitField<E>(bits) : this;
    }

    /** Returns a read-only iterator for the bits in this field. */
    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableSet(bits).iterator();
    }

    /**
     * Returns a new set of enums containing the same bits as this instance.
     * The following boolean identity expression is always true for any
     * non-{@code null} bit field {@code bits}:
     * {@code bits.equals(BitField.copyOf(bits.toEnumSet()))}.
     *
     * @return a new set of enums containing the same bits as this instance.
     */
    public EnumSet<E> toEnumSet() {
        return bits.clone();
    }

    /**
     * Returns a concatenation of the names of the bits in this field,
     * separated by {@code "|"}.
     */
    @Override
    public String toString() {
        final int capacity = bits.size() * 11;
        if (0 >= capacity)
            return "";
        final StringBuilder s = new StringBuilder(capacity);
        for (final E bit : bits) {
            if (s.length() > 0)
                s.append('|');
            s.append(bit);
        }
        return s.toString();
    }

    /**
     * Returns {@code true} if and only if the given object is another
     * {@code BitField} and contains the same bits.
     */
    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof BitField<?>
                    && bits.equals(((BitField<?>) that).bits);
    }

    /** Returns a hash code which is consistent with {@link #equals}. */
    @Override
    public int hashCode() {
        return bits.hashCode();
    }
}