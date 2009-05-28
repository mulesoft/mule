/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.tcp.TcpConnector;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

public class HttpConnectorTestCase extends AbstractConnectorTestCase
{

    @Override
    public Connector createConnector() throws Exception
    {
        HttpConnector c = new HttpConnector();
        c.setName("HttpConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return "http://localhost:60127";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        InboundEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            getTestEndpointURI());

        getConnector().registerListener(service, endpoint);
    }

    public void testProperties() throws Exception
    {
        HttpConnector c = (HttpConnector) getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        int maxThreadsActive = c.getDispatcherThreadingProfile().getMaxThreadsActive();
        HttpConnectionManagerParams params = c.getClientConnectionManager().getParams();
        assertEquals(maxThreadsActive, params.getDefaultMaxConnectionsPerHost());
        assertEquals(maxThreadsActive, params.getMaxTotalConnections());

        // all kinds of timeouts are now being tested in TcpConnectorTestCase
    }

}
