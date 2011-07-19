/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

public class TcpSocketKeyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "tcp-socket-key-test.xml";
    }

    @Test
    public void testHashAndEquals() throws MuleException
    {
        ImmutableEndpoint endpoint1in =
                muleContext.getEndpointFactory().getInboundEndpoint("globalEndpoint1");
        TcpSocketKey key1in = new TcpSocketKey(endpoint1in);
        ImmutableEndpoint endpoint1out =
                muleContext.getEndpointFactory().getOutboundEndpoint("globalEndpoint1");
        TcpSocketKey key1out = new TcpSocketKey(endpoint1out);
        ImmutableEndpoint endpoint2in =
                muleContext.getEndpointFactory().getInboundEndpoint("globalEndpoint2");
        TcpSocketKey key2in = new TcpSocketKey(endpoint2in);

        assertEquals(key1in, key1in);
        assertEquals(key1in, key1out);
        assertNotSame(key1in, key2in);
        assertEquals(key1in.hashCode(), key1in.hashCode());
        assertEquals(key1in.hashCode(), key1out.hashCode());
        assertFalse(key1in.hashCode() == key2in.hashCode());
    }

}
