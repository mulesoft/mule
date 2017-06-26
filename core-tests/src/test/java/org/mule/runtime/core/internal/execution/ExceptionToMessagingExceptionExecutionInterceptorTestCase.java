/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.ErrorTypeLocator;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionToMessagingExceptionExecutionInterceptorTestCase extends AbstractMuleTestCase {

  @Mock
  private Processor mockMessageProcessor;
  @Mock
  private MuleContext mockMuleContext;
  @Mock
  private Event mockMuleEvent;
  @Mock
  private Event mockResultMuleEvent;
  @Mock
  private EventContext mockEventContext;
  @Mock
  private MessagingException mockMessagingException;
  @Mock
  private MuleException mockMuleException;
  @Mock
  private ErrorTypeLocator mockErrorTypeLocator;
  @Mock
  private ErrorTypeRepository mockErrorTypeRepository;
  @Mock(answer = RETURNS_DEEP_STUBS)
  private Error mockError;
  @Mock
  private ErrorType mockErrorType;

  private ExceptionToMessagingExceptionExecutionInterceptor cut;


  @Before
  public void before() {
    when(mockMessagingException.getFailingMessageProcessor()).thenReturn(mockMessageProcessor);
    when(mockMessagingException.getEvent()).thenReturn(mockMuleEvent);
    when(mockMuleContext.getErrorTypeLocator()).thenReturn(mockErrorTypeLocator);
    when(mockMuleContext.getErrorTypeRepository()).thenReturn(mockErrorTypeRepository);
    when(mockMuleEvent.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(mockMuleEvent.getError()).thenReturn(of(mockError));
    when(mockMuleEvent.getMessage()).thenReturn(Message.of(null));
    when(mockMuleEvent.getContext()).thenReturn(mockEventContext);
    when(mockErrorTypeLocator.lookupErrorType(any(Throwable.class))).thenReturn(mockErrorType);
    when(mockErrorTypeRepository.getErrorType(any()))
        .thenReturn(of(mock(ErrorType.class)));

    cut = new ExceptionToMessagingExceptionExecutionInterceptor();
    cut.setMuleContext(mockMuleContext);
  }

  @Test
  public void executionSuccessfully() throws MuleException {
    when(mockMessageProcessor.process(mockMuleEvent)).thenReturn(mockResultMuleEvent);
    Event result = cut.execute(mockMessageProcessor, mockMuleEvent);
    assertThat(result, is(mockResultMuleEvent));
  }

  @Test
  public void messageExceptionThrown() throws MuleException {
    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMessagingException);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
    }
  }

  @Test
  public void checkedExceptionThrown() throws MuleException {
    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(mockMuleException);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(mockMuleException));
    }
  }

  @Test
  public void runtimeExceptionThrown() throws MuleException {
    RuntimeException runtimeException = new RuntimeException();
    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(runtimeException);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(runtimeException));
    }
  }

  @Test
  public void errorThrown() throws MuleException {
    java.lang.Error error = new java.lang.Error();
    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(error);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat((java.lang.Error) e.getCause(), is(error));
    }
  }

  @Test
  public void messagingExceptionWithErrorWrappedByAnotherMessagingException() throws Exception {
    testExceptionWrappedByAnotherException(mockMessagingException);
  }

  @Test
  public void messagingExceptionWithErrorWrappedByNonMessagingException() throws Exception {
    testExceptionWrappedByAnotherException(mockMuleException);
  }

  private void testExceptionWrappedByAnotherException(MuleException wrapperException) throws MuleException {
    ErrorType moreSpecificErrorType = mock(ErrorType.class);
    Error moreSpecificError = mock(Error.class);
    when(moreSpecificError.getErrorType()).thenReturn(moreSpecificErrorType);
    IllegalStateException causeOfMoreSpecificException = new IllegalStateException();
    when(moreSpecificError.getCause()).thenReturn(causeOfMoreSpecificException);
    MessagingException moreSpecificException = mock(MessagingException.class, RETURNS_DEEP_STUBS.get());
    when(moreSpecificException.getEvent().getError()).thenReturn(of(moreSpecificError));
    when(moreSpecificException.getCause()).thenReturn(causeOfMoreSpecificException);

    when(wrapperException.getCause()).thenReturn(moreSpecificException);
    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(wrapperException);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat(e.getEvent().getError().get(), is(moreSpecificError));
    }
  }
}
