/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Test;

public class PGPKeyRingTestCase extends AbstractEncryptionStrategyTestCase
{
    @Test
    public void testClientKey() {
        PGPPublicKey clientKey = keyManager.getPublicKey("Mule client <mule_client@mule.com>");
        assertNotNull(clientKey);
    }

    @Test
    public void testServerKey()
    {
        PGPSecretKey serverKey = keyManager.getConfiguredSecretKey();
        assertNotNull(serverKey);
    }

    @Test
    public void testSubKeyIsToken() throws Exception
    {
        String publicSubKey = "3879972755627455806"; // This is the value of the subkey in decimal.
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL urlPublicKey = loader.getResource("./mule.gpg");
        ((PGPKeyRingImpl) keyManager).setPublicKeyRingFileName(urlPublicKey.getFile());
        ((PGPKeyRingImpl) keyManager).initialise();
        Long resultKeyId = keyManager.getPublicKey("fernando.federico (Testing pgp) <fernando.federico@mulesoft.com>").getKeyID();
        assertThat(publicSubKey, is(resultKeyId.toString()));
    }
}
