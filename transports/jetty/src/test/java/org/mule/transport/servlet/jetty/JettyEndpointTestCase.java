/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
