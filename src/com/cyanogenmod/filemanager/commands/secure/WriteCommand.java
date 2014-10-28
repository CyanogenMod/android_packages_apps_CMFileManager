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

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.WriteExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for write data to disk.<br/>
 * <br/>
 * User MUST call the {@link #createOutputStream()} to get the output stream where
 * write the data.<br/>. When no more exist then user MUST call the onEnd method
 * of the asynchronous listener.<br/>
 */
public class WriteCommand extends Program implements WriteExecutable {

    private static final String TAG = "WriteCommand"; //$NON-NLS-1$

    private final String mFile;
    private BufferedOutputStream mBuffer;
    private final AsyncResultListener mAsyncResultListener;

    private boolean mCancelled;
    private final Object mSync = new Object();

    private static final long TIMEOUT = 1000L;

    private final Object mWriteSync = new Object();
    private boolean mReady;

    /**
     * Constructor of <code>WriteCommand</code>.
     *
     * @param console The current console
     * @param file The file where to write the data
     * @param asyncResultListener The partial result listener
     */
    public WriteCommand(SecureConsole console, String file,
            AsyncResultListener asyncResultListener) {
        super(console);
        this.mFile = file;
        this.mAsyncResultListener = asyncResultListener;
        this.mCancelled = false;
        this.mReady = false;
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
    public OutputStream createOutputStream() throws IOException {
        try {
            // Wait until command is ready
            synchronized (this.mWriteSync) {
                if (!this.mReady) {
                    try {
                        this.mWriteSync.wait(TIMEOUT);
                    } catch (Exception e) {/**NON BLOCK**/}
                }
            }
            TFile f = getConsole().buildRealFile(mFile);
            this.mBuffer = new BufferedOutputStream(new TFileOutputStream(f), getBufferSize());
            return this.mBuffer;
        } catch (IOException ioEx) {
            if (isTrace()) {
                Log.e(TAG, "Result: FAILED. IOException", ioEx); //$NON-NLS-1$
            }
            throw ioEx;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws NoSuchFileOrDirectory, ExecutionException {
        synchronized (this.mSync) {
            this.mReady = true;
            this.mSync.notify();
        }

        if (isTrace()) {
            Log.v(TAG,
                    String.format("Writing file %s", this.mFile)); //$NON-NLS-1$

        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncStart();
        }

        // Wait the finalization
        try {
            synchronized (this.mSync) {
                this.mSync.wait();
            }
        } catch (Throwable _throw) {/**NON BLOCK**/}

        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncEnd(this.mCancelled);
        }
        if (this.mAsyncResultListener != null) {
            this.mAsyncResultListener.onAsyncExitCode(0);
        }

        if (isTrace()) {
            Log.v(TAG, "Result: OK"); //$NON-NLS-1$
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
        closeBuffer();
        this.mCancelled = true;
        try {
            synchronized (this.mSync) {
                this.mSync.notify();
            }
        } catch (Throwable _throw) {/**NON BLOCK**/}
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean end() {
        closeBuffer();
        try {
            synchronized (this.mSync) {
                this.mSync.notify();
            }
        } catch (Throwable _throw) {/**NON BLOCK**/}
        return true;
    }

    /**
     * Method that close the buffer
     */
    private void closeBuffer() {
        try {
            if (this.mBuffer != null) {
                this.mBuffer.close();
            }
        } catch (Exception ex) {/**NON BLOCK**/}
        try {
            Thread.yield();
        } catch (Exception ex) {/**NON BLOCK**/}
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
}
