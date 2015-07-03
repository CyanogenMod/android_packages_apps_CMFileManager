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

import com.cyanogenmod.filemanager.commands.SIGNAL;

/**
 * An interface for communicate shell results in a asynchronous way.
 */
public interface AsyncResultProgramListener {

    /**
     * Method invoked when an end of the program start.
     *
     * @return SIGNAL The signal to send to the process
     */
    SIGNAL onRequestEnd();

    /**
     * Method invoked when the parse of results will start.
     */
    void onStartParsePartialResult();

    /**
     * Method invoked when the parse of results is ended and no new result.
     * will be received
     *
     * @param cancelled Indicates if the program was cancelled
     */
    void onEndParsePartialResult(boolean cancelled);

    /**
     * Method invoked when a parse of new results are needed.
     *
     * @param partialIn A partial standard input buffer (incremental buffer)
     */
    void onParsePartialResult(byte[] partialIn);

    /**
     * Method invoked when a parse of new error results are needed.
     *
     * @param partialErr A partial standard err buffer (incremental buffer)
     */
    void onParseErrorPartialResult(byte[] partialErr);
}
