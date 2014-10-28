/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.socket.IOPool;
import java.io.File;
import java.io.IOException;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 *
 * @author Christian Schlichtherle
 */
final class TempFilePool implements IOPool<FileEntry> {

    /**
     * A default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    static final TempFilePool INSTANCE = new TempFilePool(null, null);

    private final File dir;
    private final String prefix;

    TempFilePool(
            final File dir,
            final String prefix) {
        this.dir = dir;
        this.prefix = null != prefix ? prefixPlusDot(prefix) : "tzp";
    }

    private static String prefixPlusDot(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    @Override
    public Buffer allocate() throws IOException {
        return new Buffer(createTempFile(), this);
    }

    private File createTempFile() throws IOException {
        try {
            return File.createTempFile(prefix, null, dir);
        } catch (final IOException ex) {
            if (dir.exists()) throw ex;
            createTempDir();
            return createTempFile();
        }
    }

    private void createTempDir() {
        assert !dir.exists();
        if (!dir.mkdirs() && !dir.exists()) {
            // Must NOT map to IOException - see
            // https://java.net/jira/browse/TRUEZIP-321 .
            throw new IllegalArgumentException(dir + " (cannot create directory for temporary files)");
        }
        assert dir.exists();
    }

    @Override
    public void release(Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    private static final class Buffer
    extends FileEntry
    implements Entry<FileEntry> {

        Buffer(File file, final TempFilePool pool) {
            super(file);
            assert null != file;
            assert null != pool;
            this.pool = pool;
        }

        @Override public void release() throws IOException { pool(null); }

        private void pool(final TempFilePool newPool)
        throws IOException {
            final TempFilePool oldPool = this.pool;
            this.pool = newPool;
            if (oldPool != newPool) {
                final File file = getFile();
                if (!file.delete() && file.exists())
                    throw new IOException(file + " (cannot delete temporary file)");
            }
        }

        @Override
        protected void finalize() throws Throwable {
            try { pool(null); }
            finally { super.finalize(); }
        }
    } // Buffer
}
