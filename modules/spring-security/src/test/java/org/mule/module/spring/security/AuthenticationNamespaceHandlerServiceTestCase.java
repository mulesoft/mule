/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;
import org.mule.service.ServiceCompositeMessageSource;

import org.junit.Test;

public class AuthenticationNamespaceHandlerServiceTestCase extends AuthenticationNamespaceHandlerTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "authentication-config-service.xml";
    }

    @Test
    public void testEndpointConfiguration()
    {
        Service service = muleContext.getRegistry().lookupService("echo");
        assertNotNull(service);
        assertEquals(1, ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().size());

        ImmutableEndpoint endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertNotNull(endpoint.getSecurityFilter());
        assertEquals(HttpBasicAuthenticationFilter.class, endpoint.getSecurityFilter().getClass());
    }
}
