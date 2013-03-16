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

package com.cyanogenmod.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultExecutable;
import com.cyanogenmod.filemanager.commands.ExecExecutable;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.FixedQueue;
import com.cyanogenmod.filemanager.util.FixedQueue.EmptyQueueException;

import java.util.List;

/**
 * A class that wraps a dialog for display the output generation for an
 * {@link ExecExecutable}.
 */
public class ExecutionDialog implements DialogInterface.OnClickListener {

    /**
     * @hide
     */
    final Context mContext;
    /**
     * @hide
     */
    final AlertDialog mDialog;
    /**
     * @hide
     */
    final TextView mTvOutput;
    /**
     * @hide
     */
    final TextView mTvTime;
    /**
     * @hide
     */
    final TextView mTvExitCode;

    // For cancel the operation
    private AsyncResultExecutable mCmd;
    /**
     * @hide
     */
    boolean mFinished;
    private long mStartTime;

    /**
     * @hide
     */
    final Object mSync = new Object();
    /**
     * @hide
     */
    FixedQueue<String> mQueue;

    private final int maxLines;
    private final int maxChars;

    // The drawing task
    private final AsyncTask<Void, String, Void> mConsoleDrawTask =
            new AsyncTask<Void, String, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            while (!ExecutionDialog.this.mFinished) {
                // Extract the message
                try {
                    while (!ExecutionDialog.this.mQueue.isEmpty()) {
                        // Extract all items from the queue
                        List<String> l = ExecutionDialog.this.mQueue.peekAll();
                        StringBuilder sb = new StringBuilder();
                        for (String s : l) {
                            sb.append(s);
                            sb.append("\n"); //$NON-NLS-1$
                        }

                        // Extract the message and redraw
                        publishProgress(extractMsg());

                        // Don't kill the processor
                        try {
                            Thread.yield();
                            Thread.sleep(250L);
                        } catch (Throwable _throw) {/**NON BLOCK**/}
                    }
                } catch (Exception e) {/**NON BLOCK**/}
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            drawMessage(values[0], false);
        }
    };

    /**
     * Constructor of <code>ExecutionDialog</code>.
     *
     * @param context The current context
     * @param fso The file system object to execute
     */
    public ExecutionDialog(final Context context, final FileSystemObject fso) {
        super();

        // Limits
        this.maxLines = context.getResources().getInteger(R.integer.console_max_lines);
        this.maxChars = context.getResources().getInteger(R.integer.console_max_chars_per_line);

        //Save the context
        this.mContext = context;
        this.mFinished = false;
        this.mQueue = new FixedQueue<String>(this.maxLines);

        //Create the layout
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup layout = (ViewGroup)li.inflate(R.layout.execution_dialog, null);
        TextView tvScriptName = (TextView)layout.findViewById(R.id.execution_script_name);
        tvScriptName.setText(fso.getFullPath());
        this.mTvTime = (TextView)layout.findViewById(R.id.execution_time);
        this.mTvTime.setText("-"); //$NON-NLS-1$
        this.mTvExitCode = (TextView)layout.findViewById(R.id.execution_exitcode);
        this.mTvExitCode.setText("-"); //$NON-NLS-1$
        this.mTvOutput = (TextView)layout.findViewById(R.id.execution_output);
        this.mTvOutput.setMovementMethod(new ScrollingMovementMethod());

        // Apply the theme
        applyTheme(context, layout);

        //Create the dialog
        String title = context.getString(R.string.execution_console_title);
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        0,
                                        title,
                                        layout);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(android.R.string.cancel), this);

        // Start the drawing task
        this.mConsoleDrawTask.execute();
    }

    /**
     * Method that sets the console
     * @param cmd the mCmd to set
     */
    public void setCmd(AsyncResultExecutable cmd) {
        this.mCmd = cmd;

        // Enable cancel the script after 3 seconds.
        this.mTvOutput.postDelayed(new Runnable() {
            @Override
            public void run() {
                ExecutionDialog.this.mDialog.setCancelable(true);
                ExecutionDialog.this.mDialog.getButton(
                        DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
            }
        }, 5000L);
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);
        this.mDialog.setCancelable(false);
        this.mDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
    }

    /**
     * Method that dismiss the dialog.
     */
    public void dismiss() {
        this.mDialog.dismiss();
    }

    /**
     * Method invoked when the execution starts
     */
    public void onStart() {
        // Initialize execution
        this.mStartTime = System.currentTimeMillis();
    }

    /**
     * Method invoked when the execution ends
     *
     * @param exitCode The exit code of the execution
     */
    public void onEnd(final int exitCode) {
        // Cancel the drawing task
        try {
            this.mFinished = true;
            this.mConsoleDrawTask.cancel(false);
        } catch (Exception e) {/**NON BLOK**/}

        long endTime = System.currentTimeMillis();
        final String diff = String.valueOf((endTime - this.mStartTime) / 1000);

        // Enable the ok button
        this.mTvOutput.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Draw the data one more time, and clean the queue (no more needed)
                    drawMessage(extractMsg(), true);
                    ExecutionDialog.this.mQueue.removeAll();
                } catch (EmptyQueueException eqex) {/**NON BLOCK**/}

                // Set the time and exit code
                ExecutionDialog.this.mTvTime.setText(
                        ExecutionDialog.this.mContext.getString(
                                R.string.execution_console_script_execution_time_text, diff));
                ExecutionDialog.this.mTvExitCode.setText(String.valueOf(exitCode));

                // Enable the Ok button
                ExecutionDialog.this.mDialog.setCancelable(true);
                Button button =
                        ExecutionDialog.this.mDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                button.setText(R.string.ok);
                button.setEnabled(true);
            }
        });
    }

    /**
     * Method that append data to the console output
     *
     * @param msg The message to append
     */
    public void onAppendData(final String msg) {
        if (msg != null && msg.length() > 0) {
            // Split the messages in lines
            String[] lines = msg.split("\n"); //$NON-NLS-1$
            for (String line : lines) {
                // Don't allow lines with more that x characters
                if (line.length() > this.maxChars) {
                    while (line.length() > this.maxChars) {
                        String partial = line.substring(0, Math.min(line.length(), this.maxChars));
                        line = line.substring(Math.min(line.length(), this.maxChars));
                        // The partial
                        this.mQueue.insert(partial);
                    }
                    if (line.length() > 0) {
                        // Insert the rest of the line
                        this.mQueue.insert(line);
                    }
                } else {
                    // Insert the line
                    this.mQueue.insert(line);
                }
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                // Cancel the program?
                try {
                    if (this.mCmd != null && !this.mFinished) {
                        if (this.mCmd.isCancellable() && !this.mCmd.isCancelled()) {
                            this.mCmd.cancel();
                        }
                    }
                } catch (Exception e) {/**NON BLOCK**/}
                this.mDialog.dismiss();
                break;

            default:
                break;
        }
    }

    /**
     * Method that extracts the message from the queue
     *
     * @param String The message
     * @throws EmptyQueueException If there are not message in the queue
     * @hide
     */
    String extractMsg() throws EmptyQueueException {
        // Extract all items from the queue
        List<String> l = this.mQueue.peekAll();
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s);
            sb.append("\n"); //$NON-NLS-1$
        }
        return sb.toString();
    }

    /**
     * Method that draw the message
     *
     * @param msg The message
     * @param scroll Scroll to bottom
     * @hide
     */
    void drawMessage(String msg, boolean scroll) {
        // Any message?
        if (msg != null && msg.length() > 0) {
            final TextView tv = ExecutionDialog.this.mTvOutput;
            tv.setText(msg);

            // Scroll to bottom
            if (scroll) {
                final Layout layout = tv.getLayout();
                if (layout != null) {
                    int scrollDelta =
                            layout.getLineBottom(
                                    tv.getLineCount() - 1) - tv.getScrollY() - tv.getHeight();
                    if (scrollDelta > 0) {
                        tv.scrollBy(0, scrollDelta);
                    }
                }
            } else {
                tv.scrollBy(0, 0);
            }
        }
    }

    /**
     * Method that applies the current theme to the dialog
     *
     * @param ctx The current context
     * @param root The root view
     */
    private void applyTheme(Context ctx, ViewGroup root) {
        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(ctx);
        theme.setBackgroundDrawable(ctx, root, "background_drawable"); //$NON-NLS-1$
        View v = root.findViewById(R.id.execution_time_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.execution_script_name);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.execution_time_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        theme.setTextColor(ctx, this.mTvTime, "text_color"); //$NON-NLS-1$
        v = root.findViewById(R.id.execution_exitcode_label);
        theme.setTextColor(ctx, (TextView)v, "text_color"); //$NON-NLS-1$
        theme.setTextColor(ctx, this.mTvExitCode, "text_color"); //$NON-NLS-1$
        theme.setBackgroundColor(ctx, this.mTvOutput, "console_bg_color"); //$NON-NLS-1$
        theme.setTextColor(ctx, this.mTvOutput, "console_fg_color"); //$NON-NLS-1$
    }

}
