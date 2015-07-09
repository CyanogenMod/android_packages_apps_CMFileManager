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

import com.cyanogenmod.filemanager.util.CommandHelper;

/**
 * A class for testing the {@link MoveCommand} command.
 *
 * @see MoveCommand
 */
public class MoveCommandTest extends AbstractConsoleTest {

    private static final String PATH_FILE_SRC =
            Environment.getDataDirectory().getAbsolutePath() + "/movetest.txt"; //$NON-NLS-1$
    private static final String PATH_FILE_DST =
            Environment.getDataDirectory().getAbsolutePath() + "/movetest2.txt"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRootConsoleNeeded() {
        return true;
    }

    /**
     * Method that performs a test to move a file.
     *
     * @throws Exception If test failed
     */
    @SmallTest
    public void testMoveOk() throws Exception {
        try {
            CommandHelper.createFile(getContext(), PATH_FILE_SRC, getConsole());
            boolean ret =
                    CommandHelper.move(getContext(), PATH_FILE_SRC, PATH_FILE_DST,
                            getConsole(), getConsole());
            assertTrue("response==false", ret); //$NON-NLS-1$
        } finally {
            try {
                CommandHelper.deleteFile(getContext(), PATH_FILE_SRC, getConsole());
            } catch (Throwable ex) {
                /**NON BLOCK**/
            }
            try {
                CommandHelper.deleteFile(getContext(), PATH_FILE_DST, getConsole());
            } catch (Throwable ex) {
                /**NON BLOCK**/
            }
        }
    }


}
