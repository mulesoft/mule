/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.module.ibeans.annotations.AbstractIBeansTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IBeansEndpointTestCase extends AbstractIBeansTestCase
{
    @Test
    public void testValidEndpointURI() throws Exception
    {
        EndpointURI uri = new MuleEndpointURI("ibean://hostip.getHostInfo", muleContext);
        uri.initialise();
        assertEquals("ibean", uri.getScheme());
        assertEquals("hostip.getHostInfo", uri.getAddress());
        assertEquals(0, uri.getParams().size());
        assertEquals("ibean://hostip.getHostInfo", uri.toString());
    }

    @Test
    public void testValidEndpointURIWithParams() throws Exception
    {
        EndpointURI uri = new MuleEndpointURI("ibean://hostip.getHostInfo?param1=value1", muleContext);
        uri.initialise();
        assertEquals("ibean", uri.getScheme());
        assertEquals("hostip.getHostInfo", uri.getAddress());
        assertEquals(1, uri.getParams().size());
        assertEquals("value1", uri.getParams().get("param1"));
        assertEquals("ibean://hostip.getHostInfo?param1=value1", uri.toString());

    }

    @Test
    public void testMissingIBeanEndpointURI() throws Exception
    {
        try
        {
            EndpointURI uri = new MuleEndpointURI("ibean://foo.getBar", muleContext);
            uri.initialise();
            fail("IBean foo is not on the classpath");
        }
        catch (InitialisationException e)
        {
            //Expected
            assertTrue(e.getCause() instanceof MalformedEndpointException);
        }
    }

    @Test
    public void testBanIBeanMethodEndpointURI() throws Exception
    {
        try
        {
            EndpointURI uri = new MuleEndpointURI("ibean://hostip.getBar", muleContext);
            uri.initialise();
            fail("IBean hostip does not have a method called getBar");
        }
        catch (InitialisationException e)
        {
            //Expected
            assertTrue(e.getCause() instanceof MalformedEndpointException);
        }
    }
}
