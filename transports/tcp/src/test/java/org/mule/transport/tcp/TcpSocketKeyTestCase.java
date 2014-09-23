/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;

import org.junit.Test;

public class TcpSocketKeyTestCase 
{

    @Test
    public void testHashAndEquals() throws MuleException
    {
        final TcpConnector tcpConnector = mock(TcpConnector.class);

        final ImmutableEndpoint endpoint1in = createEndpoint(tcpConnector, "localhost", 8080);
        final TcpSocketKey key1in = new TcpSocketKey(endpoint1in);

        final ImmutableEndpoint endpoint1out = createEndpoint(tcpConnector, "localhost", 8080);
        final TcpSocketKey key1out = new TcpSocketKey(endpoint1out);

        final ImmutableEndpoint endpoint2in = createEndpoint(tcpConnector, "localhost", 9080);
        final TcpSocketKey key2in = new TcpSocketKey(endpoint2in);

        assertEquals(key1in, key1in);
        assertEquals(key1in, key1out);
        assertNotSame(key1in, key2in);
        assertEquals(key1in.hashCode(), key1in.hashCode());
        assertEquals(key1in.hashCode(), key1out.hashCode());
        assertFalse(key1in.hashCode() == key2in.hashCode());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnresolvedHost()
    {
        final TcpConnector tcpConnector = mock(TcpConnector.class);
        when(tcpConnector.isFailOnUnresolvedHost()).thenReturn(Boolean.TRUE);
        
        final ImmutableEndpoint endpoint = createEndpoint(tcpConnector, "some.invented.host_abc", 8080);
        @SuppressWarnings("unused")
        final TcpSocketKey key = new TcpSocketKey(endpoint);
    }

    @Test
    public void testResolvedHost()
    {
        final TcpConnector tcpConnector = mock(TcpConnector.class);
        when(tcpConnector.isFailOnUnresolvedHost()).thenReturn(Boolean.TRUE);
        
        final ImmutableEndpoint endpoint = createEndpoint(tcpConnector, "localhost", 8080);
        final TcpSocketKey key = new TcpSocketKey(endpoint);
        assertNotNull(key);
    }

    private ImmutableEndpoint createEndpoint(TcpConnector tcpConnector, String host, int port)
    {
        final EndpointURI endpointURI = mock(EndpointURI.class);
        when(endpointURI.getHost()).thenReturn(host);
        when(endpointURI.getPort()).thenReturn(port);
        final ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class);
        when(endpoint.getConnector()).thenReturn(tcpConnector);
        when(endpoint.getEndpointURI()).thenReturn(endpointURI);
        return endpoint;
    }

}
