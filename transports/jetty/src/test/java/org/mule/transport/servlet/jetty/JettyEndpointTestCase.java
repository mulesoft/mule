/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JettyEndpointTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testHostPortOnlyUrl() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("jetty:http://localhost:8080", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    @Test
    public void testHostPortOnlyUrlAndUserInfo() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("jetty:http://admin:pwd@localhost:8080", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    @Test
    public void testHostPortAndPathUrl() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("jetty:http://localhost:8080/app/path", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    @Test
    public void testHostPortAndPathUrlAndUserInfo() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("jetty:http://admin:pwd@localhost:8080/app/path", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

}
