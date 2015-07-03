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
import com.cyanogenmod.filemanager.commands.ReadExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;

/**
 * A class for read a file
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?cat"}
 */
public class ReadCommand extends AsyncResultProgram implements ReadExecutable {

    private static final String ID = "read"; //$NON-NLS-1$

    /**
     * Constructor of <code>ExecCommand</code>.
     *
     * @param file The file to read
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public ReadCommand(
            String file, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, new String[]{file});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {/**NON BLOCK**/}

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
        //If a listener is defined, then send the partial result
        if (in != null && in.length > 0) {
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(in);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParseErrorPartialResult(byte[] partialErr) {/**NON BLOCK**/}

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
    public boolean isIgnoreShellStdErrCheck() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean parseOnlyCompleteLines() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {
        // We have not privileges to read the file
        if (exitCode == 1) {
            throw new InsufficientPermissionsException();
        }

        //Ignore exit code 143 (cancelled)
        //Ignore exit code 137 (kill -9)
        if (exitCode != 0 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 &&  && exitCode != 1 && != 143 && != 137"); //$NON-NLS-1$
        }
    }
}
