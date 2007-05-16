/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.net.DatagramPacket;

public class MulticastConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
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
        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);


        try
        {
            endpoint.setEndpointURI(null);
            endpoint.setConnector(connector);
            connector.registerListener(component, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }

        try
        {
            endpoint.setEndpointURI(null);
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }

        endpoint = new MuleEndpoint();
        endpoint.setEndpointURI(new MuleEndpointURI("multicast://228.2.3.4:10100"));
        connector.registerListener(component, endpoint);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }
        connector.dispose();
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
