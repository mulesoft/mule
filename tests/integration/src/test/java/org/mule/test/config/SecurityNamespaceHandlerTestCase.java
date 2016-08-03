/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.security.MockEncryptionStrategy;
import org.mule.functional.security.TestSingleUserSecurityProvider;
import org.mule.runtime.core.api.EncryptionStrategy;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.security.PasswordBasedEncryptionStrategy;
import org.mule.runtime.core.security.SecretKeyEncryptionStrategy;

import java.util.Iterator;

import org.junit.Test;

public class SecurityNamespaceHandlerTestCase extends AbstractIntegrationTestCase
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
