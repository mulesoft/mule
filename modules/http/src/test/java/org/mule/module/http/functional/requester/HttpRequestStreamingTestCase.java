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
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.util.IOUtils;

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

    @Test
    public void streamsWhenPayloadIsInputStreamAndStreamingModeAuto() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        assertStreaming("streamingAuto", event);
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringAndStreamingModeAuto() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        assertNoStreaming("streamingAuto", event);
    }

    @Test
    public void doesNotStreamWithContentLengthHeaderAndStreamingModeAuto() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        event.getMessage().setOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length());
        assertNoStreaming("streamingAuto", event);
    }

    @Test
    public void doesNotStreamWithContentLengthTransferEncodingHeadersAndStreamingModeAuto() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        event.getMessage().setOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length());
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, CHUNKED);
        assertNoStreaming("streamingAuto", event);
    }

    @Test
    public void streamsWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeAuto() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, CHUNKED);
        assertStreaming("streamingAuto", event);
    }

    @Test
    public void streamsWhenStreamingModeAlways() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        assertStreaming("streamingAlways", event);
    }

    @Test
    public void streamsWhenPayloadIsInputStreamAndStreamingModeAlways() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        assertStreaming("streamingAlways", event);
    }

    @Test
    public void streamsWithContentLengthHeaderAndStreamingModeAlways() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(CONTENT_LENGTH, TEST_MESSAGE.length());
        assertStreaming("streamingAlways", event);
    }

    @Test
    public void streamsWithTransferEncodingInvalidValueAndStreamingModeAlways() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, "Invalid value");
        assertStreaming("streamingAlways", event);
    }


    @Test
    public void doesNotStreamWhenStreamingModeNever() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        assertNoStreaming("streamingNever", event);
    }

    @Test
    public void doesNotStreamWithTransferEncodingHeaderAndStreamingModeNever() throws Exception
    {
        MuleEvent event = getTestEvent(new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, CHUNKED);
        assertNoStreaming("streamingNever", event);
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringAndStreamingModeNever() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        assertNoStreaming("streamingNever", event);
    }

    @Test
    public void doesNotStreamWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeNever() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(TRANSFER_ENCODING, CHUNKED);
        assertNoStreaming("streamingNever", event);
    }



    private void assertNoStreaming(String flowName, MuleEvent event) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent response = flow.process(event);

        assertNull(transferEncodingHeader);
        assertThat(Integer.parseInt(contentLengthHeader), equalTo(TEST_MESSAGE.length()));
        assertTrue(response.getMessage().getPayload() instanceof InputStream);
        assertThat(response.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    private void assertStreaming(String flowName, MuleEvent event) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent response = flow.process(event);

        assertThat(transferEncodingHeader, equalTo(CHUNKED));
        assertNull(contentLengthHeader);
        assertTrue(response.getMessage().getPayload() instanceof InputStream);
        assertThat(response.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
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
