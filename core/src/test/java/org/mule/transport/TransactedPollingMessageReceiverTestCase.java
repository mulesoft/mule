/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransactedPollingMessageReceiverTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AbstractConnector mockConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockEndpoint;


    @Test
    public void callSystemExceptionHandlerReceivingMessage() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        TransactedPollingMessageReceiver messageReceiver = new TestTransactedPollingMessageReceiver(mockConnector, mockFlowConstruct, mockEndpoint);
        messageReceiver.poll();
        verify(mockConnector.getMuleContext(), timeout(1)).handleException(any(Exception.class));
    }

    @Test
    public void callSystemExceptionHandlerReceivingMessageInTransaction() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        TransactedPollingMessageReceiver messageReceiver = new TestTransactedPollingMessageReceiver(mockConnector, mockFlowConstruct, mockEndpoint);
        messageReceiver.setReceiveMessagesInTransaction(true);
        messageReceiver.poll();
        verify(mockConnector.getMuleContext(), timeout(1)).handleException(any(Exception.class));
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
        protected void processMessage(Object message) throws Exception
        {
        }
    }
}
