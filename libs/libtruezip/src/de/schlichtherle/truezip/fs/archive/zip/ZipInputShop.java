/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.zip.RawZipFile;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input shop for reading ZIP files.
 *
 * @see    ZipOutputShop
 * @author Christian Schlichtherle
 */
public class ZipInputShop
extends RawZipFile<ZipDriverEntry>
implements InputShop<ZipDriverEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private boolean appendee;
    private ZipCryptoParameters param;

    public ZipInputShop(
            final ZipDriver driver,
            final FsModel model,
            final ReadOnlyFile rof)
    throws IOException {
        super(rof, driver);
        this.driver = driver;
        if (null == (this.model = model)) {
            final NullPointerException ex = new NullPointerException();
            try {
                super.close();
            } catch (final Throwable ex2) {
                // Ignore
            }
            throw ex;
        }
    }

    /**
     * Returns the file system model provided to the constructor.
     *
     * @return The file system model provided to the constructor.
     * @since  TrueZIP 7.3
     */
    public FsModel getModel() {
        return model;
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        ZipCryptoParameters param = this.param;
        if (null == param)
            this.param = param = driver.zipCryptoParameters(this);
        return param;
    }

    /**
     * Returns {@code true} if and only if the target archive file gets entries
     * appended to it.
     * Note that the implementation in the class {@link ZipInputShop} does not
     * use this property.
     *
     * @return {@code true} if and only if the target archive file gets entries
     *         appended to it.
     */
    protected boolean isAppendee() {
        return appendee;
    }

    /**
     * Indicates whether or not the target archive file gets entries appended
     * to it.
     *
     * @param appendee {@code true} if and only if the target archive file gets
     *        entries appended to it.
     */
    final void setAppendee(boolean appendee) {
        this.appendee = appendee;
    }

    @Override
    public int getSize() {
        return super.size();
    }

    @Override
    public InputSocket<ZipDriverEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends InputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry getLocalTarget() throws IOException {
                final ZipDriverEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                if (entry.isDirectory())
                    throw new FileNotFoundException(name + " (cannot read directory entries)");
                return entry;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                throw new UnsupportedOperationException(); // TODO: Support this feature for STORED entries.
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final ZipDriverEntry local = getLocalTarget();
                final Entry peer = getPeerTarget();
                final ZipDriverEntry zpeer = peer instanceof ZipDriverEntry
                        ? (ZipDriverEntry) peer
                        : null;
                final ZipDriver driver = ZipInputShop.this.driver;
                return getInputStream(
                        local.getName(),
                        driver.check(ZipInputShop.this, local),
                        null == zpeer
                        || 0 == local.getSize()
                        || driver.process(ZipInputShop.this, local, zpeer));
            }
        } // Input

        return new Input();
    }
}
