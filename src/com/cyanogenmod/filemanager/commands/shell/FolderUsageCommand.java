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
import com.cyanogenmod.filemanager.commands.FolderUsageExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.model.BlockDevice;
import com.cyanogenmod.filemanager.model.CharacterDevice;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.DomainSocket;
import com.cyanogenmod.filemanager.model.FolderUsage;
import com.cyanogenmod.filemanager.model.NamedPipe;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for retrieve the disk usage of a folder
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?ls"}
 */
public class FolderUsageCommand extends AsyncResultProgram implements FolderUsageExecutable {

    private static final String TAG = "FolderUsageCommand"; //$NON-NLS-1$

    private static final String ID = "folderusage"; //$NON-NLS-1$

    private final String mDirectory;
    private FolderUsage mFolderUsage;

    /**
     * Constructor of <code>FolderUsageCommand</code>.
     *
     * @param directory The absolute directory to compute
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public FolderUsageCommand(
            String directory, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(ID, asyncResultListener, new String[]{directory});
        this.mFolderUsage = new FolderUsage(directory);
        this.mDirectory = directory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        this.mFolderUsage = new FolderUsage(this.mDirectory);
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
        BufferedReader br = null;
        try {
            // Parse the line. We expect a ls -l output line
            // -rw-r--r-- root     root            7 2012-12-30 00:49 test.txt
            //
            // (1) permissions
            // (2) number of links and directories
            // (3) owner
            // (4) group
            // (5) size
            // (6) date
            // (7) name

            //Partial contains full lines
            br = new BufferedReader(new StringReader(partialIn));

            //Add all lines to an array
            List<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = br.readLine()) != null) {
                // Discard empty, paths, and folder links
                if (line.length() == 0 ||
                    line.startsWith(FileHelper.ROOT_DIRECTORY) ||
                    line.startsWith(FileHelper.CURRENT_DIRECTORY) ||
                    line.startsWith(FileHelper.PARENT_DIRECTORY)) {
                    continue;
                }
                lines.add(line);
            }

            int c = 0;
            try {
                while (lines.size() > 0) {
                    // Retrieve the info
                    String szLine = lines.remove(0).trim();
                    try {
                        // Clean the line (we don't care about names, only need the extension)
                        // so remove spaces is safe here
                        while (szLine.indexOf("  ") != -1) { //$NON-NLS-1$
                            szLine = szLine.replaceAll("  ", " "); //$NON-NLS-1$ //$NON-NLS-2$
                        }

                        // Don't compute . and ..
                        // This is not secure, but we don't need a exact precission on this
                        // method
                        if (szLine.length() == 0 ||
                            szLine.endsWith(" " + FileHelper.CURRENT_DIRECTORY) || //$NON-NLS-1$
                            szLine.endsWith(" " + FileHelper.PARENT_DIRECTORY)) { //$NON-NLS-1$
                            c++;
                            continue;
                        }

                        char type = szLine.charAt(0);
                        if (type == Symlink.UNIX_ID ||
                                type == BlockDevice.UNIX_ID ||
                                type == CharacterDevice.UNIX_ID ||
                                type == DomainSocket.UNIX_ID ||
                                type == NamedPipe.UNIX_ID) {
                            // File + Category
                            this.mFolderUsage.addFile();
                            if (type == Symlink.UNIX_ID) {
                                this.mFolderUsage.addFileToCategory(MimeTypeCategory.NONE);
                            } else {
                                this.mFolderUsage.addFileToCategory(MimeTypeCategory.SYSTEM);
                            }

                        } else if (type == Directory.UNIX_ID) {
                            // Folder
                            this.mFolderUsage.addFolder();

                        } else {
                            // File + Category + Size
                            try {
                                // we need a valid line
                                String[] fields = szLine.split(" "); //$NON-NLS-1$
                                if (fields.length < 8) {
                                    continue;
                                }

                                long size = Long.parseLong(fields[4]);
                                String name = fields[fields.length-1];// We only need the extension
                                String ext = FileHelper.getExtension(name);
                                File file = new File(mDirectory, name);
                                MimeTypeCategory category =
                                        MimeTypeHelper.getCategoryFromExt(null,
                                                                          ext,
                                                                          file.getAbsolutePath());
                                this.mFolderUsage.addFile();
                                this.mFolderUsage.addFileToCategory(category);
                                this.mFolderUsage.addSize(size);
                            } catch (Exception e) {/**NON BLOCK**/}
                        }
                        c++;

                    } catch (Exception e) {
                        // Ignore.
                    }

                    // Partial notification
                    if (c % 5 == 0) {
                        //If a listener is defined, then send the partial result
                        if (getAsyncResultListener() != null) {
                            getAsyncResultListener().onPartialResult(this.mFolderUsage);
                        }
                    }
                }
            } catch (Exception ex) { /**NON BLOCK **/ }

            //If a listener is defined, then send the partial result
            if (getAsyncResultListener() != null) {
                getAsyncResultListener().onPartialResult(this.mFolderUsage);
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
    public FolderUsage getFolderUsage() {
        return this.mFolderUsage;
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

        //Access a subdirectory without permissions returns 1, but this
        //not must be treated as an error
        //Ignore exit code 143 (cancelled)
        //Ignore exit code 137 (kill -9)
        if (exitCode != 0 && exitCode != 1 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 && != 1 && != 143 && != 137"); //$NON-NLS-1$
        }
    }
}
