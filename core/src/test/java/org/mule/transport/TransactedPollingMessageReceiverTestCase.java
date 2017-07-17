/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class TransactedPollingMessageReceiverTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AbstractConnector mockConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockEndpoint;
    @Mock
    private static ExecutionTemplate<MuleEvent> executionTemplate; 

    @Test
    public void callSystemExceptionHandlerReceivingMessage() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        TransactedPollingMessageReceiver messageReceiver = new TestTransactedPollingMessageReceiver(mockConnector, mockFlowConstruct, mockEndpoint);
        messageReceiver.poll();
        verify(mockEndpoint.getMuleContext(), timeout(1)).handleException(any(Exception.class));
    }

    @Test
    public void callSystemExceptionHandlerReceivingMessageInTransaction() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        TransactedPollingMessageReceiver messageReceiver = new TestTransactedPollingMessageReceiver(mockConnector, mockFlowConstruct, mockEndpoint);
        messageReceiver.setReceiveMessagesInTransaction(true);
        messageReceiver.poll();
        verify(mockEndpoint.getMuleContext(), timeout(1)).handleException(any(Exception.class));
    }

    @Test
    public void processCorrectlyWhenMessageResultsAreNull() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Exception {
                Object[] args = invocation.getArguments();
                ExecutionCallback<MuleEvent> executionCallback= (ExecutionCallback<MuleEvent>) args[0];
                executionCallback.process();
                return null;
            }
        }).when(executionTemplate).execute(any(ExecutionCallback.class));
        TestTransactedPollingProcessNullMessageReceiver messageReceiver = new TestTransactedPollingProcessNullMessageReceiver(mockConnector, mockFlowConstruct, mockEndpoint);
        messageReceiver.setReceiveMessagesInTransaction(true);
        messageReceiver.poll();
        verify(mockEndpoint.getMuleContext(), never()).handleException(any(Exception.class));
    }

    public static class TestTransactedPollingMessageReceiver extends TransactedPollingMessageReceiver
    {

        public TestTransactedPollingMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flowConstruct, endpoint);
        }

        @Override
        protected List<MuleMessage> getMessages() throws Exception
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("test exception"));
        }

        @Override
        protected MuleEvent processMessage(Object message) throws Exception
        {
            return null;
        }
    }
    
    public static class TestTransactedPollingProcessNullMessageReceiver extends TransactedPollingMessageReceiver
    {

        public TestTransactedPollingProcessNullMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flowConstruct, endpoint);
        }

        @Override
        protected List<MuleMessage> getMessages() throws Exception
        {
            List<MuleMessage> messages = new ArrayList<MuleMessage>();
            messages.add(null);
            return messages;
        }

        @Override
        protected MuleEvent processMessage(Object message) throws Exception
        {
            return null;
        }
    }    
}
