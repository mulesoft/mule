/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.UMOException;

public class TcpSocketKeyTestCase extends FunctionalTestCase
{

    public void testHashAndEquals() throws UMOException
    {
        UMOImmutableEndpoint endpoint1in =
                managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("globalEndpoint1");
        TcpSocketKey key1in = new TcpSocketKey(endpoint1in);
        UMOImmutableEndpoint endpoint1out =
                managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint("globalEndpoint1");
        TcpSocketKey key1out = new TcpSocketKey(endpoint1out);
        UMOImmutableEndpoint endpoint2in =
                managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("globalEndpoint2");
        TcpSocketKey key2in = new TcpSocketKey(endpoint2in);

        assertEquals(key1in, key1in);
        assertEquals(key1in, key1out);
        assertNotSame(key1in, key2in);
        assertEquals(key1in.hashCode(), key1in.hashCode());
        assertEquals(key1in.hashCode(), key1out.hashCode());
        assertFalse(key1in.hashCode() == key2in.hashCode());
    }

    protected String getConfigResources()
    {
        return "tcp-socket-key-test.xml";
    }

}
