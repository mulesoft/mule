/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.echo;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;

/**
 * Tests the echo example using CXF.
 */
public class CxfEchoTestCase extends AbstractEchoTestCase
{
    public void testGetEcho() throws Exception
    {
        // CXF has built in support for understanding GET requests. They are of the form:
        // http://host/service/OPERATION/PARAM_NAME/PARAM_VALUE
        
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:65082/services/EchoUMO/echo/text/hello", "", props);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLAssert.assertXMLEqual(expectedGetResponse, result.getPayloadAsString());
    }
    
    public void testPostEcho() throws Exception
    {
        // This test doesn't apply to CXF, so we're making it empty.
    }
    
    protected String getConfigResources()
    {
        return "echo-cxf-config.xml";
    }

    protected String getExpectedGetResponseResource()
    {
        return "echo-cxf-response.xml";
    }

    protected String getExpectedPostResponseResource()
    {
        return "echo-cxf-response.xml";
    }

    protected String getProtocol()
    {
        return "cxf";
    }

}
