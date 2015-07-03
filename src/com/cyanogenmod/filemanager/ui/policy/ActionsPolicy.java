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

package com.cyanogenmod.filemanager.ui.policy;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Spanned;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.dialogs.MessageProgressDialog;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;


/**
 * A class with the convenience methods for resolve actions
 */
public abstract class ActionsPolicy {

    /**
     * An interface for using in conjunction with AsyncTask for have
     * a
     */
    protected interface BackgroundCallable {
        /**
         * Method that returns the resource identifier of the icon of the dialog
         *
         * @return int The resource identifier of the icon of the dialog
         */
        int getDialogIcon();

        /**
         * Method that returns the resource identifier of the title of the dialog
         *
         * @return int The resource identifier of the title of the dialog
         */
        int getDialogTitle();

        /**
         * Method that returns if the dialog is cancellable
         *
         * @return boolean If the dialog is cancellable
         */
        boolean isDialogCancellable();

        /**
         * Method invoked when need to update the progress of the dialog
         *
         * @return Spanned The text to show in the progress
         */
        Spanned requestProgress();

        /**
         * The method where the operation is done in background
         *
         * @param params The parameters
         * @throws Throwable If the operation failed, must be launch and exception
         */
        void doInBackground(Object... params) throws Throwable;

        /**
         * Method invoked when the operation was successfully
         */
        void onSuccess();

        /**
         * Method invoked to handle cancelling
         */
        void onCancel();

    }

    /**
     * A task class for run operations in the background. It uses a dialog while
     * perform the operation.
     *
     * @see BackgroundCallable
     */
    protected static class BackgroundAsyncTask
            extends AsyncTask<Object, Spanned, Throwable> {

        private final Context mCtx;
        private final BackgroundCallable mCallable;
        private MessageProgressDialog mDialog;

        /**
         * Constructor of <code>BackgroundAsyncTask</code>
         *
         * @param ctx The current context
         * @param callable The {@link BackgroundCallable} interface
         */
        public BackgroundAsyncTask(Context ctx, BackgroundCallable callable) {
            super();
            this.mCtx = ctx;
            this.mCallable = callable;
        }

        @Override
        protected void onPreExecute() {
            // Create the waiting dialog while doing some stuff on background
            final BackgroundAsyncTask task = this;
            this.mDialog = new MessageProgressDialog(
                    this.mCtx,
                    this.mCallable.getDialogIcon(),
                    this.mCallable.getDialogTitle(),
                    R.string.waiting_dialog_msg,
                    this.mCallable.isDialogCancellable());
            this.mDialog.setOnCancelListener(new MessageProgressDialog.OnCancelListener() {
                @Override
                public boolean onCancel() {
                    mCallable.onCancel();
                    task.cancel(true);
                    return true;
                }
            });
            Spanned progress = this.mCallable.requestProgress();
            this.mDialog.setProgress(progress);
            this.mDialog.show();
        }

        @Override
        protected Throwable doInBackground(Object... params) {
            try {
                this.mCallable.doInBackground(params);

                // Success
                return null;

            } catch (Throwable ex) {
                // Capture the exception
                return ex;
            }
        }

        @Override
        protected void onPostExecute(Throwable result) {
            // Close the waiting dialog
            this.mDialog.dismiss();

            // Check the result (no relaunch, this is responsibility of callable doInBackground)
            if (result != null) {
                ExceptionUtil.translateException(this.mCtx, result, false, false);
            } else {
                //Operation complete.
                this.mCallable.onSuccess();
            }
        }

        @Override
        protected void onCancelled() {
            this.mCallable.onCancel();
        }

        @Override
        protected void onProgressUpdate(Spanned... values) {
            this.mDialog.setProgress(values[0]);
        }

        /**
         * @hide
         */
        void onRequestProgress() {
            Spanned mProgress = this.mCallable.requestProgress();
            publishProgress(mProgress);
        }
    }

    /**
     * Method that shows a message when the operation is complete successfully
     *
     * @param ctx The current context
     * @hide
     */
    protected static void showOperationSuccessMsg(Context ctx) {
        DialogHelper.showToast(ctx, R.string.msgs_success, Toast.LENGTH_SHORT);
    }
}
