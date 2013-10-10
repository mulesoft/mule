/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpringSecurityNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "spring-security-namespace-config.xml";
    }

    @Test
    public void testProvider()
    {
        knownProperties(getProvider("memory-dao"));
    }

    protected SecurityProvider getProvider(String providerName)
    {
        SecurityManager securityManager = muleContext.getSecurityManager();
        return securityManager.getProvider(providerName);
    }

    @Test
    public void testCustom()
    {
        Iterator<SecurityProvider> providers = muleContext.getSecurityManager().getProviders().iterator();
        while (providers.hasNext())
        {
            SecurityProvider provider = providers.next();
            logger.debug(provider);
            logger.debug(provider.getName());
        }
        knownProperties(getProvider("customProvider"));
        knownProperties(getProvider("willOverwriteName"));
    }

    protected void knownProperties(SecurityProvider provider)
    {
        assertNotNull(provider);
        assertTrue(provider instanceof SpringProviderAdapter);
        SpringProviderAdapter adapter = (SpringProviderAdapter) provider;
        assertNotNull(adapter.getDelegate());
    }

}
