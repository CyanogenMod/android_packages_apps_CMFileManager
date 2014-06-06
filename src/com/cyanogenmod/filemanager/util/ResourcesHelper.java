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

import android.content.res.Resources;

import os.ransj.filemanager.R;

import java.lang.reflect.Field;

/**
 * A helper class with useful methods for deal with resources.
 */
public final class ResourcesHelper {

    /**
     * Constructor of <code>ResourcesHelper</code>.
     */
    private ResourcesHelper() {
        super();
    }

    /**
     * Method that retrieves the identifier of the resource from his string name.
     *
     * @param res The resources reference
     * @param type The type of resource (drawable, string, ...)
     * @param id The identifier of the resource
     * @return The identifier, or 0 if the resource was not located
     */
    public static int getIdentifier(Resources res, String type, String id) {
        try {
            Class<?> typeClazz =
                    Class.forName(
                            String.format("%s$%s", R.class.getCanonicalName(), type)); //$NON-NLS-1$
            Field idFld = typeClazz.getField(id);
            return idFld.getInt(null);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        return 0;
    }
}
