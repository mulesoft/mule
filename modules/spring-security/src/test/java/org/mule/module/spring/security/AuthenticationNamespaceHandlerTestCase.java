/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.SecurityProvider;
import org.mule.api.service.Service;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;
import org.mule.security.MuleSecurityManager;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthenticationNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "authentication-config.xml";
    }

    @Test
    public void testSecurityManagerConfigured()
    {
        MuleSecurityManager securityManager = 
            (MuleSecurityManager) muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
        assertNotNull(securityManager);
        
        Collection providers = securityManager.getProviders();
        assertEquals(2, providers.size());

        Iterator providersIterator = providers.iterator();
        SecurityProvider provider = (SecurityProvider) providersIterator.next();
        assertEquals(SpringProviderAdapter.class, provider.getClass());
        assertEquals(UserAndPasswordAuthenticationProvider.class, ((SpringProviderAdapter) provider).getAuthenticationProvider().getClass());

        provider = (SecurityProvider) providersIterator.next();
        assertEquals(SpringProviderAdapter.class, provider.getClass());
        assertEquals(PreAuthenticatedAuthenticationProvider.class, ((SpringProviderAdapter) provider).getAuthenticationProvider().getClass());
    }
    
    @Test
    public void testEndpointConfiguration()
    {
        Service service = muleContext.getRegistry().lookupService("echo");
        assertNotNull(service);
        assertEquals(1, ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().size());

        ImmutableEndpoint endpoint = (ImmutableEndpoint) ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertNotNull(endpoint.getSecurityFilter());
        assertEquals(HttpBasicAuthenticationFilter.class, endpoint.getSecurityFilter().getClass());
    }

}
