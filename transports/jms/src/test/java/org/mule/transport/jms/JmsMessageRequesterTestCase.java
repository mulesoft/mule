/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.mule.api.transport.MessageDispatcher.RECEIVE_NO_WAIT;
import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.context.notification.TransactionNotification;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transaction.TransactionCoordination;

import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class JmsMessageRequesterTestCase extends AbstractMuleTestCase
{

    private final InboundEndpoint endpoint = mock(InboundEndpoint.class);
    private final JmsConnector connector = mock(JmsConnector.class, RETURNS_DEEP_STUBS);
    private final JmsSupport support = mock(JmsSupport.class);
    private final MessageConsumer consumer = mock(MessageConsumer.class);
    private final MuleContext muleContext = mock(MuleContext.class);
    private final TransactionConfig transactionConfig = mock(TransactionConfig.class);
    private final Transaction transaction = mock(Transaction.class);
    private JmsMessageRequester messageRequester;

    @Before
    public void setUp() throws Exception
    {
        when(transactionConfig.isTransacted()).thenReturn(true);
        TransactionCoordination.getInstance().bindTransaction(transaction);
        when(support.createConsumer(any(Session.class), any(Destination.class), anyString(), anyBoolean(), anyString(), anyBoolean(), any(ImmutableEndpoint.class))).thenReturn(consumer);
        when(connector.getJmsSupport()).thenReturn(support);
        when(endpoint.getConnector()).thenReturn(connector);
        when(endpoint.getMuleContext()).thenReturn(muleContext);
        when(endpoint.getTransactionConfig()).thenReturn(transactionConfig);
        messageRequester = new JmsMessageRequester(endpoint);
        messageRequester.initialise();
    }

    @Test
    public void testMessageConsumerIsClosed() throws Exception
    {
        messageRequester.doRequest(RECEIVE_NO_WAIT);
        verify(connector).closeQuietly(consumer);
    }

    @Test
    public void testListenerIsUnregistered() throws Exception
    {
        doAnswer(new Answer<Void>()
        {
            TransactionNotification transactionNotification = mock(TransactionNotification.class);
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                ServerNotificationListener listener = (ServerNotificationListener) invocation.getArguments()[0];
                listener.onNotification(transactionNotification);
                return null;
            }
        }).when(muleContext).registerListener(any(ServerNotificationListener.class), anyString());
        messageRequester.doRequest(RECEIVE_NO_WAIT);
        verify(muleContext).unregisterListener(any(ServerNotificationListener.class));
    }

    @After
    public void tearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction(transaction);
    }

}
