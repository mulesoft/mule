/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleSession;
import org.mule.NullSessionHandler;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.tcp.TcpConnector;

import java.util.Arrays;

public class TcpSyncTestCase extends FunctionalTestCase
{

    private static final String endpointUri = "tcp://localhost:45441";

    protected String getConfigResources()
    {
        return "tcp-sync.xml";
    }

    protected MuleMessage send(Object payload) throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(payload);
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointUri);
        DefaultMuleSession session = new DefaultMuleSession(message, new NullSessionHandler());
        DefaultMuleEvent event = new DefaultMuleEvent(message, endpoint, session, true);
        event.setTimeout(60000);
        return event.getSession().sendEvent(event);
    }

    public void testSendString() throws Exception
    {
        MuleMessage message = send("data");
        assertNotNull(message);
        String response = message.getPayloadAsString();
        assertEquals("data", response);
    }

    public void testSyncResponseOfBufferSize() throws Exception
    {
        TcpConnector tcp = (TcpConnector)muleContext.getRegistry().lookupConnector("tcpConnector");
        tcp.setBufferSize(1024 * 16);
        byte[] data = fillBuffer(new byte[tcp.getBufferSize()]);
        MuleMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

    public void testManySyncResponseOfBufferSize() throws Exception
    {
        TcpConnector tcp = (TcpConnector)muleContext.getRegistry().lookupConnector("tcpConnector");
        tcp.setBufferSize(1024 * 16);
        byte[] data = fillBuffer(new byte[tcp.getBufferSize()]);
        for (int i = 0; i < 20; ++i)
        {
            MuleMessage message = send(data);
            assertNotNull(message);
            byte[] response = message.getPayloadAsBytes();
            assertEquals(data.length, response.length);
            assertTrue(Arrays.equals(data, response));
        }
    }

    public void testSyncResponseVeryBig() throws Exception
    {
        byte[] data = fillBuffer(new byte[1024 * 1024]);
        MuleMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

    protected byte[] fillBuffer(byte[] buffer)
    {
        for (int i = 0; i < buffer.length; ++i)
        {
            buffer[i] = (byte) (i % 255);
        }
        return buffer;
    }

}
