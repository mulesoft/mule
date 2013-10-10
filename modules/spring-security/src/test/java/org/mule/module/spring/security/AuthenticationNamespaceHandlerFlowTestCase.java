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
import org.mule.construct.Flow;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;

import org.junit.Test;

public class AuthenticationNamespaceHandlerFlowTestCase extends AuthenticationNamespaceHandlerServiceTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "authentication-config-flow.xml";
    }
 
    @Test
    public void testEndpointConfiguration()
    {
        Flow flow = muleContext.getRegistry().lookupObject("echo");
        assertNotNull(flow);       

        ImmutableEndpoint endpoint = (ImmutableEndpoint) flow.getMessageSource();
        assertNotNull(endpoint.getSecurityFilter());
        assertEquals(HttpBasicAuthenticationFilter.class, endpoint.getSecurityFilter().getClass());
    }
}
