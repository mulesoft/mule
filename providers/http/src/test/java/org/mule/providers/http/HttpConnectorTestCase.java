/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.http;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.tcp.TcpConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class HttpConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        HttpConnector c = new HttpConnector();
        c.setName("HttpConnector");
        c.initialise();
        return c;
    }

    public String getTestEndpointURI()
    {
        return "http://localhost:60127";
    }

    public Object getValidMessage() throws Exception
    {
        String msg = HttpConstants.HTTP11 + " 200 OK" + HttpConstants.HEADER_CONTENT_SEPARATOR + "Hello";
        return new ByteArrayInputStream(msg.getBytes());
    }

    public void testValidListener() throws Exception
    {
        HttpConnector connector = (HttpConnector)getConnector();

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

        endpoint.setEndpointURI(new MuleEndpointURI("http://localhost:0"));
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
        HttpConnector c = (HttpConnector)getConnector();

        c.setBufferSize(1024);
        assertEquals(1024, c.getBufferSize());
        c.setBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getBufferSize());

        c.setTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getTimeout());
        c.dispose();
    }
}
