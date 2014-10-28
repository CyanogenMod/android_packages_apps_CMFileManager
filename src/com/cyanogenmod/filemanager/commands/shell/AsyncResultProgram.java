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

import com.cyanogenmod.filemanager.commands.AsyncResultExecutable;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ConcurrentAsyncResultListener;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class that allow the consumption of partial data. Commands
 * can parse the results while this are still retrieving.
 */
public abstract class AsyncResultProgram
    extends Program implements AsyncResultExecutable, AsyncResultProgramListener {

    /**
     * @hide
     */
    static final Byte STDIN = new Byte((byte)0);
    /**
     * @hide
     */
    static final Byte STDERR = new Byte((byte)1);

    private final AsyncResultListener mAsyncResultListener;
    private AsyncResultProgramThread mWorkerThread;
    /**
     * @hide
     */
    final List<String> mPartialData;
    /**
     * @hide
     */
    final List<Byte> mPartialDataType;
    final Object mSync = new Object();
    /**
     * @hide
     */
    final Object mTerminateSync = new Object();

    private boolean mCancelled;
    private OnCancelListener mOnCancelListener;
    private OnEndListener mOnEndListener;

    private StringBuffer mTempBuffer;

    /**
     * @Constructor of <code>AsyncResultProgram</code>.
     *
     * @param id The resource identifier of the command
     * @param asyncResultListener The partial result listener
     * @param args Arguments of the command (will be formatted with the arguments from
     * the command definition)
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public AsyncResultProgram(
            String id, AsyncResultListener asyncResultListener, String... args)
            throws InvalidCommandDefinitionException {
        this(id, true, asyncResultListener, args);
    }

    /**
     * @Constructor of <code>AsyncResultProgram</code>.
     *
     * @param id The resource identifier of the command
     * @param prepare Indicates if the argument must be prepared
     * @param asyncResultListener The partial result listener
     * @param args Arguments of the command (will be formatted with the arguments from
     * the command definition)
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     */
    public AsyncResultProgram(
            String id, boolean prepare, AsyncResultListener asyncResultListener, String... args)
            throws InvalidCommandDefinitionException {
        super(id, prepare, args);
        this.mAsyncResultListener = asyncResultListener;
        if (mAsyncResultListener instanceof ConcurrentAsyncResultListener) {
            ((ConcurrentAsyncResultListener) mAsyncResultListener).onRegister();
        }
        this.mPartialData = Collections.synchronizedList(new ArrayList<String>());
        this.mPartialDataType = Collections.synchronizedList(new ArrayList<Byte>());
        this.mTempBuffer = new StringBuffer();
        this.mOnCancelListener = null;
        this.mOnEndListener = null;
        this.mCancelled = false;
    }

    /**
     * Method that communicates that a new partial result parse will start.
     * @hide
     */
    public final void onRequestStartParsePartialResult() {
        this.mWorkerThread = new AsyncResultProgramThread();
        this.mWorkerThread.start();

        //Notify start to command class
        this.onStartParsePartialResult();

        //If a listener is defined, then send the start event
        if (getAsyncResultListener() != null) {
            getAsyncResultListener().onAsyncStart();
        }
    }

    /**
     * Method that communicates that partial result is ended and no new result
     * will be received.
     *
     * @param cancelled If the program was cancelled
     * @hide
     */
    public final void onRequestEndParsePartialResult(boolean cancelled) {
        synchronized (this.mSync) {
            this.mWorkerThread.mAlive = false;
            this.mSync.notify();
        }
        synchronized (this.mTerminateSync) {
            if (this.mWorkerThread.isAlive()) {
                try {
                    this.mTerminateSync.wait();
                } catch (Exception e) {
                    /**NON BLOCK**/
                }
            }
        }

        //Notify end to command class
        this.onEndParsePartialResult(cancelled);

        //If a listener is defined, then send the start event
        if (getAsyncResultListener() != null) {
            getAsyncResultListener().onAsyncEnd(cancelled);
        }
    }

    /**
     * Method that communicates the exit code of the program
     *
     * @param exitCode The exit code of the program
     * @hide
     */
    public final void onRequestExitCode(int exitCode) {
        //If a listener is defined, then send the start event
        if (getAsyncResultListener() != null) {
            getAsyncResultListener().onAsyncExitCode(exitCode);
        }
    }

    /**
     * Method that parse the result of a program invocation.
     *
     * @param partialIn A partial standard input buffer (incremental buffer)
     * @hide
     */
    public final void onRequestParsePartialResult(String partialIn) {
        synchronized (this.mSync) {
            String data = partialIn;
            String rest = ""; //$NON-NLS-1$
            if (parseOnlyCompleteLines()) {
                int pos = partialIn.lastIndexOf(FileHelper.NEWLINE);
                if (pos == -1) {
                    //Save partial data
                    this.mTempBuffer.append(partialIn);
                    return;
                }

                //Retrieve the data
                data = this.mTempBuffer.append(partialIn.substring(0, pos + 1)).toString();
                rest = partialIn.substring(pos + 1);
            }

            this.mPartialDataType.add(STDIN);
            this.mPartialData.add(data);
            this.mTempBuffer = new StringBuffer(rest);
            this.mSync.notify();
        }
    }

    /**
     * Method that parse the error result of a program invocation.
     *
     * @param partialErr A partial standard err buffer (incremental buffer)
     * @hide
     */
    public final void parsePartialErrResult(String partialErr) {
        synchronized (this.mSync) {
            String data = partialErr;
            String rest = ""; //$NON-NLS-1$
            if (parseOnlyCompleteLines()) {
                int pos = partialErr.lastIndexOf(FileHelper.NEWLINE);
                if (pos == -1) {
                    //Save partial data
                    this.mTempBuffer.append(partialErr);
                    return;
                }

                //Retrieve the data
                data = this.mTempBuffer.append(partialErr.substring(0, pos + 1)).toString();
                rest = partialErr.substring(pos + 1);
            }

            this.mPartialDataType.add(STDERR);
            this.mPartialData.add(data);
            this.mTempBuffer = new StringBuffer(rest);
            this.mSync.notify();
        }
    }

    /**
     * Method that returns if the <code>onParsePartialResult</code> method will
     * be called only complete lines are filled.
     *
     * @return boolean if the <code>onParsePartialResult</code> method will
     * be called only complete lines are filled
     */
    @SuppressWarnings("static-method")
    public boolean parseOnlyCompleteLines() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncResultListener getAsyncResultListener() {
        return this.mAsyncResultListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isCancelled() {
        return this.mCancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean cancel() {
        //Is't cancellable by definition?
        if (!isCancellable()) {
            return false;
        }

        //Stop the thread
        synchronized (this.mSync) {
            this.mWorkerThread.mAlive = false;
            this.mSync.notify();
        }

        //Notify cancellation
        if (this.mOnCancelListener != null) {
            this.mCancelled = this.mOnCancelListener.onCancel();
            return this.mCancelled;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean end() {
        // Internally this method do the same things that cancel method, but invokes
        // onEnd instead of onCancel

        //Is't cancellable by definition?
        if (!isCancellable()) {
            return false;
        }

        //Stop the thread
        synchronized (this.mSync) {
            this.mWorkerThread.mAlive = false;
            this.mSync.notify();
        }

        //Notify ending
        SIGNAL signal = onRequestEnd();
        if (this.mOnEndListener != null) {
            if (signal == null) {
                this.mCancelled = this.mOnEndListener.onEnd();
            } else {
                this.mCancelled = this.mOnEndListener.onSendSignal(signal);
            }
            return this.mCancelled;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOnCancelListener(OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOnEndListener(OnEndListener onEndListener) {
        this.mOnEndListener = onEndListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isIndefinitelyWait() {
        // Asynchronous programs should wait indefinitely for its nature
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        //By defect an asynchronous command is cancellable
        return true;
    }

    /**
     * Method that returns if the command is expected to finalize by it self, or needs
     * a call to end method.
     *
     * @return boolean If the command is expected to finalize by it self.
     */
    @SuppressWarnings("static-method")
    public boolean isExpectEnd() {
        return true;
    }

    /**
     * An internal class for process partial results sequentially in a
     * secure way.
     */
    private class AsyncResultProgramThread extends Thread {
        boolean mAlive = true;

        /**
         * Constructor of <code>AsyncResultProgramThread</code>.
         */
        AsyncResultProgramThread() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                this.mAlive = true;
                while (this.mAlive) {
                   synchronized (AsyncResultProgram.this.mSync) {
                       AsyncResultProgram.this.mSync.wait();
                       while (AsyncResultProgram.this.mPartialData.size() > 0) {
                           Byte type = AsyncResultProgram.this.mPartialDataType.remove(0);
                           String data = AsyncResultProgram.this.mPartialData.remove(0);
                           try {
                               if (type.compareTo(STDIN) == 0) {
                                   AsyncResultProgram.this.onParsePartialResult(data);
                               } else {
                                   AsyncResultProgram.this.onParseErrorPartialResult(data);
                               }
                           } catch (Throwable ex) {
                               /**NON BLOCK**/
                           }
                       }
                   }
                }
            } catch (Exception e) {
                /**NON BLOCK**/

            } finally {
                this.mAlive = false;
                synchronized (AsyncResultProgram.this.mTerminateSync) {
                    AsyncResultProgram.this.mTerminateSync.notify();
                }
            }
        }
    }

}
