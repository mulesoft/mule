/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionFlowProcessingTemplateTestCase extends AbstractMuleTestCase {

  @Mock
  private Message message;

  @Mock
  private Event event;

  @Mock
  private Processor messageProcessor;

  @Mock
  private SourceCompletionHandler completionHandler;

  @Mock
  private ResponseCompletionCallback responseCompletionCallback;

  @Mock
  private MessagingException messagingException;

  @Mock
  private Map<String, Object> mockParameters;

  private RuntimeException runtimeException = new RuntimeException();

  private ModuleFlowProcessingTemplate template;

  @Before
  public void before() throws Exception {
    template = new ModuleFlowProcessingTemplate(message, messageProcessor, completionHandler);
    when(completionHandler.onCompletion(any(), any())).thenReturn(Mono.empty());
    when(completionHandler.onFailure(any(), any())).thenReturn(Mono.empty());
  }

  @Test
  public void getMuleEvent() throws Exception {
    assertThat(template.getMessage(), is(sameInstance(message)));
  }

  @Test
  public void routeEvent() throws Exception {
    template.routeEvent(event);
    verify(messageProcessor).process(event);
  }

  @Test
  public void routeEventAsync() throws Exception {
    when(messageProcessor.apply(any(Publisher.class))).thenReturn(just(event));
    template.routeEventAsync(event);
    verify(messageProcessor).apply(any(Publisher.class));
  }

  @Test
  public void sendResponseToClient() throws Exception {
    from(template.sendResponseToClient(event, mockParameters, (event) -> mockParameters, responseCompletionCallback)).block();
    verify(completionHandler).onCompletion(same(event), same(mockParameters));
    verify(responseCompletionCallback).responseSentSuccessfully();
  }

  @Test
  public void failedToSendResponseToClient() throws Exception {
    when(completionHandler.onCompletion(same(event), same(mockParameters))).thenReturn(error(runtimeException));
    from(template.sendResponseToClient(event, mockParameters, (event) -> mockParameters, responseCompletionCallback)).subscribe();

    verify(completionHandler, never()).onFailure(any(MessagingException.class), same(mockParameters));
    verify(responseCompletionCallback).responseSentWithFailure(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof MessagingException && ((MessagingException) o).getRootCause().equals(runtimeException);
      }
    }), eq(event));
  }

  @Test
  public void sendFailureResponseToClient() throws Exception {
    from(template.sendFailureResponseToClient(messagingException, mockParameters, responseCompletionCallback)).block();
    verify(completionHandler).onFailure(messagingException, mockParameters);
    verify(responseCompletionCallback).responseSentSuccessfully();
  }

  @Test
  public void failedToSendFailureResponseToClient() throws Exception {
    when(messagingException.getEvent()).thenReturn(event);
    when(completionHandler.onFailure(messagingException, mockParameters)).thenReturn(error(runtimeException));
    from(template.sendFailureResponseToClient(messagingException, mockParameters, responseCompletionCallback)).subscribe();
    verify(responseCompletionCallback).responseSentWithFailure(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof MessagingException && ((MessagingException) o).getRootCause().equals(runtimeException);
      }
    }), eq(event));
  }
}
