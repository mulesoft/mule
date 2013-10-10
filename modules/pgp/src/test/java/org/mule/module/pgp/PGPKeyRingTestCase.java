/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
