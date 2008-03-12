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
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;

/**
 * Tests the Echo example using a SOAP transport.
 */
public abstract class AbstractEchoTestCase extends FunctionalTestCase
{
    private String expectedGetResponse;
    private String expectedPostResponse;

    protected void doSetUp() throws Exception
    {
        try
        {
            expectedGetResponse = IOUtils.getResourceAsString(getExpectedGetResponseResource(),
                this.getClass());
            expectedPostResponse = IOUtils.getResourceAsString(getExpectedPostResponseResource(),
                this.getClass());
        }
        catch (IOException ioex)
        {
            fail(ioex.getMessage());
        }
    }

    protected abstract String getExpectedGetResponseResource();

    protected abstract String getExpectedPostResponseResource();

    protected abstract String getProtocol();

    public void testPostEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:65081/services/EchoUMO?method=echo", "hello", null);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLAssert.assertXMLEqual(expectedPostResponse, result.getPayloadAsString());
    }

    public void testGetEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:65081/services/EchoUMO?method=echo", "hello", props);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        
        XMLAssert.assertXMLEqual(expectedGetResponse, result.getPayloadAsString());
    }

    public void testSoapPostEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send(
            getProtocol() + ":http://localhost:65082/services/EchoUMO?method=echo", "hello", null);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("hello", result.getPayload());
    }
}
