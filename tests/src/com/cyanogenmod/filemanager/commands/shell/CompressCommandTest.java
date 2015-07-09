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

import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.CompressExecutable;
import com.cyanogenmod.filemanager.preferences.CompressionMode;
import com.cyanogenmod.filemanager.util.CommandHelper;

/**
 * A class for testing the compression of file system objects.
 *
 * @see CompressCommand
 */
public class CompressCommandTest extends AbstractConsoleTest {

    private static final String TAG = "CompressCommandTest"; //$NON-NLS-1$

    private static final String TAR_OUTFILE =
            Environment.getDataDirectory().getAbsolutePath() + "/test.tar"; //$NON-NLS-1$
    private static final String TAR_GZIP_OUTFILE = TAR_OUTFILE + ".gz"; //$NON-NLS-1$
    private static final String TAR_BZIP_OUTFILE = TAR_OUTFILE + ".bz2"; //$NON-NLS-1$

    private static final String[] ARCHIVE_DATA =
        {
            Environment.getDataDirectory().getAbsolutePath() + "/misc", //$NON-NLS-1$
            Environment.getRootDirectory().getAbsolutePath() + "/build.prop" //$NON-NLS-1$
        };
    private static final String COMPRESS_DATA_SRC =
            Environment.getRootDirectory().getAbsolutePath() + "/build.prop"; //$NON-NLS-1$
    private static final String COMPRESS_DATA_DST =
            Environment.getDataDirectory().getAbsolutePath() + "/build.prop"; //$NON-NLS-1$

    /**
     * @hide
     */
    final Object mSync = new Object();
    /**
     * @hide
     */
    boolean mNewPartialData;
    /**
     * @hide
     */
    boolean mNormalEnd;
    /**
     * @hide
     */
    boolean mResult;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRootConsoleNeeded() {
        return true;
    }

    /**
     * Method that performs the test of archive data in TAR format.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testArchiveTAR() throws Exception {
        testArchiveAndCompress(CompressionMode.A_TAR, TAR_OUTFILE);
    }

    /**
     * Method that performs the test of archive and compress data in GZIP format.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testArchiveCompressGZIP() throws Exception {
        testArchiveAndCompress(CompressionMode.AC_GZIP, TAR_GZIP_OUTFILE);
    }

    /**
     * Method that performs the test of archive and compress data in BZIP format.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testArchiveCompressBZIP() throws Exception {
        testArchiveAndCompress(CompressionMode.AC_BZIP, TAR_BZIP_OUTFILE);
    }

    /**
     * Method that performs the test of compress data in GZIP format.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testCompressGZIP() throws Exception {
        testCompress(CompressionMode.C_GZIP);
    }

    /**
     * Method that performs the test of compress data in BZIP format.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testCompressBZIP() throws Exception {
        testCompress(CompressionMode.C_BZIP);
    }

    /**
     * Method that archive and compress data.
     *
     * @param mode The compress mode
     * @param dst The destination file
     * @throws Exception If test failed
     */
    private void testArchiveAndCompress(CompressionMode mode, String dst) throws Exception {
        try {
            this.mNewPartialData = false;
            this.mNormalEnd = false;
            CompressExecutable cmd =
                    CommandHelper.compress(
                        getContext(), mode, dst, ARCHIVE_DATA, new AsyncResultListener() {
                            @Override
                            public void onAsyncStart() {
                                /**NON BLOCK**/
                            }
                            @Override
                            public void onAsyncEnd(boolean cancelled) {
                                synchronized (CompressCommandTest.this.mSync) {
                                    CompressCommandTest.this.mNormalEnd = true;
                                    CompressCommandTest.this.mSync.notify();
                                }
                            }
                            @Override
                            public void onAsyncExitCode(int exitCode) {
                                /**NON BLOCK**/
                            }
                            @Override
                            public void onException(Exception cause) {
                                fail(String.valueOf(cause));
                            }
                            @Override
                            public void onPartialResult(Object result) {
                                CompressCommandTest.this.mNewPartialData = true;
                                Log.d(TAG, (String)result);
                            }
                       }, getConsole());
            synchronized (CompressCommandTest.this.mSync) {
                CompressCommandTest.this.mSync.wait(60000L);
            }
            try {
                if (!this.mNormalEnd && cmd != null && cmd.isCancellable() && !cmd.isCancelled()) {
                    cmd.cancel();
                }
            } catch (Exception e) {/**NON BLOCK**/}

            // Wait for result
            Thread.sleep(500L);

            assertTrue("no new partial data", this.mNewPartialData); //$NON-NLS-1$
            assertNotNull("cmd != null", cmd); //$NON-NLS-1$
            if (cmd != null) {
                assertTrue("return != true", cmd.getResult().booleanValue()); //$NON-NLS-1$
            }
        } finally {
            try {
                CommandHelper.deleteFile(getContext(), dst, getConsole());
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * Method that compress data.
     *
     * @param mode The compress mode
     * @throws Exception If test failed
     */
    private void testCompress(CompressionMode mode) throws Exception {
        CompressExecutable cmd = null;
        try {
            // Copy a file to the folder of the test
            CommandHelper.copy(getContext(), COMPRESS_DATA_SRC, COMPRESS_DATA_DST, null,
                    getConsole(), getConsole());

            this.mNewPartialData = false;
            this.mNormalEnd = false;
            cmd =
                    CommandHelper.compress(
                        getContext(), mode, COMPRESS_DATA_DST, new AsyncResultListener() {
                            @Override
                            public void onAsyncStart() {
                                /**NON BLOCK**/
                            }
                            @Override
                            public void onAsyncEnd(boolean cancelled) {
                                synchronized (CompressCommandTest.this.mSync) {
                                    CompressCommandTest.this.mNormalEnd = true;
                                    CompressCommandTest.this.mSync.notify();
                                }
                            }
                            @Override
                            public void onAsyncExitCode(int exitCode) {
                                /**NON BLOCK**/
                            }
                            @Override
                            public void onException(Exception cause) {
                                fail(String.valueOf(cause));
                            }
                            @Override
                            public void onPartialResult(Object result) {
                                CompressCommandTest.this.mNewPartialData = true;
                                Log.d(TAG, (String)result);
                            }
                       }, getConsole());
            synchronized (CompressCommandTest.this.mSync) {
                CompressCommandTest.this.mSync.wait(15000L);
            }
            try {
                if (!this.mNormalEnd && cmd != null && cmd.isCancellable() && !cmd.isCancelled()) {
                    cmd.cancel();
                }
            } catch (Exception e) {/**NON BLOCK**/}

            // Wait for result
            Thread.sleep(500L);

            assertNotNull("cmd != null", cmd); //$NON-NLS-1$
            if (cmd != null) {
                assertTrue("return != true", cmd.getResult().booleanValue()); //$NON-NLS-1$
            }
        } finally {
            if (cmd != null) {
                try {
                    CommandHelper.deleteFile(
                            this.mContext, COMPRESS_DATA_DST, getConsole());
                } catch (Exception e) {/**NON BLOCK**/}
                try {
                    CommandHelper.deleteFile(
                            this.mContext, cmd.getOutCompressedFile(), getConsole());
                } catch (Exception e) {/**NON BLOCK**/}
            }
        }
    }

}
