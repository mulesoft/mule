/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.SecurityManager;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.security.SecretKeyEncryptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.security.MockEncryptionStrategy;
import org.mule.tck.security.TestSingleUserSecurityProvider;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecurityNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
