/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestStreamingTestCase extends AbstractHttpRequestTestCase
{

    private String transferEncodingHeader;
    private String contentLengthHeader;

    @Override
    protected String getConfigFile()
    {
        return "http-request-streaming-config.xml";
    }

    //AUTO

    @Test
    public void streamsWhenPayloadIsInputStreamAndStreamingModeAuto() throws Exception
    {
        assertStreaming(flowRunner("streamingAuto").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                   .run());
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAuto").withPayload(TEST_MESSAGE)
                                                     .run());
    }

    @Test
    public void doesNotStreamWithContentLengthHeaderAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAuto").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                     .withOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length())
                                                     .run());
    }

    @Test
    public void doesNotStreamStringWithContentLengthHeaderAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAuto").withPayload(TEST_MESSAGE)
                                                     .withOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length())
                                                     .run());
    }

    @Test
    public void doesNotStreamWithContentLengthTransferEncodingHeadersAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAutoHeader").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                           .withOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length())
                                                           .run());
    }

    @Test
    public void doesNotStreamStringWithContentLengthTransferEncodingHeadersAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAutoHeader").withPayload(TEST_MESSAGE)
                                                           .withOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length())
                                                           .run());
    }

    @Test
    public void streamsWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeAuto() throws Exception
    {
        assertStreaming(flowRunner("streamingAutoHeader").withPayload(TEST_MESSAGE)
                                                         .run());
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringTransferEncodingPropertyAndStreamingModeAuto() throws Exception
    {
        assertNoStreaming(flowRunner("streamingAuto").withPayload(TEST_MESSAGE)
                                                     .withOutboundProperty(TRANSFER_ENCODING, CHUNKED)
                                                     .run());
    }

    @Test
    public void streamsWhenPayloadIsInputStreamTransferEncodingHeaderAndStreamingModeAuto() throws Exception
    {
        assertStreaming(flowRunner("streamingAutoHeader").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                         .run());
    }

    @Test
    public void streamsWhenPayloadIsInputStreamTransferEncodingPropertyAndStreamingModeAuto() throws Exception
    {
        assertStreaming(flowRunner("streamingAutoHeader").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                         .withOutboundProperty(TRANSFER_ENCODING, CHUNKED)
                                                         .run());
    }

    //ALWAYS

    @Test
    public void streamsWhenStreamingModeAlways() throws Exception
    {
        assertStreaming(flowRunner("streamingAlways").withPayload(TEST_MESSAGE)
                                                     .run());
    }

    @Test
    public void streamsWhenPayloadIsInputStreamAndStreamingModeAlways() throws Exception
    {
        assertStreaming(flowRunner("streamingAlways").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                     .run());
    }

    @Test
    public void streamsWithContentLengthHeaderAndStreamingModeAlways() throws Exception
    {
        assertStreaming(
                flowRunner("streamingAlways").withPayload(TEST_MESSAGE)
                                             .withOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length())
                                             .run());
    }

    @Test
    public void streamsWithTransferEncodingInvalidValueAndStreamingModeAlways() throws Exception
    {
        assertStreaming(
                flowRunner("streamingAlways").withPayload(TEST_MESSAGE)
                                             .withOutboundProperty(TRANSFER_ENCODING, "Invalid value")
                                             .run());
    }

    //NEVER

    @Test
    public void doesNotStreamWhenStreamingModeNever() throws Exception
    {
        assertNoStreaming(flowRunner("streamingNever").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                      .run());
    }

    @Test
    public void doesNotStreamWithTransferEncodingHeaderAndStreamingModeNever() throws Exception
    {
        assertNoStreaming(flowRunner("streamingNever").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
                                                      .withOutboundProperty(TRANSFER_ENCODING, CHUNKED)
                                                      .run());
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringAndStreamingModeNever() throws Exception
    {
        assertNoStreaming(flowRunner("streamingNever").withPayload(TEST_MESSAGE)
                                                      .run());
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeNever() throws Exception
    {
        assertNoStreaming(
                flowRunner("streamingNever").withPayload(TEST_MESSAGE)
                                            .withOutboundProperty(TRANSFER_ENCODING, CHUNKED)
                                            .run());
    }

    private void assertNoStreaming(MuleEvent response) throws Exception
    {
        assertNull(transferEncodingHeader);
        assertThat(Integer.parseInt(contentLengthHeader), equalTo(TEST_MESSAGE.length()));
        assertTrue(response.getMessage().getPayload() instanceof InputStream);
        assertThat(getPayloadAsString(response.getMessage()), equalTo(DEFAULT_RESPONSE));
    }

    private void assertStreaming(MuleEvent response) throws Exception
    {
        assertThat(transferEncodingHeader, equalTo(CHUNKED));
        assertNull(contentLengthHeader);
        assertTrue(response.getMessage().getPayload() instanceof InputStream);
        assertThat(getPayloadAsString(response.getMessage()), equalTo(DEFAULT_RESPONSE));
    }


    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        transferEncodingHeader = baseRequest.getHeader(TRANSFER_ENCODING);
        contentLengthHeader = baseRequest.getHeader(CONTENT_LENGTH);

        IOUtils.toString(request.getInputStream());

        response.setContentType("text/html");
        response.setStatus(SC_OK);
        response.getWriter().print(DEFAULT_RESPONSE);
    }
}
