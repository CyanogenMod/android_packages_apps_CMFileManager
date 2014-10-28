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

import com.cyanogenmod.filemanager.model.Bookmark;

/**
 * A helper class with useful methods for deal with bookmarks.
 */
public final class BookmarksHelper {

    /**
     * Constructor of <code>BookmarksHelper</code>.
     */
    private BookmarksHelper() {
        super();
    }

    /**
     * Method that returns the associated icon to the bookmark.
     *
     * @param bookmark The bookmark
     * @return String The associated icon resource name
     */
    public static String getIcon(Bookmark bookmark) {
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.HOME) == 0) {
            return "ic_home_drawable"; //$NON-NLS-1$
        }
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.FILESYSTEM) == 0) {
            return "ic_filesystem_drawable"; //$NON-NLS-1$
        }
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.SDCARD) == 0) {
            return "ic_sdcard_drawable"; //$NON-NLS-1$
        }
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.USB) == 0) {
            return "ic_usb_drawable"; //$NON-NLS-1$
        }
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.SECURE) == 0) {
            return "ic_secure_drawable"; //$NON-NLS-1$
        }
        if (bookmark.mType.compareTo(Bookmark.BOOKMARK_TYPE.REMOTE) == 0) {
            return "ic_remote_drawable"; //$NON-NLS-1$
        }
        //Bookmark add by the user
        return "ic_user_defined_bookmark_drawable"; //$NON-NLS-1$
    }
}
