/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.Errors;
import org.mule.runtime.core.exception.MessagingException;
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
  private org.mule.runtime.core.exception.ErrorTypeRepository mockErrorTypeRepository;
  @Mock(answer = RETURNS_DEEP_STUBS)
  private Error mockError;
  @Mock
  private ErrorType mockErrorType;
  @Mock
  private FlowConstruct mockFlowConstruct;

  private ExceptionToMessagingExceptionExecutionInterceptor cut;



  @Before
  public void before() {
    when(mockMessagingException.getFailingMessageProcessor()).thenCallRealMethod();
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
    when(mockFlowConstruct.getMuleContext()).thenReturn(mockMuleContext);

    cut = new ExceptionToMessagingExceptionExecutionInterceptor();
    cut.setFlowConstruct(mockFlowConstruct);
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
    IllegalStateException causeOfMoreSpecificException = new IllegalStateException();
    ErrorType mockUnknowErrorType = mock(ErrorType.class);
    when(mockUnknowErrorType.getIdentifier()).thenReturn(Errors.ComponentIdentifiers.UNKNOWN.getName());
    when(mockUnknowErrorType.getNamespace()).thenReturn(Errors.ComponentIdentifiers.UNKNOWN.getNamespace());
    when(mockErrorTypeLocator.lookupErrorType(causeOfMoreSpecificException)).thenReturn(mockUnknowErrorType);

    ErrorType moreSpecificErrorType = mock(ErrorType.class);
    Exception moreSpecificException = mock(Exception.class);
    when(moreSpecificException.getCause()).thenReturn(causeOfMoreSpecificException);
    when(mockErrorTypeLocator.lookupErrorType(moreSpecificException)).thenReturn(moreSpecificErrorType);

    ErrorType mockErrorType = mock(ErrorType.class);
    Exception mockException = mock(Exception.class);
    when(mockException.getCause()).thenReturn(moreSpecificException);
    when(mockErrorTypeLocator.lookupErrorType(mockException)).thenReturn(mockErrorType);

    when(wrapperException.getCause()).thenReturn(mockException);

    when(mockMessageProcessor.process(mockMuleEvent)).thenThrow(wrapperException);
    try {
      cut.execute(mockMessageProcessor, mockMuleEvent);
      fail("Exception should be thrown");
    } catch (MessagingException e) {
      assertThat(e.getEvent().getError().get().getErrorType(), is(moreSpecificErrorType));
    }
  }
}
