/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.net.DatagramPacket;

public class UdpConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
    {
        UdpConnector c = new UdpConnector();
        c.setName("UdpConnector");
        c.initialise();
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
        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        endpoint.setEndpointURI(null);
        endpoint.setConnector(connector);

        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }
        endpoint.setEndpointURI(null);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }

        endpoint.setEndpointURI(new MuleEndpointURI("udp://localhost:3456"));
        connector.registerListener(component, endpoint);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }
        connector.dispose();
    }

    public void testProperties() throws Exception
    {
        UdpConnector connector = (UdpConnector)this.connector;

        connector.setBufferSize(1024);
        assertEquals(1024, connector.getBufferSize());
        connector.setBufferSize(0);
        assertEquals(UdpConnector.DEFAULT_BUFFER_SIZE, connector.getBufferSize());

        connector.setTimeout(-1);
        assertEquals(UdpConnector.DEFAULT_SOCKET_TIMEOUT, connector.getTimeout());
    }

}
