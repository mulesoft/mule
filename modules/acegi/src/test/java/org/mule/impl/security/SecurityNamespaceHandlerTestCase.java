/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.UMOEncryptionStrategy;

import java.util.Iterator;

/**
 * TODO - move to functional tests packge one Ross has finished working there
 */
public class SecurityNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "security-namespace-config.xml";
    }

    public void testSecurity()
    {
        UMOSecurityManager securityManager = managementContext.getSecurityManager();
        assertNotNull(securityManager.getProvider("dummySecurityProvider"));
        assertTrue(securityManager.getProvider("dummySecurityProvider") instanceof MockSecurityProvider);
        verifyEncryptionStrategy(securityManager, "dummyEncryptionStrategy", MockEncryptionStrategy.class);
        verifyEncryptionStrategy(securityManager, "passwordEncryptionStrategy", PasswordBasedEncryptionStrategy.class);
        verifyEncryptionStrategy(securityManager, "secretKeyEncryptionStrategy", SecretKeyEncryptionStrategy.class);
    }
    
    private void verifyEncryptionStrategy(UMOSecurityManager securityManager, String name, Class clazz)
    {
        Iterator strategies = securityManager.getEncryptionStrategies().iterator();
        logger.debug("Listing strategies");
        while (strategies.hasNext())
        {
            UMOEncryptionStrategy strategy = (UMOEncryptionStrategy) strategies.next();
            logger.debug(strategy.getName() + " / " + strategy.toString() + " / " + strategy.getClass());
        }
        assertNotNull(securityManager.getEncryptionStrategy(name));
        assertTrue(securityManager.getEncryptionStrategy(name).getClass().equals(clazz));
    }

}
