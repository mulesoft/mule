/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.echo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.IOUtils;

/**
 * Tests the Echo example using Axis
 */
public class AxisEchoTestCase extends FunctionalTestCase
{

    private String expectedGetResponse;
    private String expectedPostResponse;

    public AxisEchoTestCase()
    {
        this.setDisposeManagerPerSuite(true);
    }

    protected void doPreFunctionalSetUp() throws Exception
    {
        super.doPreFunctionalSetUp();

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

    protected String getExpectedGetResponseResource()
    {
        return "echo-axis-get-response.xml";
    }

    protected String getExpectedPostResponseResource()
    {
        return "echo-axis-post-response.xml";
    }

    protected String getConfigResources()
    {
        return "echo-axis-config.xml";
    }

    protected String getProtocol()
    {
        return "axis";
    }

    public void testPostEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("http://localhost:8081/services/EchoUMO?method=echo", "hello", null);
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
        UMOMessage result = client.send("http://localhost:8081/services/EchoUMO?method=echo", "hello", props);
        assertNotNull(result);
        // TODO: MULE-1113
        if (!(this instanceof XFireEchoTestCase))
        {
            assertNull(result.getExceptionPayload());
        }
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLAssert.assertXMLEqual(expectedGetResponse, result.getPayloadAsString());
    }

    public void testSoapPostEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(
            getProtocol() + ":http://localhost:8082/services/EchoUMO?method=echo", "hello", null);
        assertNotNull(result);
        // TODO: MULE-1113
        if (!(this instanceof XFireEchoTestCase))
        {
            assertNull(result.getExceptionPayload());
        }
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("hello", result.getPayload());
    }

}
