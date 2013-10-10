/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;

public interface PGPKeyRing
{
    public abstract String getSecretPassphrase();

    public abstract PGPSecretKey getSecretKey();

    public abstract PGPPublicKey getPublicKey(String principalId);
}
