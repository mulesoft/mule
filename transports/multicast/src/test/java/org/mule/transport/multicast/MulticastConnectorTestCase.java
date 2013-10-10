/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.multicast;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;

import java.net.DatagramPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MulticastConnectorTestCase extends AbstractConnectorTestCase
{

    @Override
    public Connector createConnector() throws Exception
    {
        MulticastConnector c = new MulticastConnector(muleContext);
        c.setName("MulticastConnector");
        return c;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "multicast://228.3.4.5:60106";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return new DatagramPacket("Hello".getBytes(), 5);
    }

    @Test
    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        Connector connector = getConnector();

        InboundEndpoint endpoint2 = muleContext.getEndpointFactory()
            .getInboundEndpoint("multicast://228.2.3.4:10100");

        connector.registerListener(endpoint2, getSensingNullMessageProcessor(), service);
        try
        {
            connector.registerListener(endpoint2, getSensingNullMessageProcessor(), service);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testProperties() throws Exception
    {
        MulticastConnector c = new MulticastConnector(muleContext);
        c.setReceiveBufferSize(1024);
        assertEquals(1024, c.getReceiveBufferSize());
        c.setReceiveBufferSize(0);
        assertEquals(MulticastConnector.DEFAULT_BUFFER_SIZE, c.getReceiveBufferSize());

        c.setTimeout(-1);
        assertEquals(MulticastConnector.DEFAULT_SOCKET_TIMEOUT, c.getTimeout());

        c.setLoopback(true);
        assertTrue(c.isLoopback());
    }

}
