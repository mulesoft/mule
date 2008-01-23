/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.udp.UdpConnector;

import java.net.DatagramPacket;

public class UdpConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public Connector createConnector() throws Exception
    {
        UdpConnector c = new UdpConnector();
        c.setName("UdpConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return "udp://localhost:61024";
    }

    public Object getValidMessage() throws Exception
    {
        return new DatagramPacket("Hello".getBytes(), 5);
    }

    public void testValidListener() throws Exception
    {
        Component component = getTestComponent("orange", Orange.class);
        Endpoint endpoint = getTestEndpoint("Test", Endpoint.ENDPOINT_TYPE_RECEIVER);
        Connector connector = getConnector();

        try
        {
            endpoint.setEndpointURI(null);
            endpoint.setConnector(connector);
            connector.registerListener(component, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }

        endpoint = getTestEndpoint("Test", Endpoint.ENDPOINT_TYPE_RECEIVER);
        try
        {
            endpoint.setEndpointURI(null);
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("udp://localhost:3456");

        connector.registerListener(component, endpoint2);
        try
        {
            connector.registerListener(component, endpoint2);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testProperties() throws Exception
    {
        UdpConnector connector = (UdpConnector)this.getConnector();

        connector.setReceiveBufferSize(1024);
        assertEquals(1024, connector.getReceiveBufferSize());
        connector.setReceiveBufferSize(0);
        assertEquals(UdpConnector.DEFAULT_BUFFER_SIZE, connector.getReceiveBufferSize());

        connector.setReceiveTimeout(-1);
        assertEquals(UdpConnector.DEFAULT_SOCKET_TIMEOUT, connector.getReceiveTimeout());
    }

}
