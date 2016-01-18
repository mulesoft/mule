/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.processing;

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
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_RESPONSE;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.context.notification.NotificationHelper;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.execution.AsyncResponseFlowProcessingPhase;
import org.mule.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessPhase;
import org.mule.execution.PhaseResultNotifier;
import org.mule.execution.ResponseCompletionCallback;
import org.mule.execution.ResponseDispatchException;
import org.mule.execution.ValidationPhase;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;


@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class AsyncResponseFlowProcessingPhaseTestCase extends AbstractMuleTestCase
{

    private static final int LATCH_TIMEOUT = 50;
    private AsyncResponseFlowProcessingPhase phase = new AsyncResponseFlowProcessingPhase()
    {
        // We cannot mock this method since its protected
        @Override
        protected NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager)
        {
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
    private MuleException mockException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DefaultMuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private NotificationHelper notificationHelper;

    @Before
    public void before()
    {
        phase.setMuleContext(mock(MuleContext.class));
    }

    @Before
    public void configureExpectedBehaviour() throws Exception
    {
        when(mockTemplate.getMuleEvent()).thenReturn(mockMuleEvent);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentSuccessfully();
                return null;
            }
        }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentSuccessfully();
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        when(mockTemplate.getMuleEvent()).thenReturn(mockMuleEvent);
    }


    @Test
    public void supportedTemplates()
    {
        new PhaseSupportTestHelper<>(AsyncResponseFlowProcessingPhaseTemplate.class).testSupportTemplates(phase);
    }

    @Test
    public void order()
    {
        assertThat(phase.compareTo(new ValidationPhase()), is(1));
        assertThat(phase.compareTo(Mockito.mock(MessageProcessPhase.class)), is(0));
    }

    @Test
    public void runPhaseWithMessagingExceptionThrown() throws Exception
    {
        when(mockContext.supportsAsynchronousProcessing()).thenReturn(false);
        doThrow(mockMessagingException).when(mockTemplate).routeEvent(any(MuleEvent.class));
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(Exception.class), any(MuleEvent.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void runPhaseWithSuccessfulFlowProcessing() throws Exception
    {
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void runPhaseWithSuccessfulFlowProcessingNonBlocking() throws Exception
    {
        final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                return sensingMessageProcessor.process((MuleEvent) invocation.getArguments()[0]);
            }
        });

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        verify(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void sendResponseWhenFlowExecutionFailsAndExceptionIsHandled() throws MuleException
    {
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        when(mockMessagingException.handled()).thenReturn(true);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void sendResponseWhenFlowExecutionFailsAndExceptionIsHandledNonBlocking() throws Exception
    {
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        when(mockMessagingException.handled()).thenReturn(true);

        final Latch latch = new Latch();
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        verify(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void sendFailureResponseWhenFlowExecutionFailsAndExceptionIsNotHandled() throws MuleException
    {
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        when(mockMessagingException.handled()).thenReturn(false);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void sendFailureResponseWhenFlowExecutionFailsAndExceptionIsNotHandledNonBlocking() throws Exception
    {
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        when(mockMessagingException.handled()).thenReturn(false);
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        verify(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void callExceptionHandlerWhenSuccessfulExecutionFailsWritingResponse() throws Exception
    {
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(Exception.class), any(MuleEvent.class));
        verify(mockMuleEvent).resetAccessControl();
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void callExceptionHandlerWhenSuccessfulExecutionFailsWritingResponseNonBlocking() throws Exception
    {
        final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                return sensingMessageProcessor.process((MuleEvent) invocation.getArguments()[0]);
            }
        });

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(Exception.class), any(MuleEvent.class));
        verify(mockMuleEvent).resetAccessControl();
        verifyOnlySuccessfulWasCalled();
    }

    @Test
    public void doNotCallExceptionHandlerWhenFailureExecutionFailsWritingResponse() throws Exception
    {
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, ((MessagingException)invocationOnMock.getArguments()[0]).getEvent());
                return null;
            }
        }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any(ResponseCompletionCallback.class));
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(Exception.class), any(MuleEvent.class));
        verifyOnlyFailureWasCalled(mockException);
    }

    @Test
    public void doNotCallExceptionHandlerWhenFailureExecutionFailsWritingResponseNonBlocking() throws Exception
    {
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, ((MessagingException)invocationOnMock.getArguments()[0]).getEvent());
                return null;
            }
        }).when(mockTemplate).sendFailureResponseToClient(any(MessagingException.class), any
                (ResponseCompletionCallback.class));
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        verify(mockContext.getFlowConstruct().getExceptionListener()).handleException(any(Exception.class), any(MuleEvent.class));
        verifyOnlyFailureWasCalled(mockException);
    }

    @Test
    public void allowNullEventsOnNotifications() throws Exception
    {
        RequestContext.setEvent(null);
        when(mockTemplate.getMuleEvent()).thenReturn(null);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenReturn(null);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        when(mockMuleEvent.newThreadCopy()).thenReturn(mockMuleEvent);
        RequestContext.setEvent(mockMuleEvent);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
    }


    @Test
    public void responseNotificationFired() throws Exception
    {
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(notificationHelper).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
        verify(notificationHelper, never()).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
    }

    @Test
    public void responseNotificationFiredNonBlocking() throws Exception
    {
        final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                return sensingMessageProcessor.process((MuleEvent) invocation.getArguments()[0]);
            }
        });

        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        verify(notificationHelper).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
        verify(notificationHelper, never()).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
    }

    @Test
    public void errorResponseNotificationFired() throws Exception
    {
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);
        verify(notificationHelper, never()).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
        verify(notificationHelper).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
    }

    @Test
    public void errorResponseNotificationFiredNonBlocking() throws Exception
    {
        final SensingNullMessageProcessor sensingMessageProcessor = new SensingNullMessageProcessor();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ResponseCompletionCallback callback = (ResponseCompletionCallback) invocationOnMock.getArguments()[1];
                callback.responseSentWithFailure(mockException, mockMuleEvent);
                return null;
            }
        }).when(mockTemplate).sendResponseToClient(any(MuleEvent.class), any(ResponseCompletionCallback.class));
        when(mockMuleEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockTemplate.routeEvent(any(MuleEvent.class))).thenThrow(mockMessagingException);
        phase.runPhase(mockTemplate, mockContext, mockNotifier);

        sensingMessageProcessor.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        verify(notificationHelper, never()).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_RESPONSE));
        verify(notificationHelper).fireNotification(any(MuleEvent.class), isNull(String.class), any(FlowConstruct.class), eq(MESSAGE_ERROR_RESPONSE));
    }

    private void verifyOnlySuccessfulWasCalled()
    {
        verify(mockNotifier, Mockito.never()).phaseFailure(any(Exception.class));
        verify(mockNotifier, Mockito.never()).phaseConsumedMessage();
        verify(mockNotifier).phaseSuccessfully();
    }

    private void verifyOnlyFailureWasCalled(Exception e)
    {
        verify(mockNotifier).phaseFailure(e);
        verify(mockNotifier, Mockito.never()).phaseConsumedMessage();
        verify(mockNotifier, Mockito.never()).phaseSuccessfully();
    }

}
