/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.api.endpoint.Endpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.tcp.TcpConnector;

public class TcpConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public Connector createConnector() throws Exception
    {
        TcpConnector c = new TcpConnector();
        c.setName("TcpConnector");
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
        Service service = getTestService("orange", Orange.class);
        
        Endpoint endpoint = (Endpoint) muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(getTestEndpointURI());

        Connector connector = getConnector();

        try
        {
            endpoint.setEndpointURI(null);
            fail("endpointUri cannot be null");
        }
        catch (Exception e)
        {
            // expected
        }

        endpoint.setEndpointURI(new MuleEndpointURI(getTestEndpointURI()));
        connector.registerListener(service, endpoint);
    }

    public void testProperties() throws Exception
    {
        TcpConnector c = (TcpConnector)getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        // timeouts
        c.setServerSoTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getServerSoTimeout());
        c.setClientSoTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getClientSoTimeout());
        c.setClientSoTimeout(1000);
        c.setServerSoTimeout(1000);
        assertEquals(1000, c.getServerSoTimeout());
        assertEquals(1000, c.getClientSoTimeout());
    }
}
