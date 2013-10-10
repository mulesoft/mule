/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PGPKeyRingTestCase extends AbstractEncryptionStrategyTestCase
{
    @Test
    public void testClientKey()
    {
        PGPPublicKey clientKey = keyManager.getPublicKey("Mule client <mule_client@mule.com>");
        assertNotNull(clientKey);
    }

    @Test
    public void testServerKey()
    {
        PGPSecretKey serverKey = keyManager.getSecretKey();
        assertNotNull(serverKey);
    }
}
