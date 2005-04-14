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
package org.mule.providers.jdbc;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcEndpointTestCase extends NamedTestCase {

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
        assertEquals(null, url.getAddress());
        assertNull(url.getEndpointName());
        assertNotNull(url.getParams());
        assertEquals("SELECT * FROM TABLE", url.getParams().get("sql"));
        assertEquals("jdbc://?sql=SELECT%20*%20FROM%20TABLE", url.toString());
    }
    
    public void testWithEndpointName() throws Exception {
        UMOEndpointURI url = new MuleEndpointURI("jdbc://history/writeTests?type=2");
        assertEquals("jdbc", url.getScheme());
        assertEquals("writeTests", url.getAddress());
        assertEquals("history", url.getEndpointName());
        assertNotNull(url.getParams());
        assertEquals("2", url.getParams().get("type"));
        assertEquals("jdbc://history/writeTests?type=2", url.toString());
    }
    
}
