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

public class HttpListenerOutputHandlerResponseStreaming11TestCase extends HttpListenerResponseStreamingTestCase
{

  @Override
  protected HttpVersion getHttpVersion()
  {
    return HTTP_1_1;
  }

  @Override
  protected String getConfigFile()
  {
    return "http-listener-output-handler-response-streaming-config.xml";
  }

  // AUTO

  @Test
  public void outputHandler() throws Exception
  {
    final String url = getUrl("outputHandler");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithContentLengthHeader() throws Exception
  {
    final String url = getUrl("outputHandlerWithContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("outputHandlerWithContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingHeader() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingHeader");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingOutboundProperty() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingOutboundProperty");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingAndContentLengthHeader() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingAndContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingAndContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingAndContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingHeaderAndContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingHeaderAndContentLengthOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void outputHandlerWithTransferEncodingOutboundPropertyAndContentLengthHeader() throws Exception
  {
    final String url = getUrl("outputHandlerWithTransferEncodingOutboundPropertyAndContentLengthHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  // NEVER

  @Test
  public void neverOutputHandler() throws Exception
  {
    final String url = getUrl("neverOutputHandler");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void neverOutputHandlerTransferEncodingHeader() throws Exception
  {
    final String url = getUrl("neverOutputHandlerTransferEncodingHeader");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  @Test
  public void neverOutputHandlerTransferEncodingOutboundProperty() throws Exception
  {
    final String url = getUrl("neverOutputHandlerTransferEncodingOutboundProperty");
    testResponseIsContentLengthEncoding(url, getHttpVersion());
  }

  // ALWAYS

  @Test
  public void alwaysOutputHandler() throws Exception
  {
    final String url = getUrl("alwaysOutputHandler");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

  @Test
  public void alwaysOutputHandlerContentLengthHeader() throws Exception
  {
    final String url = getUrl("alwaysOutputHandlerContentLengthHeader");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

  @Test
  public void alwaysOutputHandlerContentLengthOutboundProperty() throws Exception
  {
    final String url = getUrl("alwaysOutputHandlerContentLengthOutboundProperty");
    testResponseIsChunkedEncoding(url, getHttpVersion());
  }

}
