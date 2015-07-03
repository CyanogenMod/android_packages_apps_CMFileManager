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

import com.cyanogenmod.filemanager.listeners.OnSelectionListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;

import java.io.File;
import java.util.List;

/**
 * SecureChoiceSelectionListener
 * <pre>
 *     This is something that the copy/move pipeline needs in order to
 *     pass a conditional check in {@link com.cyanogenmod.filemanager.ui.policy
 *     .CopyMoveActionPolicy}
 * </pre>
 *
 * @see {@link com.cyanogenmod.filemanager.listeners.OnSelectionListener}
 */
/* package */ class SecureChoiceSelectionListener implements OnSelectionListener {

    // Members
    private File mFile;

    /**
     * Constructor
     *
     * @param fso {@link java.io.File}
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public SecureChoiceSelectionListener(File fso) throws IllegalArgumentException {
        if (fso == null) {
            throw new IllegalArgumentException("'fso' cannot be null!");
        }
        mFile = fso;
    }

    @Override
    public void onToggleSelection(FileSystemObject fso) {

    }

    @Override
    public void onDeselectAll() {

    }

    @Override
    public void onSelectAllVisibleItems() {

    }

    @Override
    public void onDeselectAllVisibleItems() {

    }

    @Override
    public List<FileSystemObject> onRequestSelectedFiles() {
        return null;
    }

    @Override
    public List<FileSystemObject> onRequestCurrentItems() {
        return null;
    }

    @Override
    public String onRequestCurrentDir() {
        return mFile.getParent();
    }

}
