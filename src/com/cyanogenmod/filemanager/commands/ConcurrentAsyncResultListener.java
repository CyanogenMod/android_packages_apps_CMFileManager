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

package com.cyanogenmod.filemanager.commands;

/**
 * An interface for communicate partial results in concurrent mode.
 */
public abstract class ConcurrentAsyncResultListener implements AsyncResultListener {

    private final Object mSync = new Object();
    private int mRefs;
    private boolean mStartNotified = false;
    private boolean mCancelled = false;

    /**
     * Constructor of {@code ConcurrentAsyncResultListener}
     */
    public ConcurrentAsyncResultListener() {
        super();
        mRefs = 0;
    }

    /**
     * Method invoked when the partial data has initialized.
     */
    public abstract void onConcurrentAsyncStart();

    /**
     * Method invoked when the partial data has finalized.
     *
     * @param cancelled Indicates if the program was cancelled
     */
    public abstract void onConcurrentAsyncEnd(boolean cancelled);

    /**
     * Method invoked when the program is ended.
     *
     * @param exitCode The exit code of the program
     */
    public abstract void onConcurrentAsyncExitCode(int exitCode);

    /**
     * Method invoked when new partial data are ready.
     *
     * @param result New data result
     */
    public abstract void onConcurrentPartialResult(Object result);

    /**
     * Method invoked when an exception occurs while executing the program.
     *
     * @param cause The cause that raise the exception
     */
    public abstract void onConcurrentException(Exception cause);

    /**
     * Return if the operation was cancelled by other listener
     *
     * @return boolean If the operation was cancelled
     */
    public boolean isCancelled() {
        return mCancelled;
    }

    /**
     * Method invoked when an object want to be part of this concurrent listener
     */
    public void onRegister() {
        synchronized (mSync) {
            mRefs++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onAsyncStart() {
        boolean notify = false;
        synchronized (mSync) {
            if (!mStartNotified) {
                notify = true;
            }
            mStartNotified = true;
        }
        if (notify) {
            onConcurrentAsyncStart();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onAsyncEnd(boolean cancelled) {
        boolean notify = false;
        if (cancelled) {
            mCancelled = true;
        }
        synchronized (mSync) {
            if (mRefs <= 1) {
                notify = true;
            }
            mRefs--;
            mStartNotified = true;
        }
        if (notify) {
            onConcurrentAsyncEnd(mCancelled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onAsyncExitCode(int exitCode) {
        boolean notify = false;
        synchronized (mSync) {
            if (mRefs <= 0) {
                notify = true;
            }
            mStartNotified = true;
        }
        if (notify) {
            onConcurrentAsyncExitCode(exitCode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onPartialResult(Object result) {
        synchronized (mSync) {
            if (!mCancelled && mRefs >= 1) {
                onConcurrentPartialResult(result);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onException(Exception cause) {
        synchronized (mSync) {
            if (!mCancelled && mRefs >= 1) {
                onConcurrentException(cause);
            }
        }
    }

}
