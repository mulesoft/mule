/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class AbstractHttpListenerErrorHandlingTestCase extends AbstractHttpTestCase {

  public static InputStream stream;

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @BeforeClass
  public static void beforeClass() throws IOException {
    stream = mock(InputStream.class);
    when(stream.read()).thenThrow(new IOException("Some exception"));
    when(stream.read(any(byte[].class))).thenThrow(new IOException("Some exception"));
    when(stream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Some exception"));
  }

  @Before
  public void before() {
    TrackPassageMessageProcessor.passed = false;
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
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), containsString(""));
    assertThat(TrackPassageMessageProcessor.passed, is(false));
  }

  protected String getUrl(String path) {
    return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
  }

  public static class TrackPassageMessageProcessor implements Processor {

    public static boolean passed = false;

    @Override
    public Event process(Event event) throws MuleException {
      passed = true;
      return event;
    }

  }

}
