/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class HttpEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortOnlyUrl() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("http://localhost:8080");
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testHostPortOnlyUrlAndUserInfo() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("http://admin:pwd@localhost:8080");
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
    }

    public void testHostPortAndPathUrl() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("http://localhost:8080/app/path");
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

    public void testHostPortAndPathUrlAndUserInfo() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("http://admin:pwd@localhost:8080/app/path");
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
    }
}
