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
package org.mule.providers.rmi;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */

public class RmiEndpointTestCase extends NamedTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("rmi://localhost:1099");
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    public void testQueryParams1() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod");
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsRmiServer", url.getPath());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty("method"));
    }

    public void testQueryParams2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod&endpointName=rmiProvider&blankParam=");
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsRmiServer", url.getPath());
        assertNotNull(url.getEndpointName());
        assertEquals("rmiProvider", url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod&endpointName=rmiProvider&blankParam=",
                     url.toString());
        assertEquals("method=testMethod&endpointName=rmiProvider&blankParam=", url.getQuery());
        assertEquals(2, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty("method"));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }
}
