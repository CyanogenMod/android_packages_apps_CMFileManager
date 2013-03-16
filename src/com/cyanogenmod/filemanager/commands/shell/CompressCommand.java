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
import com.cyanogenmod.filemanager.commands.CompressExecutable;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.preferences.CompressionMode;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;

/**
 * A class for compress file system objects
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?tar"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?gzip"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?bzip2"}
 */
public class CompressCommand extends AsyncResultProgram implements CompressExecutable {

    /**
     * An enumeration of implemented compression modes.
     */
    private enum Mode {
        /**
         * Archive using Tar algorithm
         */
        A_TAR(TAR_ID, "", CompressionMode.A_TAR), //$NON-NLS-1$
        /**
         * Archive and compress using Gzip algorithm
         */
        AC_GZIP(TAR_ID, "z", CompressionMode.AC_GZIP), //$NON-NLS-1$
        /**
         * Archive and compress using Gzip algorithm
         */
        AC_GZIP2(TAR_ID, "z", CompressionMode.AC_GZIP2), //$NON-NLS-1$
        /**
         * Archive and compress using Bzip algorithm
         */
        AC_BZIP(TAR_ID, "j", CompressionMode.AC_BZIP), //$NON-NLS-1$
        /**
         * Compress using Gzip algorithm
         */
        C_GZIP(GZIP_ID, "z", CompressionMode.C_GZIP), //$NON-NLS-1$
        /**
         * Compress using Bzip algorithm
         */
        C_BZIP(BZIP_ID, "j", CompressionMode.C_BZIP), //$NON-NLS-1$
        /**
         * Archive using Zip algorithm
         */
        A_ZIP(ZIP_ID, "", CompressionMode.A_ZIP); //$NON-NLS-1$

        final String mId;
        final String mFlag;
        final CompressionMode mMode;

        /**
         * Constructor of <code>Mode</code>
         *
         * @param id The command identifier
         * @param flag The tar compression flag
         * @param mode The compression mode
         */
        private Mode(String id, String flag, CompressionMode mode) {
            this.mId = id;
            this.mFlag = flag;
            this.mMode = mode;
        }

        /**
         * Method that return the mode from his compression mode
         *
         * @param mode The compression mode
         * @return Mode The mode
         */
        public static Mode fromCompressionMode(CompressionMode mode) {
            Mode[] modes = Mode.values();
            int cc = modes.length;
            for (int i = 0; i < cc; i++) {
                if (modes[i].mMode.compareTo(mode) == 0) {
                    return modes[i];
                }
            }
            return null;
        }
    }

    private static final String TAR_ID = "tar"; //$NON-NLS-1$
    private static final String GZIP_ID = "gzip"; //$NON-NLS-1$
    private static final String BZIP_ID = "bzip"; //$NON-NLS-1$
    private static final String ZIP_ID = "zip"; //$NON-NLS-1$

    private Boolean mResult;
    private String mPartial;

    private final Mode mMode;
    private final String mOutFile;

    /**
     * Constructor of <code>CompressCommand</code>. This method creates an archive-compressed
     * file from one or various file system objects.
     *
     * @param mode The compression mode
     * @param dst The absolute path of the new compress file
     * @param src An array of file system objects to compress
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public CompressCommand(
            CompressionMode mode, String dst, String[] src, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(Mode.fromCompressionMode(mode).mId,
              asyncResultListener,
              resolveArchiveArgs(Mode.fromCompressionMode(mode), dst));
        this.mMode = Mode.fromCompressionMode(mode);

        if (!this.mMode.mMode.mArchive) {
            throw new InvalidCommandDefinitionException(
                            "Unsupported archive mode"); //$NON-NLS-1$
        }

        //Convert the arguments from absolute to relative
        addExpandedArguments(
                convertAbsolutePathsToRelativePaths(dst, src), true);

        // Create the output file
        this.mOutFile = dst;
    }

    /**
     * Constructor of <code>CompressCommand</code>. This method creates a compressed
     * file from one file.
     *
     * @param mode The compression mode
     * @param src The file to compress
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public CompressCommand(
            CompressionMode mode, String src, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(Mode.fromCompressionMode(mode).mId,
              asyncResultListener,
              resolveCompressArgs(mode, src));
        this.mMode = Mode.fromCompressionMode(mode);

        if (this.mMode.mMode.mArchive) {
            throw new InvalidCommandDefinitionException(
                            "Unsupported compression mode"); //$NON-NLS-1$
        }

        // Create the output file
        this.mOutFile = resolveOutputFile(mode, src);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartParsePartialResult() {
        this.mResult = Boolean.FALSE;
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
        int cc = lines.length;
        for (int i = 0; i < cc-1; i++) {
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResult() {
        return this.mResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {

        //Ignore exit code 143 (cancelled)
        //Ignore exit code 137 (kill -9)
        if (exitCode != 0 && exitCode != 1 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 && != 143 && != 137"); //$NON-NLS-1$
        }

        // Correct
        this.mResult = Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutCompressedFile() {
        return this.mOutFile;
    }

    /**
     * Method that resolves the arguments for the archive mode
     *
     * @return String[] The arguments
     */
    private final static String[] resolveArchiveArgs(Mode mode, String dst) {
        if (mode.compareTo(Mode.A_ZIP) == 0) {
            return new String[]{
                    FileHelper.getParentDir(dst),
                    dst
                };
        }
        return new String[]{
                FileHelper.getParentDir(dst),
                mode.mFlag,
                dst
            };
    }

    /**
     * Method that resolves the arguments for the compression mode
     *
     * @return String[] The arguments
     */
    private static String[] resolveCompressArgs(CompressionMode mode, String src) {
        switch (mode) {
            case C_GZIP:
            case C_BZIP:
                return new String[]{src};
            default:
                return new String[]{};
        }
    }

    /**
     * Method that processes a line to determine if it's a valid partial result
     *
     * @param line The line to process
     * @return String The processed line
     */
    private String processPartialResult(String line) {
        if (this.mMode.compareTo(Mode.A_ZIP) == 0) {
            if (line.startsWith("  adding: ")) { //$NON-NLS-1$
                int pos = line.lastIndexOf('(');
                if (pos != -1) {
                    // Remove progress
                    return line.substring(10, pos).trim();
                }
                return line.substring(10).trim();
            }
            return null;
        }
        return line;
    }

    /**
     * Method that resolves the output path of the compressed file
     *
     * @return String The output path of the compressed file
     */
    private static String resolveOutputFile(CompressionMode mode, String src) {
        return String.format("%s.%s", src, mode.mExtension); //$NON-NLS-1$
    }

    /**
     * Method that converts the absolute paths of the source files to relative paths
     *
     * @param dst The destination compressed file
     * @param src The source uncompressed files
     * @return String[] The array of relative paths
     */
    private static String[] convertAbsolutePathsToRelativePaths(String dst, String[] src) {
        File parent  = new File(dst).getParentFile();
        String p = File.separator;
        if (parent != null) {
            p = parent.getAbsolutePath();
        }

        // Converts every path
        String[] out = new String[src.length];
        int cc = src.length;
        for (int i = 0; i < cc; i++) {
            out[i] = FileHelper.toRelativePath(src[i], p);
        }
        return out;
    }
}
