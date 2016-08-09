/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;


import static java.lang.String.valueOf;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestStreamingTestCase extends AbstractHttpRequestTestCase {

  private static final String HEADERS = "headers";
  private String transferEncodingHeader;
  private String contentLengthHeader;
  private Map<String, String> headersToSend = new HashMap<>();

  @Override
  protected String getConfigFile() {
    return "http-request-streaming-config.xml";
  }

  // AUTO

  @Test
  public void streamsWhenPayloadIsInputStreamAndStreamingModeAuto() throws Exception {
    assertStreaming(runFlowWithPayload("streamingAuto", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  @Test
  public void doesNotStreamWhenPayloadIsStringAndStreamingModeAuto() throws Exception {
    assertNoStreaming(runFlowWithPayload("streamingAuto", TEST_MESSAGE));
  }

  @Test
  public void doesNotStreamWithContentLengthHeaderAndStreamingModeAuto() throws Exception {
    addHeader(CONTENT_LENGTH, valueOf(TEST_MESSAGE.length()));
    assertNoStreaming(runFlowWithPayload("streamingAuto", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  @Test
  public void doesNotStreamStringWithContentLengthHeaderAndStreamingModeAuto() throws Exception {
    addHeader(CONTENT_LENGTH, valueOf(TEST_MESSAGE.length()));
    assertNoStreaming(runFlowWithPayload("streamingAuto", TEST_MESSAGE));
  }

  @Test
  public void doesNotStreamWithContentLengthTransferEncodingHeadersAndStreamingModeAuto() throws Exception {
    assertNoStreaming(flowRunner("streamingAutoBothHeaders").withPayload(new ByteArrayInputStream(TEST_MESSAGE.getBytes()))
        .run());
  }

  @Test
  public void doesNotStreamStringWithContentLengthTransferEncodingHeadersAndStreamingModeAuto() throws Exception {
    assertNoStreaming(flowRunner("streamingAutoBothHeaders").withPayload(TEST_MESSAGE).run());
  }

  @Test
  public void streamsWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeAuto() throws Exception {
    addHeader(TRANSFER_ENCODING, CHUNKED);
    assertStreaming(runFlowWithPayload("streamingAuto", TEST_MESSAGE));
  }

  @Test
  public void streamsWhenPayloadIsInputStreamTransferEncodingHeaderAndStreamingModeAuto() throws Exception {
    addHeader(TRANSFER_ENCODING, CHUNKED);
    assertStreaming(runFlowWithPayload("streamingAuto", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  // ALWAYS

  @Test
  public void streamsWhenStreamingModeAlways() throws Exception {
    assertStreaming(runFlowWithPayload("streamingAlways", TEST_MESSAGE));
  }

  @Test
  public void streamsWhenPayloadIsInputStreamAndStreamingModeAlways() throws Exception {
    assertStreaming(runFlowWithPayload("streamingAlways", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  @Test
  public void streamsWithContentLengthHeaderAndStreamingModeAlways() throws Exception {
    addHeader(CONTENT_LENGTH, valueOf(TEST_MESSAGE.length()));
    assertStreaming(runFlowWithPayload("streamingAlways", TEST_MESSAGE));
  }

  @Test
  public void streamsWithTransferEncodingInvalidValueAndStreamingModeAlways() throws Exception {
    addHeader(TRANSFER_ENCODING, "Invalid value");
    assertStreaming(runFlowWithPayload("streamingAlways", TEST_MESSAGE));
  }

  // NEVER

  @Test
  public void doesNotStreamWhenStreamingModeNever() throws Exception {
    assertNoStreaming(runFlowWithPayload("streamingNever", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  @Test
  public void doesNotStreamWithTransferEncodingHeaderAndStreamingModeNever() throws Exception {
    addHeader(TRANSFER_ENCODING, CHUNKED);
    assertNoStreaming(runFlowWithPayload("streamingNever", new ByteArrayInputStream(TEST_MESSAGE.getBytes())));
  }

  @Test
  public void doesNotStreamWhenPayloadIsStringAndStreamingModeNever() throws Exception {
    assertNoStreaming(runFlowWithPayload("streamingNever", TEST_MESSAGE));
  }

  @Test
  public void doesNotStreamWhenPayloadIsStringTransferEncodingHeaderAndStreamingModeNever() throws Exception {
    addHeader(TRANSFER_ENCODING, CHUNKED);
    assertNoStreaming(runFlowWithPayload("streamingNever", TEST_MESSAGE));
  }

  public MuleEvent runFlowWithPayload(String flow, Object payload) throws Exception {
    return flowRunner(flow).withPayload(payload).withFlowVariable(HEADERS, headersToSend).run();
  }

  private void addHeader(String name, String value) {
    headersToSend.put(name, value);
  }

  private void assertNoStreaming(MuleEvent response) throws Exception {
    assertNull(transferEncodingHeader);
    assertThat(Integer.parseInt(contentLengthHeader), equalTo(TEST_MESSAGE.length()));
    assertTrue(response.getMessage().getPayload() instanceof InputStream);
    assertThat(getPayloadAsString(response.getMessage()), equalTo(DEFAULT_RESPONSE));
  }

  private void assertStreaming(MuleEvent response) throws Exception {
    assertThat(transferEncodingHeader, equalTo(CHUNKED));
    assertNull(contentLengthHeader);
    assertTrue(response.getMessage().getPayload() instanceof InputStream);
    assertThat(getPayloadAsString(response.getMessage()), equalTo(DEFAULT_RESPONSE));
  }


  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    transferEncodingHeader = baseRequest.getHeader(TRANSFER_ENCODING);
    contentLengthHeader = baseRequest.getHeader(CONTENT_LENGTH);

    IOUtils.toString(request.getInputStream());

    response.setContentType("text/html");
    response.setStatus(SC_OK);
    response.getWriter().print(DEFAULT_RESPONSE);
  }
}
