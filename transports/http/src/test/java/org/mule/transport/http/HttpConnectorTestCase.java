/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpConnectorTestCase extends AbstractConnectorTestCase
{

    @Override
    public Connector createConnector() throws Exception
    {
        HttpConnector c = new HttpConnector(muleContext);
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

    @Test
    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            getTestEndpointURI());

        getConnector().registerListener(endpoint, getSensingNullMessageProcessor(), service);
    }

    @Test
    public void testProperties() throws Exception
    {
        HttpConnector c = (HttpConnector) getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        int maxDispatchers = c.getMaxTotalDispatchers();
        HttpConnectionManagerParams params = c.getClientConnectionManager().getParams();
        assertEquals(maxDispatchers, params.getDefaultMaxConnectionsPerHost());
        assertEquals(maxDispatchers, params.getMaxTotalConnections());

        // all kinds of timeouts are now being tested in TcpConnectorTestCase
    }

}
