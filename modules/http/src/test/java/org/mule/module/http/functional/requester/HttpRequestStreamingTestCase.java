/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.functional.TestInputStream;
import org.mule.util.IOUtils;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestStreamingTestCase extends AbstractHttpRequestTestCase
{

    private String transferEncodingHeader;
    private String contentLengthHeader;

    private Latch requestReceivedLatch = new Latch();
    private Latch streamLatch = new Latch();

    @Override
    protected String getConfigFile()
    {
        return "http-request-streaming-config.xml";
    }

    @Test
    public void streamsRequestWhenPayloadIsInputStream() throws Exception
    {
        MuleClient client = muleContext.getClient();
        TestInputStream testInputStream = new TestInputStream(streamLatch);

        client.dispatch("vm://streamingDefaultIn", testInputStream, null);

        assertTrue("Request was never received in the server", requestReceivedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertThat(transferEncodingHeader, equalTo("chunked"));

        MuleMessage message = client.request("vm://streamingDefaultOut", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof InputStream);
        assertThat(message.getPayloadAsString(), equalTo(DEFAULT_RESPONSE));

    }

    @Test
    public void doesNotStreamRequestWhenPayloadIsString() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://streamingDefaultIn", TEST_MESSAGE, null);

        assertTrue("Request was never received in the server", requestReceivedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertNull(transferEncodingHeader);
        assertThat(Integer.parseInt(contentLengthHeader), equalTo(TEST_MESSAGE.length()));

        MuleMessage message = client.request("vm://streamingDefaultOut", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof InputStream);
        assertThat(message.getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void streamsWhenRequestStreamingTrue() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://streamingTrueIn", TEST_MESSAGE, null);

        assertTrue("Request was never received in the server", requestReceivedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertThat(transferEncodingHeader, equalTo("chunked"));

        MuleMessage message = client.request("vm://streamingTrueOut", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof InputStream);
        assertThat(message.getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void doesNotStreamWhenRequestStreamingFalse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream payload = new ByteArrayInputStream(TEST_MESSAGE.getBytes());

        client.dispatch("vm://streamingFalseIn", payload, null);

        assertTrue("Request was never received in the server", requestReceivedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertNull(transferEncodingHeader);
        assertThat(Integer.parseInt(contentLengthHeader), equalTo(TEST_MESSAGE.length()));

        MuleMessage message = client.request("vm://streamingFalseOut", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof InputStream);
        assertThat(message.getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        transferEncodingHeader = baseRequest.getHeader("Transfer-Encoding");
        contentLengthHeader = baseRequest.getHeader("Content-Length");

        requestReceivedLatch.release();
        streamLatch.release();

        IOUtils.toString(request.getInputStream());

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(DEFAULT_RESPONSE);
    }
}
