/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Maps a given offset to a file pointer position.
 *
 * @author Christian Schlichtherle
 */
class PositionMapper {
    long map(long offset) {
        return offset;
    }

    long unmap(long offset) {
        return offset;
    }
}