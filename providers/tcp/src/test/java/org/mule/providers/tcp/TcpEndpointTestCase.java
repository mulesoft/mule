/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TcpEndpointTestCase extends NamedTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("tcp://localhost:7856");
        assertEquals("tcp", url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertEquals(0, url.getParams().size());
    }


    public void testQueryParams1() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("tcp://localhost:7856?param=1");
        assertEquals("tcp", url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856?param=1", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
    }

    public void testQueryParams2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("tcp://localhost:7856?param=1&endpointName=tcpProvider&blankParam=");
        assertEquals("tcp", url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("tcpProvider", url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856?param=1&endpointName=tcpProvider&blankParam=", url.toString());
        assertEquals("param=1&endpointName=tcpProvider&blankParam=", url.getQuery());
        assertEquals(2, url.getParams().size());
        assertEquals("1", url.getParams().getProperty("param"));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }
}
