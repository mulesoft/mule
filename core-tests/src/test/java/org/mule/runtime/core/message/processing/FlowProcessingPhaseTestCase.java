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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.FlowProcessingPhase;
import org.mule.runtime.core.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessPhase;
import org.mule.runtime.core.execution.PhaseResultNotifier;
import org.mule.runtime.core.execution.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseDispatchException;
import org.mule.runtime.core.execution.ValidationPhase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FlowProcessingPhaseTestCase extends AbstractMuleTestCase {

  private FlowProcessingPhase phase = new FlowProcessingPhase() {

    // We cannot mock this method since its protected
    @Override
    protected NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager) {
      return notificationHelper;
    };
  };

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FlowProcessingPhaseTemplate mockTemplate;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RequestResponseFlowProcessingPhaseTemplate mockRequestResponseTemplate;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessContext mockContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PhaseResultNotifier mockNotifier;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ResponseDispatchException mockResponseDispatchException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingException mockMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleException mockException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private NotificationHelper notificationHelper;

  @Before
  public void before() {
    phase.setMuleContext(mock(MuleContext.class));
    when(mockContext.getTransactionConfig()).thenReturn(empty());
  }

  @Test
  public void supportedTemplates() {
    new PhaseSupportTestHelper<FlowProcessingPhaseTemplate>(FlowProcessingPhaseTemplate.class).testSupportTemplates(phase);
  }

  @Test
  public void order() {
    assertThat(phase.compareTo(new ValidationPhase()), is(1));
    assertThat(phase.compareTo(Mockito.mock(MessageProcessPhase.class)), is(0));
  }

  @Test
  public void runPhaseWithExceptionThrown() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    doThrow(mockException).when(mockTemplate).afterSuccessfulProcessingFlow(any(Event.class));
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockContext.getFlowConstruct()).getExceptionListener();
    verifyOnlyFailureWasCalled(mockException);
  }

  @Test
  public void runPhaseWithMessagingExceptionThrown() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    when(mockTemplate.routeEvent(Mockito.any(Event.class))).thenThrow(mockMessagingException);
    phase.runPhase(mockTemplate, mockContext, mockNotifier);
    verify(mockContext.getFlowConstruct()).getExceptionListener();
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void callSendResponseForRequestResponseTemplate() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verifyOnlySuccessfulWasCalled();
    verify(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
  }

  @Test
  public void successfulPhaseExecutionInOrder() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    InOrder inOrderVerify =
        Mockito.inOrder(mockContext, mockContext.getFlowConstruct(), mockRequestResponseTemplate, mockNotifier);
    inOrderVerify.verify(mockContext, atLeastOnce()).getTransactionConfig();
    inOrderVerify.verify(mockContext.getFlowConstruct()).getExceptionListener();
    inOrderVerify.verify(mockRequestResponseTemplate).getEvent();
    inOrderVerify.verify(mockRequestResponseTemplate).beforeRouteEvent(any(Event.class));
    inOrderVerify.verify(mockRequestResponseTemplate).routeEvent(any(Event.class));
    inOrderVerify.verify(mockRequestResponseTemplate).afterRouteEvent(any(Event.class));
    inOrderVerify.verify(mockRequestResponseTemplate).afterSuccessfulProcessingFlow(any(Event.class));
    inOrderVerify.verify(mockNotifier).phaseSuccessfully();
    verifyOnlySuccessfulWasCalled();
  }

  @Test
  public void sendResponseWhenFlowExecutionFailsAndExceptionIsHandled() throws MuleException {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    when(mockRequestResponseTemplate.afterRouteEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(true);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
  }

  @Test
  public void sendFailureResponseWhenFlowExecutionFailsAndExceptionIsNotHandled() throws MuleException {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    when(mockRequestResponseTemplate.afterRouteEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(false);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(mockRequestResponseTemplate).afterFailureProcessingFlow(mockMessagingException);
  }

  @Test
  public void doNotSendResponseWhenFlowExecutionFailsSendingResponseAndExceptionIsHandled() throws MuleException {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    doThrow(mockResponseDispatchException).when(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
    when(mockMessagingException.handled()).thenReturn(true);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
    verify(mockRequestResponseTemplate, never()).afterFailureProcessingFlow(mockMessagingException);
  }

  @Test
  public void doNotSendFailureResponseWhenFlowExecutionFailsSendingResponseAndExceptionIsNotHandled() throws MuleException {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    doThrow(mockMessagingException).when(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
    when(mockMessagingException.handled()).thenReturn(false);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(mockRequestResponseTemplate).sendResponseToClient(any(Event.class));
    verify(mockRequestResponseTemplate).afterFailureProcessingFlow(mockMessagingException);
  }

  @Test
  public void responseNotificationFired() throws MuleException {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    when(mockRequestResponseTemplate.afterRouteEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(true);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class),
                                                any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  @Test
  public void errorResponseNotificationFired() throws Exception {
    when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
    when(mockRequestResponseTemplate.afterRouteEvent(any(Event.class))).thenThrow(mockMessagingException);
    when(mockMessagingException.handled()).thenReturn(false);
    phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
    verify(notificationHelper, never()).fireNotification(any(MessageSource.class), any(Event.class),
                                                         any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
    verify(notificationHelper).fireNotification(any(MessageSource.class), any(Event.class),
                                                any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
  }

  private void verifyOnlySuccessfulWasCalled() {
    verify(mockNotifier, Mockito.never()).phaseFailure(any(Exception.class));
    verify(mockNotifier, Mockito.never()).phaseConsumedMessage();
    verify(mockNotifier).phaseSuccessfully();
  }

  private void verifyOnlyFailureWasCalled(Exception e) {
    verify(mockNotifier).phaseFailure(e);
    verify(mockNotifier, Mockito.never()).phaseConsumedMessage();
    verify(mockNotifier, Mockito.never()).phaseSuccessfully();
  }

}
