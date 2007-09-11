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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.NullSessionHandler;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.Arrays;

public class TcpSyncTestCase extends FunctionalTestCase
{

    private static final String endpointUri = "tcp://localhost:45441";

    protected String getConfigResources()
    {
        return "tcp-sync.xml";
    }

    protected UMOMessage send(Object payload) throws Exception
    {
        UMOMessage message = new MuleMessage(payload);
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupOutboundEndpoint(endpointUri, managementContext);
        MuleSession session = new MuleSession(message, new NullSessionHandler());
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        event.setTimeout(60000);
        return event.getSession().sendEvent(event);
    }

    public void testSendString() throws Exception
    {
        UMOMessage message = send("data");
        assertNotNull(message);
        String response = message.getPayloadAsString();
        assertEquals("data", response);
    }

    public void testSyncResponseOfBufferSize() throws Exception
    {
        TcpConnector tcp = (TcpConnector)managementContext.getRegistry().lookupConnector("tcpConnector");
        tcp.setBufferSize(1024 * 16);
        byte[] data = new byte[tcp.getBufferSize()];
        UMOMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

    public void testSyncResponseVeryBig() throws Exception
    {
        byte[] data = new byte[1024 * 1024];
        UMOMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

}
