/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.cyanogenmod.filemanager.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import com.cyanogenmod.filemanager.providers.MimeTypeIndexProvider;

/**
 * FetchStatsByTypeTask
 * <pre>
 *     Task for fetching the cursor of data for stats by mime type
 * </pre>
 *
 * @see {@link android.os.AsyncTask}
 */
public class FetchStatsByTypeTask extends AsyncTask<String, Void, Cursor> {

    // Members
    private Context mContext;
    private Listener mListener;

    /**
     * Constructor
     *
     * @param context  {@link android.content.Context}
     * @param listener {@link com.cyanogenmod.filemanager.tasks.FetchStatsByTypeTask.Listener}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public FetchStatsByTypeTask(Context context, Listener listener)
            throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null");
        }
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Cursor doInBackground(String... strings) {
        if (strings.length < 1) {
            return null;
        }
        String fileRoot = strings[0];
        return MimeTypeIndexProvider.getMountPointUsage(mContext, fileRoot);
    }

    @Override
    protected void onPostExecute(Cursor cursor) {
        if (mListener != null) {
            mListener.onCursor(cursor);
        }
    }

    /**
     * Callback interface for this task
     */
    public interface Listener {
        public void onCursor(Cursor cursor);
    }

}
