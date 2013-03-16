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
import com.cyanogenmod.filemanager.commands.ChecksumExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;

import java.io.File;

/**
 * A class for calculate MD5 and SHA-1 checksums of a file system object.<br />
 * <br />
 * Partial results are returned in order (MD5 -> SHA1)
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?md5sum"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?sha1sum"}
 * @see com.cyanogenmod.filemanager.commands.ChecksumExecutable.CHECKSUMS
 */
public class ChecksumCommand extends AsyncResultProgram implements ChecksumExecutable {

    private static final String ID = "checksum";  //$NON-NLS-1$

    private final String mName;
    private final String[] mChecksums;
    private int mChecksumsCounter;
    private String mPartial;

    /**
     * Constructor of <code>ChecksumCommand</code>.
     *
     * @param src The source file
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public ChecksumCommand(String src, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, src);
        this.mChecksums = new String[]{null, null};
        this.mName = new File(src).getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        this.mChecksums[0] = null;
        this.mChecksums[1] = null;
        this.mChecksumsCounter = 0;
        this.mPartial = ""; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndParsePartialResult(boolean cancelled) {
        // Send the last partial data
        if (this.mPartial != null && this.mPartial.length() > 0) {
            if (getAsyncResultListener() != null) {
                String data = processPartialResult(this.mPartial);
                if (data != null) {
                    getAsyncResultListener().onPartialResult(data);
                }
            }
        }
        this.mPartial = ""; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParsePartialResult(final String partialIn) {
        if (partialIn == null || partialIn.length() ==0) return;
        boolean endsWithNewLine = partialIn.endsWith("\n"); //$NON-NLS-1$
        String[] lines = partialIn.split("\n"); //$NON-NLS-1$

        // Append the pending data to the first line
        lines[0] = this.mPartial + lines[0];

        // Return all the lines, except the last
        for (int i = 0; i < lines.length-1; i++) {
            if (getAsyncResultListener() != null) {
                String data = processPartialResult(lines[i]);
                if (data != null) {
                    getAsyncResultListener().onPartialResult(data);
                }
            }
        }

        // Return the last line?
        if (endsWithNewLine) {
            if (getAsyncResultListener() != null) {
                String data = processPartialResult(lines[lines.length-1]);
                if (data != null) {
                    getAsyncResultListener().onPartialResult(data);
                }
            }
            this.mPartial = ""; //$NON-NLS-1$
        } else {
            // Save the partial for next calls
            this.mPartial = lines[lines.length-1];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParseErrorPartialResult(String partialErr) {/**NON BLOCK**/}

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
     * Method that processes a line to determine if it's a valid partial result
     *
     * @param line The line to process
     * @return String The processed line
     */
    private String processPartialResult(String line) {
        // MD5 and SHA-1 return both the digest and the name of the file
        // 4c044b884cf2ff3839713da0e81dced19f099b09  boot.zip
        int pos = line.indexOf(" "); //$NON-NLS-1$
        if (line.endsWith(this.mName) && pos != -1) {
            String digest = line.substring(0, pos).trim();
            if (this.mChecksumsCounter < this.mChecksums.length) {
                this.mChecksums[this.mChecksumsCounter] = digest;
            }
            this.mChecksumsCounter++;
            return digest;
        }
        return null;
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
