/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.config.MuleProperties;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class EjbEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("ejb://localhost:1099");
        url.initialise();
        assertEquals("ejb", url.getScheme());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    public void testQueryParams1() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("ejb://localhost:1099/BeeShirtsejbServer?method=testMethod");
        url.initialise();
        assertEquals("ejb", url.getScheme());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsejbServer", url.getPath());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ejb://localhost:1099/BeeShirtsejbServer?method=testMethod", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
    }

    public void testQueryParams2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI(
            "ejb://localhost:1099/BeeShirtsejbServer?method=testMethod&endpointName=ejbProvider&blankParam=");
        url.initialise();
        assertEquals("ejb", url.getScheme());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsejbServer", url.getPath());
        assertNotNull(url.getEndpointName());
        assertEquals("ejbProvider", url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(
            "ejb://localhost:1099/BeeShirtsejbServer?method=testMethod&endpointName=ejbProvider&blankParam=",
            url.toString());
        assertEquals("method=testMethod&endpointName=ejbProvider&blankParam=", url.getQuery());
        assertEquals(3, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }
}
