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
package org.mule.providers.http;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpEndpointTestCase extends NamedTestCase
{
    public void testHostPortOnlyUrl() throws Exception
    {
        UMOEndpointURI endpointUri = new MuleEndpointURI("http://localhost:8080");
        assertEquals("http", endpointUri.getScheme());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(8080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("http://localhost:8080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
    }

    public void testHostPortAndPathUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("http://localhost:8080/app/path");
        assertEquals("http", url.getScheme());
        assertEquals("http://localhost:8080/app/path", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(8080, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("http://localhost:8080/app/path", url.getAddress());
        assertEquals(url.getPath(), "/app/path");
        assertEquals(0, url.getParams().size());
    }
}
