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

package com.cyanogenmod.filemanager.commands.shell;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.commands.WriteExecutable;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for write data to disk.<br/>
 * <br/>
 * User MUST call the {@link #createOutputStream()} to get the output stream where
 * write the data.<br/>. When no more exist then user MUST call the onEnd method
 * of the asynchronous listener.<br/>
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?dd"}
 */
public class WriteCommand extends AsyncResultProgram implements WriteExecutable {

    private static final String ID = "write";  //$NON-NLS-1$

    private static final long TIMEOUT = 1000L;

    /**
     * @hide
     */
    final Object mWriteSync = new Object();
    private boolean mReady;
    /**
     * @hide
     */
    boolean mError;

    /**
     * Constructor of <code>WriteCommand</code>.
     *
     * @param file The file where to write the data
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public WriteCommand(
            String file, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, file);
        this.mReady = false;
        this.mError = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExpectEnd() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream createOutputStream() throws IOException {

        // Wait until command is ready
        synchronized (this.mWriteSync) {
            if (!this.mReady) {
                try {
                    this.mWriteSync.wait(TIMEOUT);
                } catch (Exception e) {/**NON BLOCK**/}
            }
        }
        return getProgramListener().getOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        synchronized (this.mWriteSync) {
            this.mReady = true;
            this.mWriteSync.notify();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndParsePartialResult(boolean cancelled) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public SIGNAL onRequestEnd() {
        try {
            if (this.getProgramListener().getOutputStream() != null) {
                this.getProgramListener().getOutputStream().flush();
            }
        } catch (Exception ex) {/**NON BLOCK**/}
        try {
            Thread.yield();
        } catch (Exception ex) {/**NON BLOCK**/}
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParsePartialResult(final byte[] partialIn) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParseErrorPartialResult(byte[] partialErr) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnoreShellStdErrCheck() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {
        //Ignore exit code 143 (cancelled)
        //Ignore exit code 137 (kill -9)
        if (exitCode != 0 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 && != 143 && != 137"); //$NON-NLS-1$
        }
    }

}
