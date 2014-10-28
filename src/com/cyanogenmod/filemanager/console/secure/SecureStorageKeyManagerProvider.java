/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.console.secure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.AbstractKeyManagerProvider;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.PromptingKeyProvider;

/**
 * The SecureStorage KeyManager provider
 */
public class SecureStorageKeyManagerProvider extends AbstractKeyManagerProvider {

    /** The singleton instance of this class. */
    static final SecureStorageKeyManagerProvider SINGLETON =
            new SecureStorageKeyManagerProvider();

    private final static SecureStorageKeyPromptDialog PROMPT_DIALOG =
            new SecureStorageKeyPromptDialog();

    /** You cannot instantiate this class. */
    private SecureStorageKeyManagerProvider() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return Boot.MANAGERS;
    }

    /**
     * @hide
     */
    void unmount() {
        PROMPT_DIALOG.umount();
        getKeyProvider().setKey(null);
    }

    /**
     * @hide
     */
    void reset() {
        PROMPT_DIALOG.reset();
        getKeyProvider().setKey(null);
    }

    /**
     * @hide
     */
    void drop() {
        PROMPT_DIALOG.drop();
        getKeyProvider().setKey(null);
    }

    @SuppressWarnings("unchecked")
    private PromptingKeyProvider<AesCipherParameters> getKeyProvider() {
        PromptingKeyManager<AesCipherParameters> keyManager =
                (PromptingKeyManager<AesCipherParameters>)get().get(AesCipherParameters.class);
         return (PromptingKeyProvider<AesCipherParameters>) keyManager.getKeyProvider(
                 SecureConsole.getSecureStorageRootUri());
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<Class<?>, KeyManager<?>> MANAGERS;
        static {
            final Map<Class<?>, KeyManager<?>> fast = new LinkedHashMap<Class<?>, KeyManager<?>>();
            fast.put(AesCipherParameters.class,
                    new PromptingKeyManager<AesCipherParameters>(PROMPT_DIALOG));
            MANAGERS = Collections.unmodifiableMap(fast);
        }
    } // class Boot

}
