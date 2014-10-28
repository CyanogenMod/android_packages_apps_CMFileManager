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

import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;


/**
 * An abstract base class for all secure executables.
 */
public abstract class Program implements Executable {

    private SecureConsole mConsole;
    private boolean mTrace;
    private int mBufferSize;

    /**
     * Constructor of <code>Program</code>
     */
    public Program(SecureConsole console) {
        super();
        mConsole = console;
    }

    /**
     * Method that return if the command has to trace his operations
     *
     * @return boolean If the command has to trace
     */
    public boolean isTrace() {
        return this.mTrace;
    }

    /**
     * Method that sets if the command has to trace his operations
     *
     * @param trace If the command has to trace
     */
    public void setTrace(boolean trace) {
        this.mTrace = trace;
    }

    /**
     * Method that return the buffer size of the program
     *
     * @return int The buffer size of the program
     */
    public int getBufferSize() {
        return this.mBufferSize;
    }

    /**
     * Method that sets the buffer size of the program
     *
     * @param bufferSize The buffer size of the program
     */
    public void setBufferSize(int bufferSize) {
        this.mBufferSize = bufferSize;
    }

    /**
     * Method that returns the current console of the program
     *
     * @return SecureConsole The current console
     */
    public SecureConsole getConsole() {
        return mConsole;
    }

    /**
     * Method that returns if this program uses an asynchronous model. <code>false</code>
     * by default.
     *
     * @return boolean If this program uses an asynchronous model
     */
    @SuppressWarnings("static-method")
    public boolean isAsynchronous() {
        return false;
    }

    /**
     * Method that returns if the program requires a sync of the underlying storage
     *
     * @return boolean if the program requires a sync operation
     */
    public boolean requiresSync() {
        return false;
    }

    /**
     * Method that returns if the program requires that the file system is mounted
     *
     * @return boolean If the program requires that the file system is mounted
     */
    public boolean requiresOpen() {
        return true;
    }

    /**
     * Method that executes the program
     *
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws ExecutionException If the operation returns a invalid exit code
     */
    public abstract void execute()
            throws NoSuchFileOrDirectory, ExecutionException;

}
