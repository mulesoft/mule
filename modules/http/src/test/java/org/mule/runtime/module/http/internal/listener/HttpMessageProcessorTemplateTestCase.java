/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.runtime.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

@SmallTest
public class HttpMessageProcessorTemplateTestCase extends AbstractMuleTestCase {

  private static final String TEST_MESSAGE = "";

  private MuleContext muleContext;

  @Before
  public void before() {
    muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void statusCodeOnFailures() throws Exception {
    Event testEvent = createMockEvent();

    HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
    ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    doNothing().when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

    HttpMessageProcessorTemplate httpMessageProcessorTemplate =
        new HttpMessageProcessorTemplate(testEvent, mock(Processor.class), responseReadyCallback,
                                         HttpResponseBuilder.emptyInstance(muleContext),
                                         HttpResponseBuilder.emptyInstance(muleContext));

    httpMessageProcessorTemplate
        .sendFailureResponseToClient(new MessagingException(createStaticMessage(TEST_MESSAGE), testEvent, new RuntimeException()),
                                     null);
    assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void statusCodeOnExceptionBuildingResponse() throws Exception {
    Event testEvent = createMockEvent();

    HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
    ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    doNothing().when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

    HttpMessageProcessorTemplate httpMessageProcessorTemplate =
        new HttpMessageProcessorTemplate(testEvent, mock(Processor.class), responseReadyCallback, null,
                                         HttpResponseBuilder.emptyInstance(muleContext));

    ResponseCompletionCallback responseCompletionCallback = mock(ResponseCompletionCallback.class);
    httpMessageProcessorTemplate.sendResponseToClient(testEvent, responseCompletionCallback);

    verify(responseCompletionCallback).responseSentWithFailure(isA(MessagingException.class), eq(testEvent));
    assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void statusCodeOnExceptionSendingResponse() throws Exception {
    Event testEvent = createMockEvent();

    HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
    RuntimeException expected = new RuntimeException("Some exception");
    ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    doThrow(expected).when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

    HttpMessageProcessorTemplate httpMessageProcessorTemplate =
        new HttpMessageProcessorTemplate(testEvent, mock(Processor.class), responseReadyCallback, null,
                                         HttpResponseBuilder.emptyInstance(muleContext));

    ResponseCompletionCallback responseCompletionCallback = mock(ResponseCompletionCallback.class);

    try {
      httpMessageProcessorTemplate.sendResponseToClient(testEvent, responseCompletionCallback);
      fail("Expected exception");
    } catch (RuntimeException e) {
      assertThat(e, sameInstance(expected));
    }

    verify(responseCompletionCallback, never()).responseSentWithFailure(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof MessagingException && ((MessagingException) o).getCauseException().equals(expected);
      }
    }), eq(testEvent));
    assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  private Event createMockEvent() throws Exception {
    InternalMessage testMessage = InternalMessage.builder().payload("").build();

    Event testEvent =
        spy(Event.builder(DefaultEventContext.create(MuleTestUtils.getTestFlow(muleContext), TEST_CONNECTOR))
            .message(testMessage).build());
    when(muleContext.getTransformationService().transform(any(InternalMessage.class), any(DataType.class)))
        .thenReturn(InternalMessage.builder().payload("".getBytes(UTF_8)).build());
    return testEvent;
  }

}
