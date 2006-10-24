/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class JdbcEndpointTestCase extends AbstractMuleTestCase
{

    public void testWithoutEndpointName() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jdbc:/?sql=SELECT * FROM TABLE");
        assertEquals("jdbc", url.getScheme());
        assertEquals("", url.getAddress());
        assertNull(url.getEndpointName());
        assertNotNull(url.getParams());
        assertEquals("SELECT * FROM TABLE", url.getParams().get("sql"));
        assertEquals("jdbc:/?sql=SELECT%20*%20FROM%20TABLE", url.toString());
    }

    public void testWithoutEndpointName2() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jdbc://?sql=SELECT * FROM TABLE");
        assertEquals("jdbc", url.getScheme());
        assertEquals("jdbc", url.getAddress());
        assertNull(url.getEndpointName());
        assertNotNull(url.getParams());
        assertEquals("SELECT * FROM TABLE", url.getParams().get("sql"));
        assertEquals("jdbc://?sql=SELECT%20*%20FROM%20TABLE", url.toString());
    }

    public void testWithEndpointName() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jdbc://history/writeTests?type=2");
        assertEquals("jdbc", url.getScheme());
        assertEquals("writeTests", url.getAddress());
        assertEquals("history", url.getEndpointName());
        assertNotNull(url.getParams());
        assertEquals("2", url.getParams().get("type"));
        assertEquals("jdbc://history/writeTests?type=2", url.toString());
    }

}
