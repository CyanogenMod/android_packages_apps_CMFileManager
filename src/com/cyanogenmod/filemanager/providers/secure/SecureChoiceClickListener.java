/*
* Copyright (C) 2015 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.providers.secure;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy.LinkedResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * SecureChoiceClickListener
 * <pre>
 *    This listens for the secure choice user selection
 * </pre>
 *
 * @see {@link android.content.DialogInterface.OnClickListener}
 */
public class SecureChoiceClickListener implements DialogInterface.OnClickListener {

    // Constants
    /* package */ static final String CACHE_DIR = ".opened-files";

    // Members
    private Context mContext;
    private FileSystemObject mFso;
    private ISecureChoiceCompleteListener mListener;

    /**
     * Constructor
     *
     * @param context {@link android.content.Context}
     * @param fso {@link com.cyanogenmod.filemanager.model.FileSystemObject}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public SecureChoiceClickListener(Context context, FileSystemObject fso,
            ISecureChoiceCompleteListener listener) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        if (fso == null) {
            throw new IllegalArgumentException("'fso' cannot be null!");
        }
        if (listener == null) {
            throw new IllegalArgumentException("'listener' cannot be null!");
        }
        mContext = context;
        mFso = fso;
        mListener = listener;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                performUserFlow();
                break;
            default:
                break;
        }
    }

    private void performUserFlow() {
        List<LinkedResource> selection = new ArrayList<LinkedResource>();
        File hiddenCacheDirectory = new File(mContext.getExternalCacheDir(), CACHE_DIR);
        // Check if the hidden directory exists
        if (!hiddenCacheDirectory.exists()) {
            hiddenCacheDirectory.mkdirs();
        }
        final File tmpFso = new File(hiddenCacheDirectory, mFso.getName());
        selection.add(new LinkedResource(new File(mFso.getFullPath()), tmpFso));
        CopyMoveActionPolicy.copyFileSystemObjects(mContext, selection,
                new SecureChoiceSelectionListener(tmpFso),
                new SecureChoiceRefreshListener(tmpFso, mListener));

    }
}
