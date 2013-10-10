/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.security.Authentication;

import java.io.FileInputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PGPSecurityProviderTestCase extends AbstractEncryptionStrategyTestCase
{
    private PGPSecurityProvider securityProvider;
    private Message message;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        securityProvider = new PGPSecurityProvider();
        securityProvider.setKeyManager(keyManager);
        securityProvider.initialise();

        URL url = Thread.currentThread().getContextClassLoader().getResource("./signed.asc");
        FileInputStream in = new FileInputStream(url.getFile());

        message = MessageFactory.getMessage(IOUtils.toByteArray(in));
    }

    @Override
    protected void doTearDown() throws Exception
    {
        securityProvider = null;
        message = null;
        super.doTearDown();
    }

    @Test
    public void testAuthenticate() throws Exception
    {
        Authentication auth = new PGPAuthentication("Mule client <mule_client@mule.com>", message);
        auth = securityProvider.authenticate(auth);
        assertTrue(auth.isAuthenticated());
    }
}
