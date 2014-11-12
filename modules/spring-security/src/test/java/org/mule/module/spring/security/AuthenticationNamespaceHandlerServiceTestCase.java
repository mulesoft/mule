/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class AuthenticationNamespaceHandlerServiceTestCase extends AuthenticationNamespaceHandlerTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
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
