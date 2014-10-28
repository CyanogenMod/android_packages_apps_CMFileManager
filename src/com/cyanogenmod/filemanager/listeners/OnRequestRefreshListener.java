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

package com.cyanogenmod.filemanager.listeners;

/**
 * A listener for requesting an object refresh.
 */
public interface OnRequestRefreshListener {

    /**
     * Invoked when a new refresh is needed.
     *
     * @param o The object that should be refreshed
     * @param clearSelection If the refresh should clear the selection
     */
    void onRequestRefresh(Object o, boolean clearSelection);

    /**
     * Invoked when bookmarks need a refresh
     */
    void onRequestBookmarksRefresh();

    /**
     * Invoked when the object was removed.
     *
     * @param o The object that was removed
     * @param clearSelection If the refresh should clear the selection
     */
    void onRequestRemove(Object o, boolean clearSelection);

    /**
     * Invoked when the object need to navigate to.
     *
     * @param o The object where to navigate to
     */
    void onNavigateTo(Object o);
}
