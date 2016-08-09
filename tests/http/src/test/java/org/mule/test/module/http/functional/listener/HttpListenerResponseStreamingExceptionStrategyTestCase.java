/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerResponseStreamingExceptionStrategyTestCase extends AbstractHttpTestCase {

  public static InputStream stream;

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-response-streaming-exception-strategy-config.xml";
  }

  @BeforeClass
  public static void beforeClass() throws IOException {
    stream = mock(InputStream.class);
    when(stream.read()).thenThrow(new RuntimeException("Some exception"));
    when(stream.read(any(byte[].class))).thenThrow(new RuntimeException("Some exception"));
    when(stream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new RuntimeException("Some exception"));
  }

  @Before
  public void before() {
    TrackPassageMessageProcessor.passed = false;
  }

  protected String getUrl(String path) {
    return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
  }

  @Test
  public void exceptionHandledWhenBuildingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionBuildingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyExecuted(httpResponse);
  }

  @Test
  public void exceptionNotHandledWhenSendingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }

  @Test
  public void exceptionHandledWhenBuildingResponseFailAgain() throws Exception {
    final Response response = Get(getUrl("exceptionBuildingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT)
        .socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse);
  }

  @Test
  public void exceptionNotHandledWhenSendingResponseFailAgain() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(DEFAULT_TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }

  public static class TrackPassageMessageProcessor implements MessageProcessor {

    public static boolean passed = false;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      passed = true;
      return event;
    }
  }

  protected void assertExceptionStrategyExecuted(final HttpResponse httpResponse) throws IOException {
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("Exception Handled"));
    assertThat(TrackPassageMessageProcessor.passed, is(true));
  }

  protected void assertExceptionStrategyFailed(final HttpResponse httpResponse) throws IOException {
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_INTERNAL_SERVER_ERROR));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(""));
    assertThat(TrackPassageMessageProcessor.passed, is(true));
  }

  protected void assertExceptionStrategyNotExecuted(final HttpResponse httpResponse) throws IOException {
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_INTERNAL_SERVER_ERROR));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(""));
    assertThat(TrackPassageMessageProcessor.passed, is(false));
  }
}
