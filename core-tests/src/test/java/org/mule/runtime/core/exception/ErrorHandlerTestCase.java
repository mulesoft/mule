/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.exception.ErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@SmallTest
@Features(ERROR_HANDLING)
@Stories(ERROR_HANDLER)
@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy1;
  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy2;
  private DefaultMessagingExceptionHandlerAcceptor defaultMessagingExceptionHandler =
      spy(new DefaultMessagingExceptionHandlerAcceptor());
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Event mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;
  @Mock
  private ErrorType mockErrorType;
  private MessagingException mockException;

  @Before
  public void before() throws MuleException {
    when(mockMuleEvent.getMessage()).thenReturn(Message.of(""));
    when(mockMuleEvent.getMuleContext()).thenReturn(mockMuleContext);
    Error mockError = mock(Error.class);
    when(mockError.getErrorType()).thenReturn(mockErrorType);
    when(mockMuleEvent.getError()).thenReturn(of(mockError));
    mockException = new MessagingException(mockMuleEvent, new Exception());
    Event handledEvent = testEvent();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(true);
    when(mockTestExceptionStrategy1.apply(any(MessagingException.class))).thenReturn(just(handledEvent));
    when(mockTestExceptionStrategy2.accept(any(Event.class))).thenReturn(true);
    when(mockTestExceptionStrategy2.apply(any(MessagingException.class))).thenReturn(just(handledEvent));
  }

  @Test
  public void nonMatchThenCallDefault() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(any(Event.class))).thenReturn(false);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(mockTestExceptionStrategy2, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(1)).handleException(eq(mockException), any(Event.class));
  }

  @Test
  public void secondMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(false);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
    verify(mockTestExceptionStrategy2, times(1)).handleException(eq(mockException), any(Event.class));
  }

  @Test(expected = MuleRuntimeException.class)
  public void firstAcceptsAllMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(defaultMessagingExceptionHandler);
    when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
    when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
    errorHandler.initialise();
  }

  @Test
  public void criticalIsNotHandled() throws Exception {
    when(mockErrorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
  }

  @Test
  public void defaultErrorHandler() throws InitialisationException {
    final ErrorHandler defaultHandler = new ErrorHandlerFactory().createDefault();

    assertThat(defaultHandler.getExceptionListeners(), hasSize(1));
    assertThat(defaultHandler.getExceptionListeners(), hasItem(instanceOf(OnErrorPropagateHandler.class)));
  }

  class DefaultMessagingExceptionHandlerAcceptor implements MessagingExceptionHandlerAcceptor {

    @Override
    public boolean accept(Event event) {
      return true;
    }

    @Override
    public boolean acceptsAll() {
      return true;
    }

    @Override
    public Event handleException(MessagingException exception, Event event) {
      exception.setHandled(true);
      return event;
    }
  }

}
