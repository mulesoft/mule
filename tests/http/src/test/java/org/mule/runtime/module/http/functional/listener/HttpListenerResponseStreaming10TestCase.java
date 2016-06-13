/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.listener;

import java.io.IOException;

import org.apache.http.HttpVersion;
import org.junit.Test;

public class HttpListenerResponseStreaming10TestCase extends HttpListenerResponseStreamingTestCase
{

    @Override
    protected HttpVersion getHttpVersion()
    {
        return HttpVersion.HTTP_1_0;
    }

    // AUTO - String

    @Test
    public void string() throws Exception
    {
        final String url = getUrl("string");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithTransferEncodingAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // AUTO  - InputStream

    @Test
    public void inputStream() throws Exception
    {
        final String url = getUrl("inputStream");
        testResponseIsNotChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingHeader");
        testResponseIsNotChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithTransferEncodingAndContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingAndContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // NEVER - String

    @Test
    public void neverString() throws Exception
    {
        final String url = getUrl("neverString");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverStringTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverStringTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // NEVER - InputStream

    @Test
    public void neverInputStream() throws Exception
    {
        final String url = getUrl("neverInputStream");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverInputStreamTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverInputStreamTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // ALWAYS - String

    /**
     * Last paragraph of <a href="http://tools.ietf.org/html/rfc2068#section-3.6">rfc2068#section-3.6</a> states:
     *  A server MUST NOT send transfer-codings to an HTTP/1.0 client.
     * @throws IOException
     */
    @Test
    public void alwaysString() throws Exception
    {
        final String url = getUrl("alwaysString");
        testResponseIsNotChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysStringContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysStringContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // ALWAYS - InputStream

    @Test
    public void alwaysInputStream() throws Exception
    {
        final String url = getUrl("alwaysInputStream");
        testResponseIsNotChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysInputStreamContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysInputStreamContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

}
