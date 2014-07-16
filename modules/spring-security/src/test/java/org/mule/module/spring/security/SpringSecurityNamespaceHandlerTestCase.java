/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Iterator;

import org.junit.Test;

public class SpringSecurityNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
