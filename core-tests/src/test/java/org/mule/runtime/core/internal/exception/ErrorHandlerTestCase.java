/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
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
import static org.mule.runtime.core.api.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.NotificationDispatcher;
import org.mule.runtime.core.api.exception.MessagingException;
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

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy1;
  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy2;
  private DefaultMessagingExceptionHandlerAcceptor defaultMessagingExceptionHandler =
      spy(new DefaultMessagingExceptionHandlerAcceptor());
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private InternalEvent mockMuleEvent;
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
    InternalEvent handledEvent = testEvent();
    when(mockTestExceptionStrategy1.accept(any(InternalEvent.class))).thenReturn(true);
    when(mockTestExceptionStrategy1.apply(any(MessagingException.class))).thenReturn(just(handledEvent));
    when(mockTestExceptionStrategy2.accept(any(InternalEvent.class))).thenReturn(true);
    when(mockTestExceptionStrategy2.apply(any(MessagingException.class))).thenReturn(just(handledEvent));
  }

  @Test
  public void nonMatchThenCallDefault() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler(any())).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(InternalEvent.class))).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(any(InternalEvent.class))).thenReturn(false);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(mockTestExceptionStrategy2, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(1)).handleException(eq(mockException), any(InternalEvent.class));
  }

  @Test
  public void secondMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(InternalEvent.class))).thenReturn(false);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
    verify(mockTestExceptionStrategy2, times(1)).handleException(eq(mockException), any(InternalEvent.class));
  }

  @Test(expected = MuleRuntimeException.class)
  public void firstAcceptsAllMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
    when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
  }

  @Test
  public void criticalIsNotHandled() throws Exception {
    when(mockErrorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
  }

  @Test
  public void defaultErrorHandler() throws InitialisationException {
    final ErrorHandler defaultHandler = new ErrorHandlerFactory().createDefault(mock(NotificationDispatcher.class));

    assertThat(defaultHandler.getExceptionListeners(), hasSize(1));
    assertThat(defaultHandler.getExceptionListeners(), hasItem(instanceOf(OnErrorPropagateHandler.class)));
  }

  class DefaultMessagingExceptionHandlerAcceptor implements MessagingExceptionHandlerAcceptor {

    @Override
    public boolean accept(InternalEvent event) {
      return true;
    }

    @Override
    public boolean acceptsAll() {
      return true;
    }

    @Override
    public InternalEvent handleException(MessagingException exception, InternalEvent event) {
      exception.setHandled(true);
      return event;
    }
  }

}
