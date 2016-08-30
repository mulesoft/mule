/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ExceptionHandlingReplyToHandlerDecoratorTestCase extends AbstractMuleTestCase {

  @Mock
  private MessagingExceptionHandler messagingExceptionHandler;
  @Mock
  private FlowConstruct flow;
  @Mock
  private MuleEvent sourceEvent;
  @Mock
  private MuleEvent handledEvent;

  @Test
  public void handleException() {
    SensingNullReplyToHandler sensingReplyToHandler = new SensingNullReplyToHandler();
    ExceptionHandlingReplyToHandlerDecorator errorHandlingreplyToHandler =
        new ExceptionHandlingReplyToHandlerDecorator(sensingReplyToHandler, messagingExceptionHandler, flow);
    MessagingException messagingException = new MessagingException(sourceEvent, new RuntimeException());

    when(messagingExceptionHandler.handleException(messagingException, sourceEvent)).thenAnswer(invocation -> handledEvent);

    errorHandlingreplyToHandler.processExceptionReplyTo(messagingException, null);

    verify(messagingExceptionHandler, times(1)).handleException(messagingException, sourceEvent);
    assertThat(sensingReplyToHandler.exception, CoreMatchers.<Exception>equalTo(messagingException));
    assertThat(((MessagingException) sensingReplyToHandler.exception).getEvent(), equalTo(handledEvent));
    assertThat(sensingReplyToHandler.event, nullValue());
  }

  @Test
  public void handleExceptionAndMarkHandled() {
    SensingNullReplyToHandler sensingReplyToHandler = new SensingNullReplyToHandler();
    ExceptionHandlingReplyToHandlerDecorator errorHandlingReplyToHandler =
        new ExceptionHandlingReplyToHandlerDecorator(sensingReplyToHandler, messagingExceptionHandler, flow);
    MessagingException messagingException = new MessagingException(sourceEvent, new RuntimeException());

    when(messagingExceptionHandler.handleException(messagingException, sourceEvent)).thenAnswer(invocation -> {
      ((MessagingException) invocation.getArguments()[0]).setHandled(true);
      return handledEvent;
    });

    errorHandlingReplyToHandler.processExceptionReplyTo(messagingException, null);

    verify(messagingExceptionHandler, times(1)).handleException(messagingException, sourceEvent);
    assertThat(sensingReplyToHandler.exception, nullValue());
    assertThat(sensingReplyToHandler.event, equalTo(handledEvent));
  }

}
