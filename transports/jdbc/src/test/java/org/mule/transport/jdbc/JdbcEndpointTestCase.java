/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JdbcEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testWithoutEndpointName() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jdbc:/?sql=SELECT * FROM TABLE", muleContext);
        url.initialise();
        assertEquals("jdbc", url.getScheme());
        assertEquals("", url.getAddress());
        assertNotNull(url.getParams());
        assertEquals("SELECT * FROM TABLE", url.getParams().get("sql"));
        assertEquals("jdbc:/?sql=SELECT%20*%20FROM%20TABLE", url.toString());
    }

    @Test
    public void testWithoutEndpointName2() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jdbc://?sql=SELECT * FROM TABLE", muleContext);
        url.initialise();
        assertEquals("jdbc", url.getScheme());
        assertEquals("jdbc", url.getAddress());
        assertNotNull(url.getParams());
        assertEquals("SELECT * FROM TABLE", url.getParams().get("sql"));
        assertEquals("jdbc://?sql=SELECT%20*%20FROM%20TABLE", url.toString());
    }

    @Test
    public void testWithEndpointName() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jdbc://writeTests?type=2", muleContext);
        url.initialise();
        assertEquals("jdbc", url.getScheme());
        assertEquals("writeTests", url.getAddress());
        assertNotNull(url.getParams());
        assertEquals("2", url.getParams().get("type"));
        assertEquals("jdbc://writeTests?type=2", url.toString());
    }

}
