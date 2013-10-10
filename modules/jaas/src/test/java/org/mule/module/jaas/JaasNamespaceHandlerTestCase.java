/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jaas;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JaasNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
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
