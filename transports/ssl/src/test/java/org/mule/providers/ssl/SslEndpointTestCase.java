/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class SslEndpointTestCase extends AbstractMuleTestCase
{

    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("ssl://localhost:7856");
        assertEquals("ssl", url.getScheme());
        assertEquals("ssl://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ssl://localhost:7856", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    public void testQueryParams1() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("ssl://localhost:7856?param=1");
        assertEquals("ssl", url.getScheme());
        assertEquals("ssl://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ssl://localhost:7856?param=1", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
    }

    public void testQueryParams2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI(
            "ssl://localhost:7856?param=1&endpointName=sslProvider&blankParam=");
        assertEquals("ssl", url.getScheme());
        assertEquals("ssl://localhost:7856", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("sslProvider", url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ssl://localhost:7856?param=1&endpointName=sslProvider&blankParam=", url.toString());
        assertEquals("param=1&endpointName=sslProvider&blankParam=", url.getQuery());
        assertEquals(3, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }
}
