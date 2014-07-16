/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.security.SecretKeyEncryptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.security.MockEncryptionStrategy;
import org.mule.tck.security.TestSingleUserSecurityProvider;

import java.util.Iterator;

import org.junit.Test;

public class SecurityNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "security-namespace-config.xml";
    }

    @Test
    public void testSecurity()
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        assertNotNull(securityManager.getProvider("dummySecurityProvider"));
        assertTrue(securityManager.getProvider("dummySecurityProvider") instanceof TestSingleUserSecurityProvider);
        verifyEncryptionStrategy(securityManager, "dummyEncryptionStrategy", MockEncryptionStrategy.class);
        verifyEncryptionStrategy(securityManager, "passwordEncryptionStrategy", PasswordBasedEncryptionStrategy.class);
        verifyEncryptionStrategy(securityManager, "secretKeyEncryptionStrategy", SecretKeyEncryptionStrategy.class);
    }
    
    private void verifyEncryptionStrategy(SecurityManager securityManager, String name, Class clazz)
    {
        Iterator strategies = securityManager.getEncryptionStrategies().iterator();
        logger.debug("Listing strategies");
        while (strategies.hasNext())
        {
            EncryptionStrategy strategy = (EncryptionStrategy) strategies.next();
            logger.debug(strategy.getName() + " / " + strategy.toString() + " / " + strategy.getClass());
        }
        assertNotNull(name, securityManager.getEncryptionStrategy(name));
        assertTrue(securityManager.getEncryptionStrategy(name).getClass().equals(clazz));
    }
}
