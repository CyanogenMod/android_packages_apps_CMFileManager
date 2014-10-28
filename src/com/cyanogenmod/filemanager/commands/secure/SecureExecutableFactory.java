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

package com.cyanogenmod.filemanager.commands.secure;

import com.cyanogenmod.filemanager.commands.ExecutableCreator;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
/**
 * A class that represents a factory for creating java {@link "Executable"} objects.
 */
public class SecureExecutableFactory extends ExecutableFactory {

    private final SecureConsole mConsole;

    /**
     * Constructor of <code>SecureExecutableFactory</code>.
     *
     * @param console A secure console that use for create objects
     */
    public SecureExecutableFactory(SecureConsole console) {
        super();
        this.mConsole = console;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutableCreator newCreator() {
        return new SecureExecutableCreator(this.mConsole);
    }

}
