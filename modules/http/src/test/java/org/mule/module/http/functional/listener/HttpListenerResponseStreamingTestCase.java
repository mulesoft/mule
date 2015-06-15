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
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerResponseStreamingTestCase extends FunctionalTestCase
{

    public static final String TEST_BODY = RandomStringUtils.randomAlphabetic(100*1024);
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-response-streaming-config.xml";
    }

    // AUTO - String

    @Test
    public void string() throws Exception
    {
        final String url = getUrl("string");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingHeader");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingAndContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingAndContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingHeaderAndContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingHeaderAndContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void stringWithTransferEncodingOutboundPropertyAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingOutboundPropertyAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    // AUTO  - InputStream

    @Test
    public void inputStream() throws Exception
    {
        final String url = getUrl("inputStream");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void inputStreamWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void inputStreamWithContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingHeader");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingOutboundProperty");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingAndContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingAndContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingHeaderAndContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingHeaderAndContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void inputStreamWithTransferEncodingOutboundPropertyAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingOutboundPropertyAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url);
    }

    // NEVER - String

    @Test
    public void neverString() throws Exception
    {
        final String url = getUrl("neverString");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void neverStringTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverStringTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void neverStringTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("neverStringTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    // NEVER - InputStream

    @Test
    public void neverInputStream() throws Exception
    {
        final String url = getUrl("neverInputStream");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void neverInputStreamTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverInputStreamTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url);
    }

    @Test
    public void neverInputStreamTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("neverInputStreamTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url);
    }

    // ALWAYS - String

    @Test
    public void alwaysString() throws Exception
    {
        final String url = getUrl("alwaysString");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void alwaysStringContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysStringContentLengthHeader");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void alwaysStringContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("alwaysStringContentLengthOutboundProperty");
        testResponseIsChunkedEncoding(url);
    }

    // ALWAYS - InputStream

    @Test
    public void alwaysInputStream() throws Exception
    {
        final String url = getUrl("alwaysInputStream");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void alwaysInputStreamContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysInputStreamContentLengthHeader");
        testResponseIsChunkedEncoding(url);
    }

    @Test
    public void alwaysInputStreamContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("alwaysInputStreamContentLengthOutboundProperty");
        testResponseIsChunkedEncoding(url);
    }

    private void testResponseIsContentLengthEncoding(String url) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).socketTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
        final Header contentLengthHeader = httpResponse.getFirstHeader(CONTENT_LENGTH);
        assertThat(contentLengthHeader, notNullValue());
        assertThat(transferEncodingHeader, nullValue());
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
    }

    private String getUrl(String path)
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
    }

    private void testResponseIsChunkedEncoding(String url) throws IOException
    {
        final Response response = Request.Post(url).connectTimeout(1000).socketTimeout(1000).bodyByteArray(TEST_BODY.getBytes()).execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
        final Header contentLengthHeader = httpResponse.getFirstHeader(CONTENT_LENGTH);
        assertThat(contentLengthHeader, nullValue());
        assertThat(transferEncodingHeader, notNullValue());
        assertThat(transferEncodingHeader.getValue(), is(CHUNKED));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_BODY));
    }

}
