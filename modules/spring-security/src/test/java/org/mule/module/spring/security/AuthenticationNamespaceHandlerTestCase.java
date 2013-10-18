/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.config.MuleProperties;
import org.mule.api.security.SecurityProvider;
import org.mule.security.MuleSecurityManager;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

public abstract class AuthenticationNamespaceHandlerTestCase extends FunctionalTestCase
{    
    @Test
    public void testSecurityManagerConfigured()
    {
        MuleSecurityManager securityManager =
            (MuleSecurityManager) muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
        assertNotNull(securityManager);

        Collection<SecurityProvider> providers = securityManager.getProviders();
        assertEquals(2, providers.size());

        Iterator<SecurityProvider> providersIterator = providers.iterator();
        SecurityProvider provider = providersIterator.next();
        assertEquals(SpringProviderAdapter.class, provider.getClass());
        assertEquals(UserAndPasswordAuthenticationProvider.class, ((SpringProviderAdapter) provider).getAuthenticationProvider().getClass());

        provider = providersIterator.next();
        assertEquals(SpringProviderAdapter.class, provider.getClass());
        assertEquals(PreAuthenticatedAuthenticationProvider.class, ((SpringProviderAdapter) provider).getAuthenticationProvider().getClass());
    }
}
