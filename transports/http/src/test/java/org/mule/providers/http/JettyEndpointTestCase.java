/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.jetty.JettyConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JettyEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortOnlyUrl() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("jetty:http://localhost:8080");
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.toString());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    public void testHostPortOnlyUrlAndUserInfo() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("jetty:http://admin:pwd@localhost:8080");
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUsername());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    public void testHostPortAndPathUrl() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("jetty:http://localhost:8080/app/path");
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    public void testHostPortAndPathUrlAndUserInfo() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("jetty:http://admin:pwd@localhost:8080/app/path");
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUsername());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    public void testRestHostPortAndPathUrlAndUserInfo() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("jetty:rest://admin:pwd@localhost:8080/app/path");
        assertEquals("rest", endpointUri.getScheme());
        assertEquals("rest://localhost:8080/app/path", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("rest://localhost:8080/app/path", endpointUri.getAddress());
        assertEquals(endpointUri.getPath(), "/app/path");
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUsername());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("jetty", endpointUri.getSchemeMetaInfo());
    }

    public void testJettyRestEndpointCreation() throws Exception
    {
        MuleEndpoint ep = new MuleEndpoint("jetty:rest://localhost:8080/loanbroker", true);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);

    }

    public void testJettyHttpEndpointCreation() throws Exception
    {
        MuleEndpoint ep = new MuleEndpoint("jetty:http://localhost:8080/loanbroker", true);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }
}
