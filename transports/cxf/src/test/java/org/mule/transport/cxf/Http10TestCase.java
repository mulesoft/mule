/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

public class Http10TestCase extends FunctionalTestCase
{
    public void testHttp10TransformerNotOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        assertFalse("chunked".equals(result.getProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }

    public void testHttp10TransformerOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound2", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        assertFalse("chunked".equals(result.getProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }

    protected String getConfigResources()
    {
        return "http-10-conf.xml";
    }
}