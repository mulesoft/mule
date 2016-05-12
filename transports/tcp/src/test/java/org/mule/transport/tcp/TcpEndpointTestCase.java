/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TcpEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostPortUrl() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("tcp://localhost:7856", muleContext);
        url.initialise();
        assertEquals(TcpConnector.TCP, url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    @Test
    public void testQueryParams1() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("tcp://localhost:7856?param=1", muleContext);
        url.initialise();

        assertEquals(TcpConnector.TCP, url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856?param=1", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
    }

    @Test
    public void testQueryParams2() throws Exception
    {
        EndpointURI url = new MuleEndpointURI(
            "tcp://localhost:7856?param=1&endpointName=tcpProvider&blankParam=", muleContext);
        url.initialise();
        
        assertEquals(TcpConnector.TCP, url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("tcpProvider", url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856?param=1&endpointName=tcpProvider&blankParam=", url.toString());
        assertEquals("param=1&endpointName=tcpProvider&blankParam=", url.getQuery());
        assertEquals(3, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }
}
