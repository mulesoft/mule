/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.transport.TransactedPollingMessageReceiver;

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
}
