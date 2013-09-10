/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    private static String TEST_JAPANESE_MESSAGE = "\u3042";

    public HttpEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }  

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-encoding-test-service.xml"},
            {ConfigVariant.FLOW, "http-encoding-test-flow.xml"}
        });
    }      

    @Override
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());

        MuleMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/baz;charset=UTF-16BE", reply.<String>getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("UTF-16BE", reply.getEncoding());
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
    }

    @Test
    public void testPostEncodingUsAscii() throws Exception
    {
        runPostEncodingTest("US-ASCII", "A");
    }

    @Test
    public void testPostEncodingUtf8() throws Exception
    {
        runPostEncodingTest("UTF-8", "A");
        runPostEncodingTest("UTF-8", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingShiftJs() throws Exception
    {
        runPostEncodingTest("Shift_JIS", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingWindows31J() throws Exception
    {
        runPostEncodingTest("Windows-31J", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingEucJp() throws Exception
    {
        runPostEncodingTest("EUC-JP", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingUsAscii() throws Exception
    {
        runGetEncodingTest("US-ASCII", "A");
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingUtf8() throws Exception
    {
        runGetEncodingTest("UTF-8", "A");
        runGetEncodingTest("UTF-8", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingShiftJs() throws Exception
    {
        runGetEncodingTest("Shift_JIS", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingWindows31J() throws Exception
    {
        runGetEncodingTest("Windows-31J", TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingEucJp() throws Exception
    {
        runGetEncodingTest("EUC-JP", TEST_JAPANESE_MESSAGE);
    }

    private void runPostEncodingTest(String encoding, String payload) throws Exception
    {
        MuleMessage reply = runEncodingTest(encoding, payload, HttpConstants.METHOD_POST);
        assertEquals(payload + " Received", reply.getPayloadAsString());
    }

    private void runGetEncodingTest(String encoding, String payload) throws Exception
    {
        MuleMessage reply = runEncodingTest(encoding, payload, HttpConstants.METHOD_GET);

        String expectedReplyMessage = "/" + encoding + "?body=" + URLEncoder.encode(payload, encoding);
        assertEquals(expectedReplyMessage + " Received", reply.getPayloadAsString());
    }

    private MuleMessage runEncodingTest(String encoding, String payload, String httpMethod) throws Exception
    {
        Map<String, Object> messageProperties = createMessageProperties(encoding, httpMethod);

        MuleClient client = muleContext.getClient();
        String endpointUri = "clientEndpoint." + encoding;
        MuleMessage reply = client.send(endpointUri, payload, messageProperties);

        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        Object contentTypeHeader = reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        assertEquals("text/plain;charset=" + encoding, contentTypeHeader);

        assertEquals(encoding, reply.getEncoding());

        return reply;
    }

    private Map<String, Object> createMessageProperties(String encoding, String httpMethod)
    {
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        String contentType = "text/plain;charset=" + encoding;
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        messageProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, httpMethod);
        return messageProperties;
    }

    protected String getSendEncoding()
    {
        return "text/plain;charset=UTF-8";
    }
}
