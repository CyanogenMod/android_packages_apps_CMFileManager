/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

/**
 * Static utility methods for hash maps.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
public class HashMaps {

    /**
     * The number of entries which should be additionally accomodatable by a
     * hash map with a load factor of 75% before resizing it, which is {@value}.
     * When a new hash map gets created, this constant should get used in order
     * to compute the initial capacity or overhead for additional entries.
     *
     * @see   #initialCapacity(int)
     */
    public static final int OVERHEAD_SIZE = (64 - 1) * 3 / 4; // consider 75% load factor

    /* Can't touch this - hammer time! */
    private HashMaps() { }

    /**
     * Returns the initial capacity for a hash table with a load factor of 75%.
     *
     * @param  size the number of entries to accommodate space for.
     * @return The initial capacity for a hash table with a load factor of 75%.
     * @see    #OVERHEAD_SIZE
     */
    public static int initialCapacity(int size) {
        if (size < OVERHEAD_SIZE)
            size = OVERHEAD_SIZE;
        final long capacity = size * 4L / 3 + 1;
        return Integer.MAX_VALUE >= capacity
                ? powerOfTwo((int) capacity)
                : Integer.MAX_VALUE; // not very realistic
    }

    // "Borrowed" from Scala's HashTable.
    private static int powerOfTwo(int c) {
      /* See http://bits.stephan-brumme.com/roundUpToNextPowerOfTwo.html */
      c--;
      c |= c >>>  1;
      c |= c >>>  2;
      c |= c >>>  4;
      c |= c >>>  8;
      c |= c >>> 16;
      return c + 1;
    }
}
