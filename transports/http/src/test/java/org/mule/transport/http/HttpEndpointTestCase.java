/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostPortOnlyUrl() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("http://localhost:8080", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
    }

    @Test
    public void testHostPortOnlyUrlAndUserInfo() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("http://admin:pwd@localhost:8080", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("http://admin:****@localhost:8080", endpointUri.toString());
    }

    @Test
    public void testHostPortAndPathUrl() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("http://localhost:8080/app/path", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
    }

    @Test
    public void testHostPortAndPathUrlAndUserInfo() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("http://admin:pwd@localhost:8080/app/path", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("http://admin:****@localhost:8080/app/path", endpointUri.toString());

    }

    @Test
    public void testHostPortAndPathUrlUserInfoAndQuery() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("http://admin:pwd@localhost:8080/app/path?${foo}", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path?$[foo]", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(endpointUri.getQuery(), "$[foo]");
        assertEquals(1, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("http://admin:****@localhost:8080/app/path?$[foo]", endpointUri.toString());

    }
}
