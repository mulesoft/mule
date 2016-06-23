/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class HttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    private static String TEST_JAPANESE_MESSAGE = "\u3042";

    public HttpEncodingFunctionalTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-encoding-test-flow.xml";
    }

    @Override
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Serializable> messageProperties = new HashMap<>();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());

        MuleMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/baz;charset=UTF-16BE", reply.<String>getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertThat(reply.getDataType().getMimeType().getEncoding().get(), is(UTF_16BE));
        assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(reply));
    }

    @Test
    public void testPostEncodingUsAscii() throws Exception
    {
        runPostEncodingTest(US_ASCII, "A");
    }

    @Test
    public void testPostEncodingUtf8() throws Exception
    {
        runPostEncodingTest(UTF_8, "A");
        runPostEncodingTest(UTF_8, TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingShiftJs() throws Exception
    {
        runPostEncodingTest(Charset.forName("Shift_JIS"), TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingWindows31J() throws Exception
    {
        runPostEncodingTest(Charset.forName("Windows-31J"), TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testPostEncodingEucJp() throws Exception
    {
        runPostEncodingTest(Charset.forName("EUC-JP"), TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingUsAscii() throws Exception
    {
        runGetEncodingTest(US_ASCII, "A");
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingUtf8() throws Exception
    {
        runGetEncodingTest(UTF_8, "A");
        runGetEncodingTest(UTF_8, TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingShiftJs() throws Exception
    {
        runGetEncodingTest(Charset.forName("Shift_JIS"), TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingWindows31J() throws Exception
    {
        runGetEncodingTest(Charset.forName("Windows-31J"), TEST_JAPANESE_MESSAGE);
    }

    @Test
    @Ignore("MULE-3690 make me run green")
    public void testGetEncodingEucJp() throws Exception
    {
        runGetEncodingTest(Charset.forName("EUC-JP"), TEST_JAPANESE_MESSAGE);
    }

    private void runPostEncodingTest(Charset encoding, String payload) throws Exception
    {
        MuleMessage reply = runEncodingTest(encoding, payload, HttpConstants.METHOD_POST);
        assertEquals(payload + " Received", getPayloadAsString(reply));
    }

    private void runGetEncodingTest(Charset encoding, String payload) throws Exception
    {
        MuleMessage reply = runEncodingTest(encoding, payload, HttpConstants.METHOD_GET);

        String expectedReplyMessage = "/" + encoding + "?body=" + URLEncoder.encode(payload, encoding.name());
        assertEquals(expectedReplyMessage + " Received", getPayloadAsString(reply));
    }

    private MuleMessage runEncodingTest(Charset encoding, String payload, String httpMethod) throws Exception
    {
        Map<String, Serializable> messageProperties = createMessageProperties(encoding, httpMethod);

        MuleClient client = muleContext.getClient();
        String endpointUri = "clientEndpoint." + encoding.name();
        MuleMessage reply = client.send(endpointUri, payload, messageProperties);

        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        Object contentTypeHeader = reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        assertEquals("text/plain;charset=" + encoding.name(), contentTypeHeader);

        assertThat(reply.getDataType().getMimeType().getEncoding().get(), is(encoding));

        return reply;
    }

    private Map<String, Serializable> createMessageProperties(Charset encoding, String httpMethod)
    {
        Map<String, Serializable> messageProperties = new HashMap<>();
        String contentType = "text/plain;charset=" + encoding.name();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        messageProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, httpMethod);
        return messageProperties;
    }

    protected String getSendEncoding()
    {
        return "text/plain;charset=UTF-8";
    }
}
