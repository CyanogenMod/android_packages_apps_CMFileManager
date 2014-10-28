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

package com.cyanogenmod.filemanager.console;

import java.io.IOException;

/**
 * An exception that indicates that the operation failed because an authentication failure
 */
public class AuthenticationFailedException extends IOException {
    private static final long serialVersionUID = -2199496556437722726L;

    /**
     * Constructor of <code>AuthenticationFailedException</code>.
     *
     * @param msg The associated message
     */
    public AuthenticationFailedException(String msg) {
        super(msg);
    }

}
