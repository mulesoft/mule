/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;

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

    protected UMOSecurityProvider getProvider(String name)
    {
        UMOSecurityManager securityManager = managementContext.getSecurityManager();
        return securityManager.getProvider(name);
    }

    protected void knownProperties(UMOSecurityProvider provider)
    {
        assertNotNull(provider);
        assertTrue(provider instanceof PGPSecurityProvider);
        PGPSecurityProvider pgpProvider = (PGPSecurityProvider) provider;
        assertNotNull(pgpProvider.getKeyManager());
        assertTrue(pgpProvider.getKeyManager() instanceof PGPKeyRingImpl);
    }
    
    protected UMOEncryptionStrategy getEncryptionStrategy(String name)
    {
        UMOSecurityManager securityManager = managementContext.getSecurityManager();
        return securityManager.getEncryptionStrategy(name);
    }
    
    protected void knownProperties(UMOEncryptionStrategy encryptionStrategy)
    {
        assertNotNull(encryptionStrategy);
        assertTrue(encryptionStrategy instanceof KeyBasedEncryptionStrategy);
        KeyBasedEncryptionStrategy keyBased = (KeyBasedEncryptionStrategy) encryptionStrategy;
        assertNotNull(keyBased.getKeyManager());
        assertTrue(keyBased.getKeyManager() instanceof PGPKeyRingImpl);
    }

}
