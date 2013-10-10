/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;

import java.net.DatagramPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UdpConnectorTestCase extends AbstractConnectorTestCase
{

    @Override
    public Connector createConnector() throws Exception
    {
        UdpConnector c = new UdpConnector(muleContext);
        c.setName("UdpConnector");
        return c;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "udp://localhost:61024";
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
            .getInboundEndpoint("udp://localhost:3456");

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
        UdpConnector connector = (UdpConnector)this.getConnector();

        connector.setReceiveBufferSize(1024);
        assertEquals(1024, connector.getReceiveBufferSize());
        connector.setReceiveBufferSize(0);
        assertEquals(UdpConnector.DEFAULT_BUFFER_SIZE, connector.getReceiveBufferSize());

        connector.setTimeout(-1);
        assertEquals(UdpConnector.DEFAULT_SOCKET_TIMEOUT, connector.getTimeout());
    }

}
