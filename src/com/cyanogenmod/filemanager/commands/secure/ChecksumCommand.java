/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.commands.secure;

import android.util.Log;

import com.android.internal.util.HexDump;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ChecksumExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * A class for calculate MD5 and SHA-1 checksums of a file system object.<br />
 * <br />
 * Partial results are returned in order (MD5 -> SHA1)
 */
public class ChecksumCommand extends Program implements ChecksumExecutable {

    private static final String TAG = "ChecksumCommand"; //$NON-NLS-1$

    private final File mSrc;
    private final String[] mChecksums;
    private final AsyncResultListener mAsyncResultListener;

    private boolean mCancelled;
    private final Object mSync = new Object();

    /**
     * Constructor of <code>ChecksumCommand</code>.
     *
     * @param console The current console
     * @param src The source file
     * @param asyncResultListener The partial result listener
     */
    public ChecksumCommand(SecureConsole console, String src,
            AsyncResultListener asyncResultListener) {
        super(console);
        this.mAsyncResultListener = asyncResultListener;
        this.mChecksums = new String[]{null, null};
        this.mSrc = new File(src);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsynchronous() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {

        if (isTrace()) {
            Log.v(TAG,
                    String.format("Calculating checksums of file %s", this.mSrc)); //$NON-NLS-1$
        }

        // Check that the file exists
        TFile f = getConsole().buildRealFile(this.mSrc.getAbsolutePath());
        if (!f.exists()) {
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL. NoSuchFileOrDirectory"); //$NON-NLS-1$
            }
            throw new NoSuchFileOrDirectory(this.mSrc.getAbsolutePath());
        }

        CHECKSUMS checksum = CHECKSUMS.MD5;
        try {
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onAsyncStart();
            }

            // Calculate digests
            calculateDigest(checksum, f);
            checksum = CHECKSUMS.SHA1;
            calculateDigest(checksum, f);

            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onAsyncEnd(false);
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onAsyncExitCode(0);
            }

            if (isTrace()) {
                Log.v(TAG, "Result: OK"); //$NON-NLS-1$
            }

        } catch (InterruptedException ie) {
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onAsyncEnd(true);
            }
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onAsyncExitCode(143);
            }

            if (isTrace()) {
                Log.v(TAG, "Result: CANCELLED"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            Log.e(TAG,
                    String.format(
                            "Fail to calculate %s checksum of file %s", //$NON-NLS-1$
                            checksum.name(),
                            this.mSrc.getAbsolutePath()),
                    e);
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onException(e);
            }
            if (isTrace()) {
                Log.v(TAG, "Result: FAIL"); //$NON-NLS-1$
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        synchronized (this.mSync) {
            return this.mCancelled;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel() {
        try {
            synchronized (this.mSync) {
                this.mCancelled = true;
            }
        } catch (Throwable _throw) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean end() {
        return cancel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnEndListener(OnEndListener onEndListener) {
        //Ignore. Java console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        //Ignore. Java console don't use this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getResult() {
        return this.mChecksums;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getChecksum(CHECKSUMS checksum) {
        return getResult()[checksum.ordinal()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncResultListener getAsyncResultListener() {
        return this.mAsyncResultListener;
    }

    /**
     * Method that calculate a digest of the file for the source file
     *
     * @param type The type of digest to obtain
     * @pa
     * @throws InterruptedException If the operation was cancelled
     * @throws Exception If an error occurs
     */
    private void calculateDigest(CHECKSUMS type, TFile file)
            throws InterruptedException, Exception {

        InputStream is = null;
        try {
            MessageDigest md = MessageDigest.getInstance(type.name());
            is = new TFileInputStream(file);

            // Start digesting
            byte[] data = new byte[getBufferSize()];
            int read = 0;
            while ((read = is.read(data, 0, getBufferSize())) != -1) {
                checkCancelled();
                md.update(data, 0, read);
            }
            checkCancelled();

            // Finally digest
            this.mChecksums[type.ordinal()] =
                    HexDump.toHexString(md.digest()).toLowerCase(Locale.ROOT);
            checkCancelled();
            if (this.mAsyncResultListener != null) {
                this.mAsyncResultListener.onPartialResult(this.mChecksums[type.ordinal()]);
            }

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * Checks if the operation was cancelled
     *
     * @throws InterruptedException If the operation was cancelled
     */
    private void checkCancelled() throws InterruptedException {
        synchronized (this.mSync) {
            if (this.mCancelled) {
                throw new InterruptedException();
            }
        }
    }
}
