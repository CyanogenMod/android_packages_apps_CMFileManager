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

package com.cyanogenmod.filemanager.themes;

import android.app.Activity;
import android.os.Bundle;

/**
 * A mock activity for allow to <code>File Manager</code> to find this themes.<br/>
 * <br/>
 * NOTE: This activity must define the next filters:<br/>
 * <ul>
 * <li>Permission: <b>com.cyanogenmod.filemanager.permissions.READ_THEME</b></li>
 * <li>Action: <b>com.cyanogenmod.filemanager.actions.MAIN_THEME</b></li>
 * <li>Category: <b>com.cyanogenmod.filemanager.categories.THEME</b></li>
 * </ul>
 */
public class ThemeActivity extends Activity {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        //Save state
        super.onCreate(state);
    }
}
