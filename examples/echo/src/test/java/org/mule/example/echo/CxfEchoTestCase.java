/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.echo;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests the echo example using CXF.
 */
public class CxfEchoTestCase extends FunctionalTestCase
{
    private String expectedGetResponse;

    @Override
    protected String getConfigResources()
    {
        return "echo-cxf-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        try
        {
            expectedGetResponse = IOUtils.getResourceAsString("echo-cxf-response.xml", getClass());
        }
        catch (IOException ioex)
        {
            fail(ioex.getMessage());
        }
    }

    @Test
    public void testGetEcho() throws Exception
    {
        // CXF has built in support for understanding GET requests. They are of the form:
        // http://host/service/OPERATION/PARAM_NAME/PARAM_VALUE
        
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:65082/services/EchoUMO/echo/text/hello", "", props);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLAssert.assertXMLEqual(expectedGetResponse, result.getPayloadAsString());
    }

    @Test
    public void testSoapPostEcho() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("cxf:http://localhost:65082/services/EchoUMO?method=echo", 
            "hello", null);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("hello", result.getPayload());
    }
}
