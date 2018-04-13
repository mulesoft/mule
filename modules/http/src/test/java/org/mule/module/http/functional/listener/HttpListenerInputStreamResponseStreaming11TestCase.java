/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.apache.http.HttpVersion.HTTP_1_1;

import org.apache.http.HttpVersion;
import org.junit.Test;

public class HttpListenerInputStreamResponseStreaming11TestCase extends HttpListenerResponseStreamingTestCase
{

  @Override
  protected HttpVersion getHttpVersion()
  {
    return HTTP_1_1;
  }

  @Override
  protected String getConfigFile()
  {
    return "http-listener-input-stream-response-streaming-config.xml";
  }

  // AUTO

  @Test
  public void inputStream() throws Exception
  {
    final String url = getUrl("inputStream");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithContentLengthHeader() throws Exception
  {
    final String url = getUrl("inputStreamWithContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("inputStreamWithContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingHeader() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingHeader");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingOutboundProperty() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingOutboundProperty");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingAndContentLengthHeader() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingAndContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingAndContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingAndContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingHeaderAndContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingHeaderAndContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void inputStreamWithTransferEncodingOutboundPropertyAndContentLengthHeader() throws Exception
  {
    final String url = getUrl("inputStreamWithTransferEncodingOutboundPropertyAndContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  // NEVER

  @Test
  public void neverInputStream() throws Exception
  {
    final String url = getUrl("neverInputStream");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void neverInputStreamTransferEncodingHeader() throws Exception
  {
    final String url = getUrl("neverInputStreamTransferEncodingHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void neverInputStreamTransferEncodingOutboundProperty() throws Exception
  {
    final String url = getUrl("neverInputStreamTransferEncodingOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  // ALWAYS

  @Test
  public void alwaysInputStream() throws Exception
  {
    final String url = getUrl("alwaysInputStream");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void alwaysInputStreamContentLengthHeader() throws Exception
  {
    final String url = getUrl("alwaysInputStreamContentLengthHeader");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

  @Test
  public void alwaysInputStreamContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("alwaysInputStreamContentLengthOutboundProperty");
    testResponseIsChunkedEncoding(url, getHttpVersion());
    streamIsClosed();
  }

}
