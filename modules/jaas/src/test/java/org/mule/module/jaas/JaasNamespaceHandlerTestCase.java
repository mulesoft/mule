/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JaasNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jaas-namespace-config.xml";
    }

    @Test
    public void testJaasProvider()
    {
        knownProperties(getProvider("jaasSecurityProvider"));
    }

    @Test
    public void testPasswordBasedEncryption()
    {
        knownProperties(getEncryptionStrategy("PBE"));
    }

    protected void knownProperties(EncryptionStrategy encryptionStrategy)
    {
        assertNotNull(encryptionStrategy);
        assertTrue(encryptionStrategy instanceof PasswordBasedEncryptionStrategy);
        PasswordBasedEncryptionStrategy pbe = (PasswordBasedEncryptionStrategy) encryptionStrategy;
        assertNotNull(pbe.getName());
        assertEquals("PBE", pbe.getName());
    }

    protected void knownProperties(SecurityProvider provider)
    {
        assertNotNull(provider);
        assertTrue(provider instanceof JaasSimpleAuthenticationProvider);
        JaasSimpleAuthenticationProvider jaasProvider = (JaasSimpleAuthenticationProvider) provider;
        assertNotNull(jaasProvider.getLoginContextName());
        assertEquals("jaasTest", jaasProvider.getLoginContextName());
        assertNotNull(jaasProvider.getLoginConfig());
        assertEquals("jaas.conf", jaasProvider.getLoginConfig());
    }

    protected SecurityProvider getProvider(String providerName)
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        return securityManager.getProvider(providerName);
    }

    protected EncryptionStrategy getEncryptionStrategy(String strategyName)
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        return securityManager.getEncryptionStrategy(strategyName);
    }
}
