/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.zip.DateTimeConverter;
import de.schlichtherle.truezip.zip.ZipEntry;

/**
 * JAR archive entries apply the date/time conversion rules as defined by
 * {@link DateTimeConverter#JAR}.
 *
 * @see    #getDateTimeConverter()
 * @see    JarDriver
 * @author Christian Schlichtherle
 */
public class JarDriverEntry extends ZipDriverEntry {

    public JarDriverEntry(String name) {
        super(name);
    }

    protected JarDriverEntry(String name, ZipEntry template) {
        super(name, template);
    }

    /**
     * Returns a {@link DateTimeConverter} for the conversion of Java time
     * to DOS date/time fields and vice versa.
     * <p>
     * The implementation in the class {@link ZipDriverEntry} returns
     * {@link DateTimeConverter#JAR}.
     *
     * @return {@link DateTimeConverter#JAR}
     */
    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }
}
