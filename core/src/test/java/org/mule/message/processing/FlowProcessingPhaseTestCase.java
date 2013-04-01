/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.execution.FlowProcessingPhase;
import org.mule.execution.FlowProcessingPhaseTemplate;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessPhase;
import org.mule.execution.PhaseResultNotifier;
import org.mule.execution.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.execution.ValidationPhase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FlowProcessingPhaseTestCase extends AbstractMuleTestCase
{

    private FlowProcessingPhase phase = new FlowProcessingPhase();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowProcessingPhaseTemplate mockTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RequestResponseFlowProcessingPhaseTemplate mockRequestResponseTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessContext mockContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PhaseResultNotifier mockNotifier;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessagingException mockMessagingException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleException mockException;


    @Test
    public void supportedTemplates()
    {
        new PhaseSupportTestHelper<FlowProcessingPhaseTemplate>(FlowProcessingPhaseTemplate.class).testSupportTemplates(phase);
    }

    @Test
    public void order()
    {
        assertThat(phase.compareTo(new ValidationPhase()), is(1));
        assertThat(phase.compareTo(Mockito.mock(MessageProcessPhase.class)), is(0));
    }

    @Test
    public void runPhaseWithExceptionThrown() throws Exception
    {
        when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
        doThrow(mockException).when(mockTemplate).afterSuccessfulProcessingFlow(any(MuleEvent.class));
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockContext.getFlowConstruct(), Mockito.times(1)).getExceptionListener();
        verifyOnlyFailureWasCalled(mockException);
    }

    @Test
    public void runPhaseWithMessagingExceptionThrown() throws Exception
    {
        when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
        when(mockTemplate.routeEvent(Mockito.any(MuleEvent.class))).thenThrow(mockMessagingException);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockContext.getFlowConstruct(), Mockito.times(1)).getExceptionListener();
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void callSendResponseForRequestResponseTemplate() throws Exception
    {
        when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
        phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
        verifyOnlySuccessfulWasCalled();
        verify(mockRequestResponseTemplate, times(1)).sendResponseToClient(any(MuleEvent.class));
    }

    @Test
    public void successfulPhaseExecutionInOrder() throws Exception
    {
        when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
        phase.runPhase(mockRequestResponseTemplate, mockContext, mockNotifier);
        InOrder inOrderVerify = Mockito.inOrder(mockContext, mockContext.getFlowConstruct(), mockRequestResponseTemplate, mockNotifier);
        inOrderVerify.verify(mockContext, atLeastOnce()).getTransactionConfig();
        inOrderVerify.verify(mockContext.getFlowConstruct(), times(1)).getExceptionListener();
        inOrderVerify.verify(mockRequestResponseTemplate, times(1)).getMuleEvent();
        inOrderVerify.verify(mockRequestResponseTemplate, times(1)).beforeRouteEvent(any(MuleEvent.class));
        inOrderVerify.verify(mockRequestResponseTemplate, times(1)).routeEvent(any(MuleEvent.class));
        inOrderVerify.verify(mockRequestResponseTemplate, times(1)).afterRouteEvent(any(MuleEvent.class));
        inOrderVerify.verify(mockRequestResponseTemplate, times(1)).afterSuccessfulProcessingFlow(any(MuleEvent.class));
        inOrderVerify.verify(mockNotifier, times(1)).phaseSuccessfully();
        verifyOnlySuccessfulWasCalled();
    }

    private void verifyOnlySuccessfulWasCalled()
    {
        verify(mockNotifier, Mockito.times(0)).phaseFailure(any(Exception.class));
        verify(mockNotifier, Mockito.times(0)).phaseConsumedMessage();
        verify(mockNotifier, Mockito.times(1)).phaseSuccessfully();
    }

    private void verifyOnlyFailureWasCalled(Exception e)
    {
        verify(mockNotifier, Mockito.times(1)).phaseFailure(e);
        verify(mockNotifier, Mockito.times(0)).phaseConsumedMessage();
        verify(mockNotifier, Mockito.times(0)).phaseSuccessfully();
    }

}
