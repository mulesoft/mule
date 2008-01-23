/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.tck.FunctionalTestCase;

public class PgpNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "pgp-namespace-config.xml";
    }

    public void testPgpProvider()
    {
        knownProperties(getProvider("pgpSecurityProvider"));
    }
    
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
