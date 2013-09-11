/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.retry.async.AsynchronousRetryTemplate;
import org.mule.retry.policies.RetryForeverPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transaction.TransactionCoordination;

import java.lang.reflect.UndeclaredThrowableException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Matchers;

public class JmsConnectorTestCase extends AbstractMuleContextTestCase
{
    private static final String CLIENT_ID1 = "client1";
    private static final String CLIENT_ID2 = "client2";

    /**
     * Tests that client ID is set on the connection if it is originally null.
     */
    @Test
    public void testSetClientIDInConnectorForFirstTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(null);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);

        JmsConnector connector = new JmsConnector(muleContext);
        connector.setClientId(CLIENT_ID1);
        connector.setJmsSupport(jmsSupport);

        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        connector.setConnectionFactory(mockConnectionFactory);
        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(1)).setClientID(Matchers.anyString());
        verify(connection, times(1)).setClientID(CLIENT_ID1);
    }

    /**
     * Tests that client ID is set on the connection if it has a different client ID.
     */
    @Test
    public void testSetClientIDInConnectorForSecondTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(CLIENT_ID1);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);

        JmsConnector connector = new JmsConnector(muleContext);
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        connector.setConnectionFactory(mockConnectionFactory);
        connector.setClientId(CLIENT_ID2);
        connector.setJmsSupport(jmsSupport);

        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(1)).setClientID(Matchers.anyString());
        verify(connection, times(1)).setClientID(CLIENT_ID2);
    }

    /**
     * Tests that client ID is not set on the connection if it has the same client
     * ID.
     */
    @Test
    public void testSetClientIDInConnectionForFirstTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(CLIENT_ID1);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);
        
        JmsConnector connector = new JmsConnector(muleContext);
        connector.setClientId(CLIENT_ID1);
        connector.setJmsSupport(jmsSupport);

        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        connector.setConnectionFactory(mockConnectionFactory);
        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(0)).setClientID(Matchers.anyString());
    }

    @Test
    public void testClosesSessionIfThereIsNoActiveTransaction() throws Exception
    {
        JmsConnector connector = new JmsConnector(muleContext);

        Session session = mock(Session.class);
        connector.closeSessionIfNoTransactionActive(session);
        verify(session, times(1)).close();
    }

    @Test
    public void testDoNotClosesSessionIfThereIsAnActiveTransaction() throws Exception
    {
        Transaction transaction = mock(Transaction.class);
        TransactionCoordination.getInstance().bindTransaction(transaction);

        try
        {
            JmsConnector connector = new JmsConnector(muleContext);

            Session session = mock(Session.class);
            connector.closeSessionIfNoTransactionActive(session);
            verify(session, never()).close();
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(transaction);
        }
    }

    @Test
    public void ignoreJmsExceptionOnStop() throws Exception
    {
        Connection connection = mock(Connection.class);
        doThrow(new JMSException("connection unavailable")).when(connection).stop();
        JmsConnector connector = new JmsConnector(muleContext);
        JmsConnector spy = spy(connector);
        doReturn(connection).when(spy).createConnection();
        spy.doConnect();
        spy.doStop();
        verify(connection, times(1)).stop();
    }

    @Test
    public void ignoreAmqExceptionOnStop() throws Exception
    {
        Connection connection = mock(Connection.class);
        doThrow(new UndeclaredThrowableException(new Exception("connection unavailable"))).when(connection).stop();
        JmsConnector connector = new JmsConnector(muleContext);
        JmsConnector spy = spy(connector);
        doReturn(connection).when(spy).createConnection();
        spy.doConnect();
        spy.doStop();
        verify(connection, times(1)).stop();
    }

}
