/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

public interface PGPKeyRing
{
    String getSecretPassphrase();

    /**
     * @return the PGPSecretKey obtained from the secretAliasId if it was defined. Otherwise, null.
     * @throws Exception
     */
    PGPSecretKey getConfiguredSecretKey() throws Exception;

    PGPPublicKey getPublicKey(String principalId);

    /**
     * @return all the secretKeys found in the secretKeyFile.
     */
    PGPSecretKeyRingCollection getSecretKeys ();
}
