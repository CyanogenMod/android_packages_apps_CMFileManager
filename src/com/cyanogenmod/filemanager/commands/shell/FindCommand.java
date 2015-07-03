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

import android.util.Log;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.FindExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.ParseHelper;
import com.cyanogenmod.filemanager.util.SearchHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for search files.
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?find"}
 */
public class FindCommand extends AsyncResultProgram implements FindExecutable {

    private static final String TAG = "FindCommand"; //$NON-NLS-1$

    private static final String ID = "find";  //$NON-NLS-1$

    private final File mDirectory;

    /**
     * Constructor of <code>FindCommand</code>.
     *
     * @param directory The absolute path of the directory where do the search
     * @param query The terms to be searched
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public FindCommand(
            String directory, Query query, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, createArgs(FileHelper.addTrailingSlash(directory), query));
        this.mDirectory = new File(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndParsePartialResult(boolean cancelled) {
        //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onParsePartialResult(byte[] in) {
        String partialIn = new String(in);

        // Check the in buffer to extract information
        final List<FileSystemObject> partialFiles = new ArrayList<FileSystemObject>();
        BufferedReader br = null;
        try {
            //Read the partial + previous partial and clean partial
            br = new BufferedReader(new StringReader(partialIn));

            //Add all lines to an array
            String line = null;
            while ((line = br.readLine()) != null) {
                //Checks that there is some text in the line. Otherwise ignore it
                if (line.trim().length() == 0) {
                    break;
                }

                // Add to the list
                try {
                    FileSystemObject fso = ParseHelper.parseStatOutput(line);

                    // Search directory is not part of the search
                    if (fso.getFullPath().compareTo(this.mDirectory.getAbsolutePath()) != 0) {
                        partialFiles.add(fso);
                    }

                } catch (Exception e) {
                    // Log the parsing error
                    if (isTrace()) {
                        Log.w(TAG,
                            String.format(
                                    "Failed to parse output: %s", //$NON-NLS-1$
                                    String.valueOf(line)));
                    }
                }
            }

            //If a listener is defined, then send the partial result
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(partialFiles);
            }

        } catch (Exception ex) {
            Log.w(TAG, "Partial result fails", ex); //$NON-NLS-1$

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Throwable ex) {
                /**NON BLOCK**/
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
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {

        //Search in a subdirectory without permissions returns 1, but this
        //not must be treated as an error
        //Ignore exit code 143 (cancelled)
        //Ignore exit code 137 (kill -9)
        if (exitCode != 0 && exitCode != 1 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 && != 1 && != 143 && != 137"); //$NON-NLS-1$
        }
    }

    /**
     * Method that create the arguments of this command, using the directory and
     * arguments and creating the regular expressions of the search.
     *
     * @param directory The directory where to search
     * @param query The query make for user
     * @return String[] The arguments of the command
     */
    private static String[] createArgs(String directory, Query query) {
        String[] args = new String[query.getSlotsCount() + 1];
        args[0] = directory;
        int cc = query.getSlotsCount();
        for (int i = 0; i < cc; i++) {
            args[i + 1] = SearchHelper.toIgnoreCaseRegExp(query.getSlot(i), false);
        }
        return args;
    }
}
