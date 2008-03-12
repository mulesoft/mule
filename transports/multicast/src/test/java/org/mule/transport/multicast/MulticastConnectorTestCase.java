/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.multicast;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;

import java.net.DatagramPacket;

public class MulticastConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public Connector createConnector() throws Exception
    {
        MulticastConnector c = new MulticastConnector();
        c.setName("MulticastConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return "multicast://228.3.4.5:60106";
    }

    public Object getValidMessage() throws Exception
    {
        return new DatagramPacket("Hello".getBytes(), 5);
    }

    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        Connector connector = getConnector();

        InboundEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("multicast://228.2.3.4:10100");

        connector.registerListener(service, endpoint2);
        try
        {
            connector.registerListener(service, endpoint2);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testProperties() throws Exception
    {
        MulticastConnector c = new MulticastConnector();
        c.setReceiveBufferSize(1024);
        assertEquals(1024, c.getReceiveBufferSize());
        c.setReceiveBufferSize(0);
        assertEquals(MulticastConnector.DEFAULT_BUFFER_SIZE, c.getReceiveBufferSize());

        c.setReceiveTimeout(-1);
        assertEquals(MulticastConnector.DEFAULT_SOCKET_TIMEOUT, c.getReceiveTimeout());

        c.setLoopback(true);
        assertTrue(c.isLoopback());
    }

}
