/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Adds a start value to the given offset.
 *
 * @author  Christian Schlichtherle
 */
final class OffsetPositionMapper extends PositionMapper {
    final long start;

    OffsetPositionMapper(final long start) {
        this.start = start;
    }

    @Override
    long map(long offset) {
        return offset + start;
    }

    @Override
    long unmap(long offset) {
        return offset - start;
    }
}