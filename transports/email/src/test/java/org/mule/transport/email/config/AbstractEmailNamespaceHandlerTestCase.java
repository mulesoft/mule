/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
