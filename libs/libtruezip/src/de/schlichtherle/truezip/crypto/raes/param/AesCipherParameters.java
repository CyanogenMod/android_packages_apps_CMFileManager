/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes.param;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.pbe.SafePbeParameters;

/**
 * A JavaBean which holds AES cipher parameters.
 *
 * @author  Christian Schlichtherle
 */
public final class AesCipherParameters
extends SafePbeParameters<KeyStrength, AesCipherParameters> {

    public AesCipherParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(KeyStrength.BITS_256);
    }

    @Override
    public KeyStrength[] getKeyStrengthValues() {
        return KeyStrength.values();
    }
}