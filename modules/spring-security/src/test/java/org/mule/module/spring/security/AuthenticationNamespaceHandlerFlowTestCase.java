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
import org.mule.construct.Flow;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class AuthenticationNamespaceHandlerFlowTestCase extends AuthenticationNamespaceHandlerServiceTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "authentication-config-flow.xml";
    }
 
    @Override
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
