/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ErrorHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy1;
  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy2;
  @Mock
  private MessagingExceptionHandlerAcceptor mockDefaultTestExceptionStrategy2;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;
  private MessagingException mockException;

  @Before
  public void before() {
    when(mockMuleEvent.getMessage()).thenReturn(MuleMessage.builder().payload("").build());
    when(mockMuleEvent.getMuleContext()).thenReturn(mockMuleContext);
    mockException = new MessagingException(mockMuleEvent, new Exception());
  }

  @Test
  public void testNonMatchThenCallDefault() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(mockMuleEvent)).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(false);
    when(mockDefaultTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(MuleEvent.class));
    verify(mockTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(MuleEvent.class));
    verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(mockException, mockMuleEvent);
  }

  @Test
  public void testSecondMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(mockMuleEvent)).thenReturn(false);
    when(mockTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
    when(mockDefaultTestExceptionStrategy2.accept(mockMuleEvent)).thenReturn(true);
    errorHandler.handleException(mockException, mockMuleEvent);
    verify(mockTestExceptionStrategy1, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                         any(MuleEvent.class));
    verify(mockDefaultTestExceptionStrategy2, VerificationModeFactory.times(0)).handleException(any(MessagingException.class),
                                                                                                any(MuleEvent.class));
    verify(mockTestExceptionStrategy2, VerificationModeFactory.times(1)).handleException(mockException, mockMuleEvent);
  }

  @Test(expected = MuleRuntimeException.class)
  public void testFirstAcceptsAllMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setExceptionListeners(new ArrayList<>(Arrays
        .<MessagingExceptionHandlerAcceptor>asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultExceptionStrategy()).thenReturn(mockDefaultTestExceptionStrategy2);
    when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
    when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
    when(mockDefaultTestExceptionStrategy2.acceptsAll()).thenReturn(true);
    errorHandler.initialise();
  }

}
