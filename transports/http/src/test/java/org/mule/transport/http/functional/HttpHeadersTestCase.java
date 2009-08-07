/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

public class HttpHeadersTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "http-headers-config.xml";
    }

    public void testJettyHeaders() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("clientEndpoint", null, null);
        assertNotNull(result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("application/x-download",
            result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertNotNull(result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_DISPOSITION));
        assertEquals("attachment; filename=foo.zip",
            result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_DISPOSITION));
    }

    public void testClientHeaders() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("clientEndpoint2", null, null); 

        MuleMessage result = client.request("vm://out", 5000);
        assertNotNull(result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("application/xml",
            result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertNotNull(result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_DISPOSITION));
        assertEquals("attachment; filename=foo.zip",
            result.getAdapter().getProperty(HttpConstants.HEADER_CONTENT_DISPOSITION));

        assertNotNull(result.getAdapter().getProperty("X-Test"));
        assertEquals("foo", result.getAdapter().getProperty("X-Test"));
    }
    
}


