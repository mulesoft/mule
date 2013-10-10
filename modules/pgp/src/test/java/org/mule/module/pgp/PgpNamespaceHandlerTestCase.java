/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PgpNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "pgp-namespace-config.xml";
    }

    @Test
    public void testPgpProvider()
    {
        knownProperties(getProvider("pgpSecurityProvider"));
    }
    
    @Test
    public void testKeyBasedEncryptionStrategy()
    {
        knownProperties(getEncryptionStrategy("keyBasedEncryptionStrategy"));
    }

    protected SecurityProvider getProvider(String name)
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        return securityManager.getProvider(name);
    }

    protected void knownProperties(SecurityProvider provider)
    {
        assertNotNull(provider);
        assertTrue(provider instanceof PGPSecurityProvider);
        PGPSecurityProvider pgpProvider = (PGPSecurityProvider) provider;
        assertNotNull(pgpProvider.getKeyManager());
        assertTrue(pgpProvider.getKeyManager() instanceof PGPKeyRingImpl);
    }
    
    protected EncryptionStrategy getEncryptionStrategy(String name)
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        return securityManager.getEncryptionStrategy(name);
    }
    
    protected void knownProperties(EncryptionStrategy encryptionStrategy)
    {
        assertNotNull(encryptionStrategy);
        assertTrue(encryptionStrategy instanceof KeyBasedEncryptionStrategy);
        KeyBasedEncryptionStrategy keyBased = (KeyBasedEncryptionStrategy) encryptionStrategy;
        assertNotNull(keyBased.getKeyManager());
        assertTrue(keyBased.getKeyManager() instanceof PGPKeyRingImpl);
    }

}
