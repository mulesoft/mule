/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
