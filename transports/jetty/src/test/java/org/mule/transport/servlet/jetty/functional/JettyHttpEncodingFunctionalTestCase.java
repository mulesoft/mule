/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.HttpFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JettyHttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";

    public JettyHttpEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-http-encoding-test-service.xml"},
            {ConfigVariant.FLOW, "jetty-http-encoding-test-flow.xml"}
        });
    }

    @Test
    public void testSendWithProperResponseContentType() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());

        MuleMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/baz;charset=UTF-16BE", reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("UTF-16BE", reply.getEncoding());
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
    }

    /**
     * MULE-4031 - ensure the content type isn't copied
     */
    @Test
    public void testSendWithInvalidResponseContentType() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());

        MuleMessage reply = client.send("clientEndpoint2", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/plain; charset=UTF-8", reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("UTF-8", reply.getEncoding());
    }

    protected String getSendEncoding()
    {
        return "text/plain;charset=UTF-8";
    }
}
