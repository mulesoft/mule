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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.module.pgp.i18n.PGPMessages.ambiguousPGPPrincipalExceptionMessage;

import org.mule.api.lifecycle.InitialisationException;

import java.net.URL;

public class PGPKeyRingTestCase extends AbstractEncryptionStrategyTestCase
{
    @Test
    public void testClientKey()
    {
        PGPPublicKey clientKey = keyManager.getPublicKey("Mule client <mule_client@mule.com>");
        assertNotNull(clientKey);
    }

    @Test
    public void testServerKey() throws Exception
    {
        PGPSecretKey serverKey = keyManager.getSecretKey();
        assertNotNull(serverKey);
    }

    @Test
    public void testDuplicatePrincipal() throws Exception
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource("./duplicatePrincipal.gpg");
        ((PGPKeyRingImpl) keyManager).setPublicKeyRingFileName(url.getFile());
        try
        {
            ((PGPKeyRingImpl) keyManager).initialise();
        }
        catch(InitialisationException initialisationException)
        {
            String expectedMessage = ambiguousPGPPrincipalExceptionMessage("Mule duplicate (duplicate userId) <mule_duplicate@mule.com>", "B6FD90CC2F993364", "DF34CC5CDB3360F3").getMessage();
            assertThat(initialisationException.getMessage(), is(expectedMessage));
        }
    }
}
