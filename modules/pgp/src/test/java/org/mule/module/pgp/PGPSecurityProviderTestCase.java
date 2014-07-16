/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
