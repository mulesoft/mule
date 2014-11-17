/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.HttpHeaders;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerResponseStreamingTestCase extends FunctionalTestCase
{

    public static final String TEST_BODY = "a message";
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-response-streaming-config.xml";
    }

    @Test
    public void byDefaultInputStreamPayloadDoesStreaming() throws Exception
    {
        final String url = getUrl("default");
        testResponseIsChunked(url);
    }

    @Test
    public void withTransferEncodingAndinputStreamPayloadDoesStreaming() throws Exception
    {
        final String url = getUrl("withHeaderTransferEncoding");
        testResponseIsChunked(url);
    }

    @Test
    public void withTransferEncodingAndStringPayloadDoesStreaming() throws Exception
    {
        final String url = getUrl("withHeaderTransferEncodingAndStringPayload");
        testResponseIsChunked(url);
    }

    @Test
    public void withOutboundPropertyTransferEncodingAndStringPayload() throws Exception
    {
        final String url = getUrl("withOutboundPropertyTransferEncodingAndStringPayload");
        testResponseIsChunked(url);
    }

    @Test
    public void neverStreamWithInputStreamPayload() throws Exception
    {
        final String url = getUrl("neverStreamWithInputStreamPayload");
        testResponseIsNotChunked(url);
    }

    @Test
    public void neverStreamWithOutboundPropertyAndInputStreamPayload() throws Exception
    {
        final String url = getUrl("neverStreamWithOutboundPropertyAndInputStreamPayload");
        testResponseIsChunked(url);
    }

    @Test
    public void neverStreamWithHeaderTransferEncodingAndInputStreamPayload() throws Exception
    {
        final String url = getUrl("neverStreamWithHeaderTransferEncodingAndInputStreamPayload");
        testResponseIsChunked(url);
    }

    @Test
    public void alwaysStreamWithStringPayload() throws Exception
    {
        final String url = getUrl("alwaysStreamWithStringPayload");
        testResponseIsChunked(url);
    }

    private void testResponseIsNotChunked(String url) throws IOException
    {
        final Response response = Request.Post(url).connectTimeout(1000).socketTimeout(1000).bodyByteArray(TEST_BODY.getBytes()).execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(HttpHeaders.Names.TRANSFER_ENCODING);
        final Header contentLengthHeader = httpResponse.getFirstHeader(HttpHeaders.Names.CONTENT_LENGTH);
        assertThat(contentLengthHeader, notNullValue());
        assertThat(transferEncodingHeader, nullValue());
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
    }

    private String getUrl(String path)
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
    }

    private void testResponseIsChunked(String url) throws IOException
    {
        final Response response = Request.Post(url).connectTimeout(1000).socketTimeout(1000).bodyByteArray(TEST_BODY.getBytes()).execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(HttpHeaders.Names.TRANSFER_ENCODING);
        final Header contentLengthHeader = httpResponse.getFirstHeader(HttpHeaders.Names.CONTENT_LENGTH);
        assertThat(contentLengthHeader, nullValue());
        assertThat(transferEncodingHeader, notNullValue());
        assertThat(transferEncodingHeader.getValue(), is(HttpHeaders.Values.CHUNKED));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
    }

}
