/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.UndeclaredThrowableException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class XaTransactedJmsMessageReceiverTest extends AbstractMuleTestCase {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JmsConnector mockJmsConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockInboundEndpoint;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageConsumer messageConsumer;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Transaction transaction;

    @After
    public void clearInterruptedFlag()
    {
        Thread.interrupted();
    }

    @Test
    public void testTopicReceiverShouldBeStartedOnlyInPrimaryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(true);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        XaTransactedJmsMessageReceiver messageReceiver = new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(false));
    }

    @Test
    public void testQueueReceiverShouldBeStartedInEveryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(false);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        XaTransactedJmsMessageReceiver messageReceiver = new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(true));
    }

    private void doDisconnectExceptionTest(final Exception exceptionToThrow) throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(false);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);

        XaTransactedJmsMessageReceiver messageReceiver = spy(new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint));
        doReturn(messageConsumer).when(messageReceiver).createConsumer();

        when(messageConsumer.receive(messageReceiver.timeout)).thenAnswer(new Answer<Message>()
        {
            @Override
            public Message answer(InvocationOnMock invocation) throws Exception
            {
                Thread.currentThread().interrupt();
                throw exceptionToThrow;
            }
        });

        doAnswer(new Answer()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                assertThat(Thread.currentThread().isInterrupted(), is(true));
                return null;
            }
        }).when(transaction).setRollbackOnly();
        TransactionCoordination.getInstance().bindTransaction(transaction);
        messageReceiver.getMessages();

        verify(transaction).setRollbackOnly();
    }


    @Test
    public void jmsExceptionWhileDisconnecting() throws Exception
    {
        doDisconnectExceptionTest(new JMSException("Test exception"));
    }

    @Test
    public void undeclaredThrowableExceptionWhileDisconnecting() throws Exception
    {
        doDisconnectExceptionTest(new UndeclaredThrowableException(new RuntimeException(new JMSException("Test exception"))));
    }

    @Test(expected = RuntimeException.class)
    public void otherExceptionWhileDisconnecting() throws Exception
    {
        doDisconnectExceptionTest(new RuntimeException("Test exception"));
    }

}
