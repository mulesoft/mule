/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message.processing;

import static java.util.Optional.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.message.DefaultEventBuilder.MuleEventImplementation.setCurrentEvent;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhase;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessPhase;
import org.mule.runtime.core.execution.PhaseResultNotifier;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.runtime.core.execution.ResponseDispatchException;
import org.mule.runtime.core.execution.ValidationPhase;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class AsyncResponseFlowProcessingPhaseTestCase extends AbstractMuleTestCase {

  private static final int LATCH_TIMEOUT = 50;
  private AsyncResponseFlowProcessingPhase phase = new AsyncResponseFlowProcessingPhase() {

    // We cannot mock this method since its protected
    @Override
    protected NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager) {
      return notificationHelper;
    };
  };

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private AsyncResponseFlowProcessingPhaseTemplate mockTemplate;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessContext mockContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PhaseResultNotifier mockNotifier;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ResponseDispatchException mockResponseDispatchException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingException mockMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Event mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingException mockException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private NotificationHelper notificationHelper;

  @Before
  public void before() {
    phase.setMuleContext(mock(MuleContext.class));
    when(mockMuleEvent.getError()).thenReturn(empty());
  }

  @Before
  public void configureExpectedBehaviour() throws Exception {
    when(mockTemplate.getMuleEvent()).thenReturn(mockMuleEvent);
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentSuccessfully();
      return null;
    }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentSuccessfully();
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    when(mockTemplate.getMuleEvent()).thenReturn(mockMuleEvent);
  }


  @Test
  public void supportedTemplates() {
    new PhaseSupportTestHelper<>(AsyncResponseFlowProcessingPhaseTemplate.class).testSupportTemplates(phase);
  }

  @Test
  public void order() {
    assertThat(phase.compareTo(new ValidationPhase()), is(1));
    assertThat(phase.compareTo(Mockito.mock(MessageProcessPhase.class)), is(0));
  }

  @Test
  public void runPhaseWithMessagingExceptionThrown() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    doThrow(mockMessagingException).when(mockTemplate).routeEvent(any(Event.class));
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(MessagingException.class),
                                                                                  any(Event.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void runPhaseWithSuccessfulFlowProcessing() throws Exception {
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void runPhaseWithSuccessfulFlowProcessingNonBlocking() throws Exception {
    final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockTemplate.routeEvent(any(Event.class)))
        .thenAnswer(invocation -> sensingMessageProcessor.process((Event) invocation.getArguments()[0]));

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
    verify(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void sendResponseWhenFlowExecutionFailsAndExceptionIsHandled() throws MuleException {
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(true);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void sendResponseWhenFlowExecutionFailsAndExceptionIsHandledNonBlocking() throws Exception {
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(true);

    final Latch latch = new Latch();
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    verify(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void sendFailureResponseWhenFlowExecutionFailsAndExceptionIsNotHandled() throws MuleException {
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(false);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void sendFailureResponseWhenFlowExecutionFailsAndExceptionIsNotHandledNonBlocking() throws Exception {
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(false);
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    verify(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void callExceptionHandlerWhenSuccessfulExecutionFailsWritingResponse() throws Exception {
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(MessagingException.class),
                                                                                  any(Event.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void callExceptionHandlerWhenSuccessfulExecutionFailsWritingResponseNonBlocking() throws Exception {
    final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockTemplate.routeEvent(any(Event.class)))
        .thenAnswer(invocation -> sensingMessageProcessor.process((Event) invocation.getArguments()[0]));

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
    verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(MessagingException.class),
                                                                                  any(Event.class));
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void doNotCallExceptionHandlerWhenFailureExecutionFailsWritingResponse() throws Exception {
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, ((MessagingException) invocationOnMock.getArguments()[0]).getEvent());
      return null;
    }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(MessagingException.class),
                                                                                  any(Event.class));
    verifyOnlyFailureWasCalled(mockException);
  }

  @Test
  public void doNotCallExceptionHandlerWhenFailureExecutionFailsWritingResponseNonBlocking() throws Exception {
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, ((MessagingException) invocationOnMock.getArguments()[0]).getEvent());
      return null;
    }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(MessagingException.class),
                                                                                  any(Event.class));
    verifyOnlyFailureWasCalled(mockException);
  }

  @Test
  public void allowNullEventsOnNotifications() throws Exception {
    setCurrentEvent(null);
    when(mockTemplate.getMuleEvent()).thenReturn(null);
    when(mockTemplate.routeEvent(any(Event.class))).thenReturn(null);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    setCurrentEvent(mockMuleEvent);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
  }


  @Test
  public void responseNotificationFired() throws Exception {
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  @Test
  public void responseNotificationFiredNonBlocking() throws Exception {
    final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockTemplate.routeEvent(any(Event.class)))
        .thenAnswer(invocation -> sensingMessageProcessor.process((Event) invocation.getArguments()[0]));

    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  @Test
  public void errorResponseNotificationFired() throws Exception {
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  @Test
  public void errorResponseNotificationFiredNonBlocking() throws Exception {
    final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
    doAnswer(invocationOnMock -> {
      ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
      callback.responseSentWithFailure(mockException, mockMuleEvent);
      return null;
    }).when(mockTemplate).sendResponseToClient(any(Event.class), any(ResponseCompletionCallback.class));
    when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockTemplate.routeEvent(any(Event.class))).thenThrow(mockMessagingException);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);

    sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class), isNull(String.class),
                                                any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  private void verifyOnlySuccessfulWasCalled() {
    verify(mockNotifier, never()).phaseFailure(any(Exception.class));
    verify(mockNotifier, never()).phaseConsumedMessage();
    verify(mockNotifier).phaseSuccessfully();
  }

  private void verifyOnlyFailureWasCalled(Exception e) {
    verify(mockNotifier).phaseFailure(e);
    verify(mockNotifier, never()).phaseConsumedMessage();
    verify(mockNotifier, never()).phaseSuccessfully();
  }

}
