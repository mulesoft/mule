/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

public class TcpConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
    {
        TcpConnector c = new TcpConnector();
        c.setName("TcpConnector");
        c.initialise();
        return c;
    }

    public String getTestEndpointURI()
    {
        return "tcp://localhost:56801";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
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

        endpoint.setEndpointURI(new MuleEndpointURI("tcp://localhost:30303"));
        connector.registerListener(component, endpoint);
        try
        {
            // connector.registerListener(component, endpoint);
            // fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }
    }

    public void testProperties() throws Exception
    {
        TcpConnector c = (TcpConnector)connector;

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        // timeouts
        c.setReceiveTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getReceiveTimeout());
        c.setSendTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getSendTimeout());
        c.setSendTimeout(1000);
        c.setReceiveTimeout(1000);
        assertEquals(1000, c.getReceiveTimeout());
        assertEquals(1000, c.getSendTimeout());

        c.dispose();
    }
}
