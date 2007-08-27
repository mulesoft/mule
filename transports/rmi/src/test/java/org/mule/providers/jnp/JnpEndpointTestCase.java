/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jnp;

import org.mule.config.MuleProperties;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class JnpEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jnp://localhost:1099");
        url.initialise();
        assertEquals("jnp", url.getScheme());
        assertEquals("jnp://localhost:1099", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("jnp://localhost:1099", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    public void testQueryParams1() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jnp://localhost:1099/BeeShirtsjnpServer?method=testMethod");
        url.initialise();
        assertEquals("jnp", url.getScheme());
        assertEquals("jnp://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsjnpServer", url.getPath());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("jnp://localhost:1099/BeeShirtsjnpServer?method=testMethod", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
    }

    public void testQueryParams2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI(
            "jnp://localhost:1099/BeeShirtsjnpServer?method=testMethod&endpointName=jnpProvider&blankParam=");
        url.initialise();
        assertEquals("jnp", url.getScheme());
        assertEquals("jnp://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsjnpServer", url.getPath());
        assertNotNull(url.getEndpointName());
        assertEquals("jnpProvider", url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(
            "jnp://localhost:1099/BeeShirtsjnpServer?method=testMethod&endpointName=jnpProvider&blankParam=",
            url.toString());
        assertEquals("method=testMethod&endpointName=jnpProvider&blankParam=", url.getQuery());
        assertEquals(3, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }

}
