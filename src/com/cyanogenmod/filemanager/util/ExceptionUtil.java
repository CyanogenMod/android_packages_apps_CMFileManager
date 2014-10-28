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

package com.cyanogenmod.filemanager.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.SyncResultExecutable;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.AuthenticationFailedException;
import com.cyanogenmod.filemanager.console.CancelledOperationException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.console.ReadOnlyFilesystemException;
import com.cyanogenmod.filemanager.console.RelaunchableException;
import com.cyanogenmod.filemanager.preferences.AccessMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.List;

/**
 * A helper class with useful methods for deal with exceptions.
 */
public final class ExceptionUtil {

    /**
     * An interface to communicate events related with the result of a command relaunch.
     */
    public interface OnRelaunchCommandResult {
        /**
         * Method invoked when the relaunch operation was success
         */
        void onSuccess();

        /**
         * Method invoked when the relaunch operation was cancelled by the user
         */
        void onCancelled();

        /**
         * Method invoked when the relaunch operation was failed
         *
         * @param cause The cause of the failed operation
         */
        void onFailed(Throwable cause);
    }

    /**
     * Constructor of <code>ExceptionUtil</code>.
     */
    private ExceptionUtil() {
        super();
    }

    //Definition of known exceptions and his representation mode and resource identifiers
    private static final Class<?>[] KNOWN_EXCEPTIONS = {
                                                FileNotFoundException.class,
                                                IOException.class,
                                                InvalidCommandDefinitionException.class,
                                                ConsoleAllocException.class,
                                                NoSuchFileOrDirectory.class,
                                                ReadOnlyFilesystemException.class,
                                                InsufficientPermissionsException.class,
                                                CommandNotFoundException.class,
                                                OperationTimeoutException.class,
                                                ExecutionException.class,
                                                ParseException.class,
                                                ActivityNotFoundException.class,
                                                AuthenticationFailedException.class
                                                      };
    private static final int[] KNOWN_EXCEPTIONS_IDS = {
                                                R.string.msgs_file_not_found,
                                                R.string.msgs_io_failed,
                                                R.string.msgs_command_not_found,
                                                R.string.msgs_console_alloc_failure,
                                                R.string.msgs_file_not_found,
                                                R.string.msgs_read_only_filesystem,
                                                R.string.msgs_insufficient_permissions,
                                                R.string.msgs_command_not_found,
                                                R.string.msgs_operation_timeout,
                                                R.string.msgs_operation_failure,
                                                R.string.msgs_operation_failure,
                                                R.string.msgs_not_registered_app,
                                                0
                                                     };
    private static final boolean[] KNOWN_EXCEPTIONS_TOAST = {
                                                            false,
                                                            false,
                                                            false,
                                                            false,
                                                            false,
                                                            true,
                                                            true,
                                                            false,
                                                            true,
                                                            true,
                                                            true,
                                                            false,
                                                            true
                                                            };

    /**
     * Method that attach a asynchronous task for executing when exception need
     * to be re-executed.
     *
     * @param ex The exception
     * @param task The task
     * @see RelaunchableException
     */
    public static void attachAsyncTask(Throwable ex, AsyncTask<Object, Integer, Boolean> task) {
        if (ex instanceof RelaunchableException) {
            ((RelaunchableException)ex).setTask(task);
        }
    }

    /**
     * Method that captures and translate an exception, showing a
     * toast or a alert, according to the importance.
     *
     * @param context The current context
     * @param ex The exception
     */
    public static synchronized void translateException(
            final Context context, Throwable ex) {
        translateException(context, ex, false, true);
    }

    /**
     * Method that captures and translate an exception, showing a
     * toast or a alert, according to the importance.
     *
     * @param context The current context.
     * @param ex The exception
     * @param quiet Don't show UI messages
     * @param askUser Ask the user when if the exception could be relaunched with other privileged
     */
    public static synchronized void translateException(
            final Context context, final Throwable ex,
            final boolean quiet, final boolean askUser) {
        translateException(context, ex, quiet, askUser, null);
    }

    /**
     * Method that captures and translate an exception, showing a
     * toast or a alert, according to the importance.
     *
     * @param context The current context.
     * @param ex The exception
     * @param quiet Don't show UI messages
     * @param askUser Ask the user when if the exception could be relaunched with other privileged
     * @param listener The listener where return the relaunch result
     */
    public static synchronized void translateException(
            final Context context, final Throwable ex,
            final boolean quiet, final boolean askUser,
            final OnRelaunchCommandResult listener) {

        // Is cancellable?
        if (ex instanceof CancelledOperationException) {
            return;
        }

        //Get the appropriate message for the exception
        int msgResId = R.string.msgs_unknown;
        boolean toast = true;
        int cc = KNOWN_EXCEPTIONS.length;
        for (int i = 0; i < cc; i++) {
            if (KNOWN_EXCEPTIONS[i].getCanonicalName().compareTo(
                    ex.getClass().getCanonicalName()) == 0) {
                msgResId = KNOWN_EXCEPTIONS_IDS[i];
                toast = KNOWN_EXCEPTIONS_TOAST[i];
                break;
            }
        }

        //Check exceptions that can be asked to user
        if (ex instanceof RelaunchableException && askUser) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    askUser(context, (RelaunchableException)ex, quiet, listener);
                }
            });
            return;
        }

        //Audit the exception
        Log.e(context.getClass().getSimpleName(), "Error detected", ex); //$NON-NLS-1$

        //Build the alert
        final int fMsgResId = msgResId;
        final boolean fToast = toast;
        if (!quiet) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String msg = null;
                        if (fMsgResId > 0) {
                            msg = context.getString(fMsgResId);
                        } else {
                            msg = ex.getMessage();
                        }
                        if (fToast) {
                            DialogHelper.showToast(context, msg, Toast.LENGTH_SHORT);
                        } else {
                            AlertDialog dialog =
                                    DialogHelper.createErrorDialog(
                                            context, R.string.error_title, msg);
                            DialogHelper.delegateDialogShow(context, dialog);
                        }
                    } catch (Exception e) {
                        Log.e(context.getClass().getSimpleName(),
                                "ExceptionUtil. Failed to show dialog", ex); //$NON-NLS-1$
                    }
                }
            });
        }
    }

    /**
     * Method that ask the user for an operation and re-execution of the command.
     *
     * @param context The current context
     * @param relaunchable The exception that contains the command that must be re-executed.
     * @param listener The listener where return the relaunch result
     * @hide
     */
    static void askUser(
            final Context context,
            final RelaunchableException relaunchable,
            final boolean quiet,
            final OnRelaunchCommandResult listener) {

        //Is privileged?
        boolean isPrivileged = false;
        try {
            isPrivileged = ConsoleBuilder.getConsole(context).isPrivileged();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        // If console is privileged there is not need to change
        // If we are in a ChRooted environment, resolve the error without doing anymore
        if (relaunchable instanceof InsufficientPermissionsException &&
                (isPrivileged ||
                 FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0)) {
            translateException(
                    context, relaunchable, quiet, false, null);

            // Operation failed
            if (listener != null) {
                listener.onFailed(relaunchable);
            }
            return;
        }

        //Create a yes/no dialog and ask the user
        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    //Run the executable again
                    try {
                        //Prepare the system before re-launch the command
                        prepare(context, relaunchable);

                        //Re-execute the command
                        List<SyncResultExecutable> executables = relaunchable.getExecutables();
                        int cc = executables.size();
                        for (int i = 0; i < cc; i++) {
                            SyncResultExecutable executable = executables.get(i);
                            Object result = CommandHelper.reexecute(context, executable, null);
                            AsyncTask<Object, Integer, Boolean> task = relaunchable.getTask();
                            if (task != null && task.getStatus() != AsyncTask.Status.RUNNING) {
                                task.execute(result);
                            }
                        }

                        // Operation complete
                        if (listener != null) {
                            listener.onSuccess();
                        }

                    } catch (Throwable ex) {
                        //Capture the exception, this time in quiet mode, if the
                        //exception is the same
                        boolean ask = ex.getClass().getName().compareTo(
                                relaunchable.getClass().getName()) == 0;
                        translateException(context, ex, quiet, !ask, listener);

                        // Operation failed
                        if (listener != null) {
                            listener.onFailed(ex);
                        }
                    }
                } else {
                    // Operation cancelled
                    if (listener != null) {
                        listener.onCancelled();
                    }
                }
            }
        };

        AlertDialog alert = DialogHelper.createYesNoDialog(
                    context,
                    R.string.confirm_operation,
                    relaunchable.getQuestionResourceId(),
                    clickListener);

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Operation cancelled
                if (listener != null) {
                    listener.onCancelled();
                }
            }
        });
        DialogHelper.delegateDialogShow(context, alert);
    }

    /**
     * Method that prepares the system for re-execute the command.
     *
     * @param context The current context
     * @param relaunchable The {@link RelaunchableException} reference
     * @hide
     */
    static void prepare(final Context context, final RelaunchableException relaunchable) {
        //- This exception need change the console before re-execute
        if (relaunchable instanceof InsufficientPermissionsException) {
            ConsoleBuilder.changeToPrivilegedConsole(context);
        }
    }

    /**
     * Method that prints the exception to an string
     *
     * @param cause The exception
     * @return String The stack trace in an string
     */
    public static String toStackTrace(Exception cause) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            cause.printStackTrace(pw);
            return sw.toString();
        } finally {
            try {
                pw.close();
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }
}
