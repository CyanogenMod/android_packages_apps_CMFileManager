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

import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;

import java.io.OutputStream;


/**
 * An abstract class that represents a command that need a shell
 * to be executed.
 */
public abstract class Program extends Command implements Executable {

    /**
     * An interface for transmitting data to the program
     */
    public interface ProgramListener {
        /**
         * Invoked when a request the program need to write in the shell program.
         *
         * @param data The data to write to the shell console
         * @param offset The initial position in buffer to store the bytes read from this stream
         * @param byteCount The maximum number of bytes to store in b
         * @return boolean If the write was transmitted successfully
         * @throws ExecutionException If the console is not ready
         */
        boolean onRequestWrite(byte[] data, int offset, int byteCount) throws ExecutionException;

        /**
         * Method that returns the output stream of the console.
         *
         * @return OutputStream The output stream of the console
         */
        OutputStream getOutputStream();
    }

    // The listener for the program
    private ProgramListener mProgramListener;

    // Indicate if the program expect some output to stderr. If something is received
    // in the stderr the program should be killed
    private boolean mExitOnStdErrOutput;

    /**
     * @Constructor of <code>Program</code>
     *
     * @param id The resource identifier of the command
     * @param args Arguments of the command (will be formatted with the arguments from
     * the command definition)
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public Program(String id, String... args) throws InvalidCommandDefinitionException {
        super(id, args);
        this.mExitOnStdErrOutput = false;
    }

    /**
     * @Constructor of <code>Program</code>
     *
     * @param id The resource identifier of the command
     * @param prepare Indicates if the argument must be prepared
     * @param args Arguments of the command (will be formatted with the arguments from
     * the command definition)
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public Program(String id, boolean prepare, String... args)
            throws InvalidCommandDefinitionException {
        super(id, prepare, args);
        this.mExitOnStdErrOutput = false;
    }

    /**
     * Method that returns the program listener
     *
     * @return ProgramListener The program listener
     */
    protected ProgramListener getProgramListener() {
        return this.mProgramListener;
    }

    /**
     * Method that sets the program listener
     *
     * @param programListener The program listener
     */
    public void setProgramListener(ProgramListener programListener) {
        this.mProgramListener = programListener;
    }

    /**
     * Method that returns if the program should be killed if some output is received in
     * the standard error buffer.
     *
     * @return boolean If the program should be killed
     */
    public boolean isExitOnStdErrOutput() {
        return this.mExitOnStdErrOutput;
    }

    /**
     * Method that sets if the program should be killed if some output is received in
     * the standard error buffer.
     *
     * @param exitOnStdErrOutput If the program should be killed
     */
    public void setExitOnStdErrOutput(boolean exitOnStdErrOutput) {
        this.mExitOnStdErrOutput = exitOnStdErrOutput;
    }

    /**
     * Returns whether the shell should wait indefinitely for the end of the command.
     *
     * @return boolean If shell should wait indefinitely for the end of the command
     * @hide
     */
    @SuppressWarnings("static-method")
    public boolean isIndefinitelyWait() {
        return false;
    }

    /**
     * Returns whether the shell shouldn't raise a {@link OperationTimeoutException} when
     * the program didn't exited but new data was received.
     *
     * @return boolean If shell shouldn't raise a {@link OperationTimeoutException} if new
     * data was received
     * @hide
     */
    @SuppressWarnings("static-method")
    public boolean isWaitOnNewDataReceipt() {
        return false;
    }

    /**
     * Method that returns if the standard error must be
     * ignored safely by the shell, and don't check for errors
     * like <code>NoSuchFileOrDirectory</code> or
     * <code>Permission denied</code> by the shell.
     *
     * @return boolean If the standard error must be ignored
     * @hide
     */
    @SuppressWarnings("static-method")
    public boolean isIgnoreShellStdErrCheck() {
        return false;
    }

    /**
     * Method that checks if the standard errors has exceptions.
     *
     * @param exitCode Program exit code
     * @param err Standard Error buffer
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws CommandNotFoundException If the command was not found
     * @throws ExecutionException If the another exception is detected in the standard error
     * @hide
     */
    public void checkStdErr(int exitCode, String err)
            throws InsufficientPermissionsException, NoSuchFileOrDirectory,
            CommandNotFoundException, ExecutionException {
        /**NON BLOCK**/
    }

}
