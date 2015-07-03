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
import com.cyanogenmod.filemanager.commands.ExecExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;

/**
 * A class for execute a command
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?sh"}
 */
public class ExecCommand extends AsyncResultProgram implements ExecExecutable {

    private static final String ID = "exec"; //$NON-NLS-1$

    private int mExitCode;

    /**
     * Constructor of <code>ExecCommand</code>.
     *
     * @param cmd The absolute directory to compute
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public ExecCommand(
            String cmd, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, new String[]{cmd});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        this.mExitCode = 0;
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
    public void onParsePartialResult(byte[] in) {
        String partialIn = new String(in);
        //If a listener is defined, then send the partial result
        if (partialIn != null && partialIn.length() > 0) {
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(partialIn);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParseErrorPartialResult(byte[] in) {
        String partialErr = new String(in);
        //If a listener is defined, then send the partial result
        if (partialErr != null && partialErr.length() > 0) {
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(partialErr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SIGNAL onRequestEnd() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExitCode() {
        return this.mExitCode;
    }

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
        this.mExitCode = exitCode;
    }
}
