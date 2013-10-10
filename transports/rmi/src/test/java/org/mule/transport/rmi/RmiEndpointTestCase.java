/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RmiEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostPortUrl() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("rmi://localhost:1099", muleContext);
        url.initialise();
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals(0, url.getParams().size());
    }

    @Test
    public void testQueryParams1() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod", muleContext);
        url.initialise();
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsRmiServer", url.getPath());
        assertNull(url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod", url.toString());
        assertEquals(1, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
    }

    @Test
    public void testQueryParams2() throws Exception
    {
        EndpointURI url = new MuleEndpointURI(
            "rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod&endpointName=rmiProvider&blankParam=", muleContext);
        url.initialise();
        assertEquals("rmi", url.getScheme());
        assertEquals("rmi://localhost:1099", url.getAddress());
        assertEquals("/BeeShirtsRmiServer", url.getPath());
        assertNotNull(url.getEndpointName());
        assertEquals("rmiProvider", url.getEndpointName());
        assertEquals(1099, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(
            "rmi://localhost:1099/BeeShirtsRmiServer?method=testMethod&endpointName=rmiProvider&blankParam=",
            url.toString());
        assertEquals("method=testMethod&endpointName=rmiProvider&blankParam=", url.getQuery());
        assertEquals(3, url.getParams().size());
        assertEquals("testMethod", url.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals("", url.getParams().getProperty("blankParam"));
    }

}
