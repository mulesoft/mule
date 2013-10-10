/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TcpSyncTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-sync-service.xml"},
            {ConfigVariant.FLOW, "tcp-sync-flow.xml"}
        });
    }

    public TcpSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected MuleMessage send(Object payload) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        return client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inService")).getAddress(), payload, null);
    }

    @Test
    public void testSendString() throws Exception
    {
        MuleMessage message = send("data");
        assertNotNull(message);
        String response = message.getPayloadAsString();
        assertEquals("data", response);
    }

    @Test
    public void testSyncResponseOfBufferSize() throws Exception
    {
        int size = 1024 * 16;
        TcpConnector tcp = (TcpConnector)muleContext.getRegistry().lookupConnector("tcpConnector");
        tcp.setSendBufferSize(size);
        tcp.setReceiveBufferSize(size);
        byte[] data = fillBuffer(new byte[size]);
        MuleMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

    @Test
    public void testManySyncResponseOfBufferSize() throws Exception
    {
        int size = 1024 * 16;
        TcpConnector tcp = (TcpConnector)muleContext.getRegistry().lookupConnector("tcpConnector");
        tcp.setSendBufferSize(size);
        tcp.setReceiveBufferSize(size);
        byte[] data = fillBuffer(new byte[size]);
        for (int i = 0; i < 20; ++i)
        {
            MuleMessage message = send(data);
            assertNotNull(message);
            byte[] response = message.getPayloadAsBytes();
            assertEquals(data.length, response.length);
            assertTrue(Arrays.equals(data, response));
        }
    }

    @Test
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
