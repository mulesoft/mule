/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ejb;

import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EjbEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostPortUrl() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("ejb://localhost:1099", muleContext);
        url.initialise();
        assertEquals("ejb", url.getScheme());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("ejb://localhost:1099", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    @Test
    public void testQueryParams1() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("ejb://localhost:1099/BeeShirtsejbServer?method=testMethod", muleContext);
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

    @Test
    public void testQueryParams2() throws Exception
    {
        EndpointURI url = new MuleEndpointURI(
            "ejb://localhost:1099/BeeShirtsejbServer?method=testMethod&endpointName=ejbProvider&blankParam=", muleContext);
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
