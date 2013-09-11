/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.config;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractEmailNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected void testInboundEndpoint(String name, String protocolName) throws MuleException
    {
        ImmutableEndpoint endpoint =
            muleContext.getEndpointFactory().getInboundEndpoint(name);
        testEndpoint(endpoint, protocolName);
    }

    protected void testOutboundEndpoint(String name, String protocolName) throws MuleException
    {
        ImmutableEndpoint endpoint =
            muleContext.getEndpointFactory().getOutboundEndpoint(name);
        testEndpoint(endpoint, protocolName);
    }

    private void testEndpoint(ImmutableEndpoint endpoint, String protocolName)
    {
        assertNotNull(endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull(address);
        assertEquals("bob@localhost:123", address);
        String password = endpoint.getEndpointURI().getPassword();
        assertNotNull(password);
        assertEquals("secret", password);
        String protocol = endpoint.getProtocol();
        assertNotNull(protocol);
        assertEquals(protocolName, protocol);
    }
}
