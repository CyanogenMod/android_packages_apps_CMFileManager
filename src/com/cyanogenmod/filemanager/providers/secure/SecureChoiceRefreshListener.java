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

import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;

import java.io.File;

/**
 * SecureChoiceRefreshListener
 * <pre>
 *    This is just a chained callback
 * </pre>
 *
 * @see {@link com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener}
 */
/* package */ class SecureChoiceRefreshListener implements OnRequestRefreshListener {

    // Members
    private File mCacheFile;
    private ISecureChoiceCompleteListener mListener;

    /**
     * Constructor
     *
     * @param listener
     * {@link com.cyanogenmod.filemanager.providers.secure.ISecureChoiceCompleteListener}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public SecureChoiceRefreshListener(File cacheFile, ISecureChoiceCompleteListener listener)
            throws IllegalArgumentException {
        if (cacheFile == null) {
            throw new IllegalArgumentException("'cacheFile' cannot be null!");
        }
        if (listener == null) {
            throw new IllegalArgumentException("'listener' cannot be null!");
        }
        mCacheFile = cacheFile;
        mListener = listener;
    }

    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        mListener.onComplete(mCacheFile);
    }

    @Override
    public void onRequestBookmarksRefresh() {

    }

    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {

    }

    @Override
    public void onNavigateTo(Object o) {

    }

    @Override
    public void onCancel() {
        mListener.onCancelled();
    }
}
