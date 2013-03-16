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
import android.test.suitebuilder.annotation.SmallTest;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ChecksumExecutable;
import com.cyanogenmod.filemanager.commands.ChecksumExecutable.CHECKSUMS;
import com.cyanogenmod.filemanager.util.CommandHelper;

/**
 * A class for testing checksum command.
 *
 * @see ChecksumCommand
 */
public class ChecksumCommandTest extends AbstractConsoleTest {

    private static final String TEST_FILE =
            Environment.getRootDirectory().getAbsolutePath() + "/fonts/Roboto-Bold.ttf"; //$NON-NLS-1$

    private static final String MD5_SUM = "0a15e86bdff7da5886fe6535b50d9988"; //$NON-NLS-1$
    private static final String SHA1_SUM = "624735f02422f13e50ccf466f0d29edda05adb36"; //$NON-NLS-1$

    /**
     * @hide
     */
    final Object mSync = new Object();
    /**
     * @hide
     */
    boolean mNormalEnd;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRootConsoleNeeded() {
        return true;
    }

    /**
     * Method that performs a checksum test
     *
     * @throws Exception If an exception occurs while executing the test
     */
    @SmallTest
    @SuppressWarnings("null")
    public void testChecksums() throws Exception {
        ChecksumExecutable cmd =
                CommandHelper.checksum(getContext(),
                        TEST_FILE, new AsyncResultListener() {
                            @Override
                            public void onAsyncStart() {
                                /**NON BLOCK**/
                            }
                            @Override
                            public void onAsyncEnd(boolean cancelled) {
                                synchronized (ChecksumCommandTest.this.mSync) {
                                    ChecksumCommandTest.this.mNormalEnd = true;
                                    ChecksumCommandTest.this.mSync.notify();
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
                            public void onPartialResult(Object results) {
                                /**NON BLOCK**/
                            }
                        }, getConsole());
        
        synchronized (ChecksumCommandTest.this.mSync) {
            ChecksumCommandTest.this.mSync.wait(15000L);
        }
        try {
            if (!this.mNormalEnd && cmd != null && cmd.isCancellable() && !cmd.isCancelled()) {
                cmd.cancel();
            }
        } catch (Exception e) {/**NON BLOCK**/}
        assertNotNull("md5==null", cmd.getChecksum(CHECKSUMS.MD5)); //$NON-NLS-1$
        assertNotNull("sha1==null", cmd.getChecksum(CHECKSUMS.SHA1)); //$NON-NLS-1$
        assertEquals("md5sum fails", MD5_SUM, cmd.getChecksum(CHECKSUMS.MD5)); //$NON-NLS-1$
        assertEquals("sha1sum fails", SHA1_SUM, cmd.getChecksum(CHECKSUMS.SHA1)); //$NON-NLS-1$
    }

}
