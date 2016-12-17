/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.exception.ErrorTypeRepository.CRITICAL_ERROR_TYPE;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@SmallTest
@Features("Error Handling")
@Stories("Error Handler")
@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy1;
  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy2;
  @Mock
  private MessagingExceptionHandlerAcceptor mockDefaultTestExceptionStrategy2;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Event mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;
  @Mock
  private ErrorType mockErrorType;
  private MessagingException mockException;

  @Before
  public void before() {
    when(mockMuleEvent.getMessage()).thenReturn(InternalMessage.builder().payload("").build());
    when(mockMuleEvent.getMuleContext()).thenReturn(mockMuleContext);
    Error mockError = mock(Error.class);
    when(mockError.getErrorType()).thenReturn(mockErrorType);
    when(mockMuleEvent.getError()).thenReturn(of(mockError));
    mockException = new MessagingException(mockMuleEvent, new Exception());
  }

  @Test
  public void nonMatchThenCallDefault() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(mockDefaultTestExceptionStrategy2);
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(any(Event.class))).thenReturn(false);
    when(mockDefaultTestExceptionStrategy2.accept(any(Event.class))).thenReturn(true);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(Event.class));
    verify(mockTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(Event.class));
    verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(eq(mockException),
                                                                                                any(Event.class));
  }

  @Test
  public void secondMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(mockDefaultTestExceptionStrategy2);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(any(Event.class))).thenReturn(true);
    when(mockDefaultTestExceptionStrategy2.accept(any(Event.class))).thenReturn(true);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(Event.class));
    verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                                any(Event.class));
    verify(mockTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(eq(mockException), any(Event.class));
  }

  @Test(expected = MuleRuntimeException.class)
  public void firstAcceptsAllMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(mockDefaultTestExceptionStrategy2);
    when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
    when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
    when(mockDefaultTestExceptionStrategy2.acceptsAll()).thenReturn(true);
    errorHandler.initialise();
  }

  @Test
  public void criticalIsNotHandled() throws Exception {
    when(mockErrorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler()).thenReturn(mockDefaultTestExceptionStrategy2);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(Event.class))).thenReturn(true);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(Event.class));
    verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                                any(Event.class));
  }

}
