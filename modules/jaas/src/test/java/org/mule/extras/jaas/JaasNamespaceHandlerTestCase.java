/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;

public class JaasNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jaas-namespace-config.xml";
    }
    
    public void testJaasProvider()
    {
        knownProperties(getProvider("jaasSecurityProvider"));
    }
    
    public void testPasswordBasedEncryption()
    {
        knownProperties(getEncryptionStrategy("PBE"));
    }
    
    protected UMOSecurityProvider getProvider(String name)
    {
        UMOSecurityManager securityManager = managementContext.getSecurityManager();
        return securityManager.getProvider(name);
    }
    
    protected void knownProperties(UMOSecurityProvider provider)
    {
        assertNotNull(provider);
        assertTrue(provider instanceof JaasSimpleAuthenticationProvider);
        JaasSimpleAuthenticationProvider jaasProvider = (JaasSimpleAuthenticationProvider) provider;
        assertNotNull(jaasProvider.getLoginContextName());
        assertEquals("jaasTest", jaasProvider.getLoginContextName());
        assertNotNull(jaasProvider.getLoginConfig());
        assertEquals("jaas.conf", jaasProvider.getLoginConfig());
    }
    
    protected UMOEncryptionStrategy getEncryptionStrategy(String name)
    {
        UMOSecurityManager securityManager = managementContext.getSecurityManager();
        return securityManager.getEncryptionStrategy(name);
    }
    
    protected void knownProperties(UMOEncryptionStrategy encryptionStrategy)
    {
        assertNotNull(encryptionStrategy);
        assertTrue(encryptionStrategy instanceof PasswordBasedEncryptionStrategy);
        PasswordBasedEncryptionStrategy pbe = (PasswordBasedEncryptionStrategy) encryptionStrategy;
        assertNotNull(pbe.getName());
        assertEquals("PBE", pbe.getName());
    }

}


