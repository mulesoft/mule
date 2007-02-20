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
        c.initialise(managementContext);
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
        MulticastConnector connector = new MulticastConnector();
        connector.initialise(managementContext);
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
        { /* expected */
        }
        endpoint.setEndpointURI(null);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }

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
        c.initialise(managementContext);
        c.setBufferSize(1024);
        assertEquals(1024, c.getBufferSize());
        c.setBufferSize(0);
        assertEquals(MulticastConnector.DEFAULT_BUFFER_SIZE, c.getBufferSize());

        c.setTimeout(-1);
        assertEquals(MulticastConnector.DEFAULT_SOCKET_TIMEOUT, c.getTimeout());

        c.setLoopback(true);
        assertTrue(c.isLoopback());
    }

}
