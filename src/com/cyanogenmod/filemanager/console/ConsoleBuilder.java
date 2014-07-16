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

package com.cyanogenmod.filemanager.console;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.java.JavaConsole;
import com.cyanogenmod.filemanager.console.shell.NonPriviledgeConsole;
import com.cyanogenmod.filemanager.console.shell.PrivilegedConsole;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class responsible for creating consoles.
 */
public final class ConsoleBuilder {

    private static final String TAG = "ConsoleBuilder"; //$NON-NLS-1$

    private static final Object SYNC = new Object();
    private static ConsoleHolder sHolder;

    private static final int ROOT_UID = 0;

    /**
     * Constructor of <code>ConsoleBuilder</code>.
     */
    private ConsoleBuilder() {
        super();
    }

    /**
     * Method that returns a console, and creates a new console
     * if no console is allocated. The console is create if not exists.
     *
     * @param context The current context
     * @return Console An allocated console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     */
    public static Console getConsole(Context context)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        return getConsole(context, true);
    }

    /**
     * Method that returns a console. If {@linkplain "createIfNotExists"} is specified
     * a new console will be created
     *
     * @param context The current context
     * @param createIfNotExists Indicates that the console should be create if not exists
     * @return Console An allocated console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     */
    public static Console getConsole(Context context, boolean createIfNotExists)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {

        //Check if has a console. Otherwise create a new console
        if (sHolder == null || sHolder.getConsole() == null) {
            if (!createIfNotExists) {
                return null;
            }
            createDefaultConsole(context);
        }
        return sHolder.getConsole();
    }

    /**
     * Method that changes the current console to a non-privileged console.
     *
     * @param context The current context
     * @return boolean If the operation was successfully
     */
    public static boolean changeToNonPrivilegedConsole(Context context) {

        //Check the current console
        if (sHolder != null && sHolder.getConsole() instanceof NonPriviledgeConsole) {
            //The current console is non-privileged. Not needed
            return true;
        }

        //Create the console
        ConsoleHolder holder = null;
        try {
            //Create the console, destroy the current console, and marks as current
            holder = new ConsoleHolder(
                    createNonPrivilegedConsole(context));
            destroyConsole();
            sHolder = holder;
            return true;

        } catch (Throwable e) {
            if (holder != null) {
                holder.dispose();
            }
        }
        return false;
    }

    /**
     * Method that changes the current console to a privileged console.
     *
     * @param context The current context
     * @return boolean If the operation was successfully
     */
    public static boolean changeToPrivilegedConsole(Context context) {

        //Destroy and create the new console
        if (sHolder != null && sHolder.getConsole() instanceof PrivilegedConsole) {
            //The current console is privileged. Not needed
            return true;
        }

        //Create the console
        ConsoleHolder holder = null;
        try {
            //Create the console, destroy the current console, and marks as current
            holder = new ConsoleHolder(
                    createAndCheckPrivilegedConsole(context));
            destroyConsole();
            sHolder = holder;

            // Change also the background console to privileged
            FileManagerApplication.changeBackgroundConsoleToPriviligedConsole();

            return sHolder.getConsole() instanceof PrivilegedConsole;

        } catch (Throwable e) {
            destroyConsole();
            if (holder != null) {
                holder.dispose();
            }
        }
        return false;
    }

    /**
     * Method that returns a console, and creates a new console if no
     * console is allocated or if the settings preferences has changed.
     *
     * @param context The current context
     * @return Console An allocated console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     */
    //IMP! This must be invoked from the main activity creation
    public static Console createDefaultConsole(Context context)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        //Gets superuser mode settings
        boolean superuserMode =
                FileManagerApplication.getAccessMode().compareTo(AccessMode.ROOT) == 0;
        boolean advancedMode =
                FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;
        boolean restrictedMode =
                AndroidHelper.hasSupportForMultipleUsers(context) && !AndroidHelper.isUserOwner();
        if (restrictedMode) {
            // Is a secondary user. Restrict access to the whole system
            try {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_ACCESS_MODE, AccessMode.SAFE, true);
            } catch (Throwable ex) {
                Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
            }
            superuserMode = false;
        }
        else if (superuserMode && !advancedMode) {
            try {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_ACCESS_MODE, AccessMode.PROMPT, true);
            } catch (Throwable ex) {
                Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
            }
            superuserMode = false;
        }
        return createDefaultConsole(context, superuserMode, advancedMode);
    }

    /**
     * Method that returns a console, and creates a new console if no
     * console is allocated or if the settings preferences has changed.
     *
     * @param context The current context
     * @param superuserMode If create with a superuser mode console (root access mode)
     * @param advancedMode If create with a advanced mode console (prompt or root access mode)
     * @return Console An allocated console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     */
    //IMP! This must be invoked from the main activity creation
    public static Console createDefaultConsole(Context context,
            boolean superuserMode, boolean advancedMode)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {

        synchronized (ConsoleBuilder.SYNC) {
            //Check if console settings has changed
            if (sHolder != null) {
                if (
                    (sHolder.getConsole() instanceof NonPriviledgeConsole && superuserMode)
                    || (sHolder.getConsole() instanceof PrivilegedConsole && !superuserMode)) {
                    //Deallocate actual console
                    sHolder.dispose();
                    sHolder = null;
                }
            }

            //Is there a console allocated
            if (sHolder == null) {
                sHolder = (superuserMode)
                        ? new ConsoleHolder(createAndCheckPrivilegedConsole(context))
                        : new ConsoleHolder(createNonPrivilegedConsole(context));
                if (superuserMode) {
                    // Change also the background console to privileged
                    FileManagerApplication.changeBackgroundConsoleToPriviligedConsole();
                }
            }
            return sHolder.getConsole();
        }
    }

    /**
     * Method that destroy the current console.
     */
    public static void destroyConsole() {
        try {
            if (sHolder != null) {
                sHolder.dispose();
            }
        } catch (Exception e) {
            /**NON BLOCK**/
        }
        sHolder = null;
    }

    /**
     * Method that creates a new non privileged console.
     *
     * @param context The current context
     * @return Console The non privileged console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @see NonPriviledgeConsole
     */
    public static Console createNonPrivilegedConsole(Context context)
            throws FileNotFoundException, IOException,
            InvalidCommandDefinitionException, ConsoleAllocException {

        int bufferSize = context.getResources().getInteger(R.integer.buffer_size);

        // Is rooted? Then create a shell console
        if (FileManagerApplication.isDeviceRooted()) {
            NonPriviledgeConsole console = new NonPriviledgeConsole();
            console.setBufferSize(bufferSize);
            console.alloc();
            return console;
        }

        // No rooted. Then create a java console
        JavaConsole console = new JavaConsole(context, bufferSize);
        console.alloc();
        return console;
    }

    /**
     * Method that creates a new privileged console. If the allocation of the
     * privileged console fails, the a non privileged console
     *
     * @param context The current context
     * @return Console The privileged console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     * @see PrivilegedConsole
     */
    public static Console createPrivilegedConsole(Context context)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        PrivilegedConsole console = new PrivilegedConsole();
        console.setBufferSize(context.getResources().getInteger(R.integer.buffer_size));
        console.alloc();
        if (console.getIdentity().getUser().getId() != ROOT_UID) {
            //The console is not a privileged console
            try {
                console.dealloc();
            } catch (Throwable ex) {
                /**NON BLOCK**/
            }
            throw new InsufficientPermissionsException(null);
        }
        return console;
    }

    /**
     * Method that creates a new privileged console. If the allocation of the
     * privileged console fails, the a non privileged console
     *
     * @param context The current context
     * @return Console The privileged console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     * @see PrivilegedConsole
     */
    public static Console createAndCheckPrivilegedConsole(Context context)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        return createAndCheckPrivilegedConsole(context, true);
    }

    /**
     * Method that creates a new privileged console. If the allocation of the
     * privileged console fails, the a non privileged console
     *
     * @param context The current context
     * @param silent Indicates that no message have to be displayed
     * @return Console The privileged console
     * @throws FileNotFoundException If the initial directory not exists
     * @throws IOException If initial directory couldn't be checked
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If the console created is not a privileged console
     * @see PrivilegedConsole
     */
    public static Console createAndCheckPrivilegedConsole(
            Context context, boolean silent)
            throws FileNotFoundException, IOException, InvalidCommandDefinitionException,
            ConsoleAllocException, InsufficientPermissionsException {
        try {
            // Create the privileged console
            return createPrivilegedConsole(context);

        } catch (ConsoleAllocException caEx) {
            //Show a message with the problem?
            Log.w(TAG, context.getString(R.string.msgs_privileged_console_alloc_failed), caEx);
            if (!silent) {
                try {
                    DialogHelper.showToast(context,
                            R.string.msgs_privileged_console_alloc_failed, Toast.LENGTH_LONG);
                } catch (Exception ex) {
                    Log.e(TAG, "can't show toast", ex);  //$NON-NLS-1$
                }
            }

            boolean advancedMode =
                    FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) != 0;
            if (advancedMode) {
                //Save settings
                try {
                    Preferences.savePreference(
                            FileManagerSettings.SETTINGS_ACCESS_MODE, AccessMode.PROMPT, true);
                } catch (Exception ex) {
                    Log.e(TAG,
                            String.format("Failed to save %s property",  //$NON-NLS-1$
                            FileManagerSettings.SETTINGS_ACCESS_MODE.getId()), ex);
                }

                //Create the non-privileged console
                return createNonPrivilegedConsole(context);
            }

            // Rethrow the exception
            throw caEx;
        }
    }

    /**
     * Method that returns if the current console is a privileged console
     *
     * @return boolean If the current console is a privileged console
     */
    public static boolean isPrivileged() {
        if (sHolder != null && sHolder.getConsole() instanceof PrivilegedConsole) {
            return true;
        }
        return false;
    }

}
