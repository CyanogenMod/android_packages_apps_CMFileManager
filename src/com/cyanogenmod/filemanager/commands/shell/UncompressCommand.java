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
import com.cyanogenmod.filemanager.commands.UncompressExecutable;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.preferences.UncompressionMode;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;

/**
 * A class for uncompress file system objects.
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?tar"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?unzip"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?gunzip"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?bunzip2"}
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?xz"}
 */
public class UncompressCommand extends AsyncResultProgram implements UncompressExecutable {

    /**
     * An enumeration of implemented uncompression modes.
     */
    private enum Mode {
        /**
         * Uncompress using Tar algorithm
         */
        A_UNTAR(UNTAR_ID, "", UncompressionMode.A_UNTAR), //$NON-NLS-1$
        /**
         * Uncompress using Tar algorithm
         */
        A_UNZIP(UNZIP_ID, "", UncompressionMode.A_UNZIP), //$NON-NLS-1$
        /**
         * Uncompress using Gzip algorithm
         */
        AC_GUNZIP(UNTAR_ID, "z", UncompressionMode.AC_GUNZIP), //$NON-NLS-1$
        /**
         * Uncompress using Gzip algorithm
         */
        AC_GUNZIP2(UNTAR_ID, "z", UncompressionMode.AC_GUNZIP2), //$NON-NLS-1$
        /**
         * Uncompress using Bzip algorithm
         */
        AC_BUNZIP(UNTAR_ID, "j", UncompressionMode.AC_BUNZIP), //$NON-NLS-1$
        /**
         * Uncompress using Lzma algorithm
         */
        AC_UNLZMA(UNTAR_ID, "a", UncompressionMode.AC_UNLZMA), //$NON-NLS-1$
        /**
         * Uncompress using Gzip algorithm
         */
        C_GUNZIP(GUNZIP_ID, "", UncompressionMode.C_GUNZIP), //$NON-NLS-1$
        /**
         * Uncompress using Bzip algorithm
         */
        C_BUNZIP(BUNZIP_ID, "", UncompressionMode.C_BUNZIP), //$NON-NLS-1$
        /**
         * Uncompress using Lzma algorithm
         */
        C_UNLZMA(UNLZMA_ID, "", UncompressionMode.C_UNLZMA), //$NON-NLS-1$
        /**
         * Uncompress using Unix compress algorithm
         */
        C_UNCOMPRESS(UNCOMPRESS_ID, "", UncompressionMode.C_UNCOMPRESS), //$NON-NLS-1$
        /**
         * Uncompress using Unix compress algorithm
         */
        C_UNXZ(UNXZ_ID, "", UncompressionMode.C_UNXZ), //$NON-NLS-1$
        /**
         * Uncompress using Rar algorithm
         */
        A_UNRAR(UNRAR_ID, "", UncompressionMode.C_UNRAR); //$NON-NLS-1$

        final String mId;
        final String mFlag;
        UncompressionMode mMode;

        /**
         * Constructor of <code>Mode</code>
         *
         * @param id The command identifier
         * @param flag The tar compression flag
         * @param mode The uncompressed mode
         */
        private Mode(String id, String flag, UncompressionMode mode) {
            this.mId = id;
            this.mFlag = flag;
            this.mMode = mode;
        }
    }

    private static final String UNTAR_ID = "untar"; //$NON-NLS-1$
    private static final String UNZIP_ID = "unzip"; //$NON-NLS-1$
    private static final String GUNZIP_ID = "gunzip"; //$NON-NLS-1$
    private static final String BUNZIP_ID = "bunzip"; //$NON-NLS-1$
    private static final String UNLZMA_ID = "unlzma"; //$NON-NLS-1$
    private static final String UNCOMPRESS_ID = "uncompress"; //$NON-NLS-1$
    private static final String UNXZ_ID = "unxz"; //$NON-NLS-1$
    private static final String UNRAR_ID = "unrar"; //$NON-NLS-1$

    private Boolean mResult;
    private String mPartial;

    private final String mOutFile;
    private final Mode mMode;

    /**
     * Constructor of <code>UncompressCommand</code>.<br/>
     * <br/>
     * <ul>
     * <li>For archive and archive-compressed files, the file is extracted in a directory
     * of the current location of the file with the name of the file without the extension.</li>
     * <li>For compressed files, the file is extracted in the same directory in a file without
     * the extension, and the source file is deleted.</li>
     * </ul>
     *
     * @param src The archive-compressed file
     * @param dst The destination file of folder (if null this method resolve with the best
     * fit based on the src)
     * @param asyncResultListener The partial result listener
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public UncompressCommand(
            String src, String dst, AsyncResultListener asyncResultListener)
            throws InvalidCommandDefinitionException {
        super(resolveId(src), asyncResultListener, resolveArguments(src, dst));

        // Check that have a valid
        Mode mode = getMode(src);
        if (mode == null) {
            throw new InvalidCommandDefinitionException(
                            "Unsupported uncompress mode"); //$NON-NLS-1$
        }
        this.mMode = mode;

        // Retrieve information about the uncompress process
        if (dst != null) {
            this.mOutFile = dst;
        } else {
            this.mOutFile = resolveOutputFile(src);
        }
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
        if (exitCode != 0 && exitCode != 143 && exitCode != 137) {
            throw new ExecutionException(
                        "exitcode != 0 && != 1 && != 143 && != 137"); //$NON-NLS-1$
        }

        // Correct
        this.mResult = Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutUncompressedFile() {
        return this.mOutFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean IsArchive() {
        return this.mMode.mMode.mArchive;
    }

    /**
     * Method that processes a line to determine if it's a valid partial result
     *
     * @param line The line to process
     * @return String The processed line
     */
    private String processPartialResult(String line) {
        if (this.mMode.compareTo(Mode.A_UNRAR) == 0) {
            if (line.startsWith("Extracting  ")) { //$NON-NLS-1$
                int pos = line.indexOf((char)8);
                if (pos != -1) {
                    // Remove progress
                    return line.substring(12, pos).trim();
                }
                return line.substring(12).trim();
            }
            return null;
        }

        if (this.mMode.compareTo(Mode.A_UNZIP) == 0) {
            if (line.startsWith("  inflating: ")) { //$NON-NLS-1$
                return line.substring(13).trim();
            }
            return null;
        }

        return line;
    }

    /**
     * Method that resolves the identifier to use as command
     *
     * @param src The compressed file
     * @return String The identifier of the command
     */
    private static String resolveId(String src) {
        Mode mode = getMode(src);
        if (mode != null) {
            return mode.mId;
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Method that resolves the arguments for the uncompression
     *
     * @param src The source file
     * @param dst The destination file
     * @return String[] The arguments
     */
    private static String[] resolveArguments(String src, String dst) {
        String out = dst;
        if (out == null) {
            out = resolveOutputFile(src);
        }
        Mode mode = getMode(src);
        if (mode != null) {
            switch (mode) {
                case A_UNTAR:
                case AC_GUNZIP:
                case AC_GUNZIP2:
                case AC_BUNZIP:
                case AC_UNLZMA:
                    return new String[]{mode.mFlag, out, src};

                case A_UNZIP:
                case A_UNRAR:
                    return new String[]{out, src};

                case C_GUNZIP:
                case C_BUNZIP:
                case C_UNLZMA:
                case C_UNCOMPRESS:
                case C_UNXZ:
                    return new String[]{src};

                default:
                    break;
            }
        }
        return new String[]{};
    }

    /**
     * Method that resolves the output path of the uncompressed file
     *
     * @return String The output path of the uncompressed file
     */
    private static String resolveOutputFile(String src) {
        String name = new File(FileHelper.getName(src)).getName();
        File dst = new File(new File(src).getParent(), name);
        return dst.getAbsolutePath();
    }

    /**
     * Method that returns the uncompression mode from the compressed file
     *
     * @param src The compressed file
     * @return Mode The uncompression mode. <code>null</code> if no mode found
     */
    private static Mode getMode(String src) {
        String extension = FileHelper.getExtension(src);
        Mode[] modes = Mode.values();
        int cc = modes.length;
        for (int i = 0; i < cc; i++) {
            Mode mode = modes[i];
            if (mode.mMode.mExtension.compareToIgnoreCase(extension) == 0) {
                return mode;
            }
        }
        return null;
    }
}
