/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.transport.jms.xa.DefaultXAConnectionFactoryWrapper;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.reflect.UndeclaredThrowableException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jms.connection.CachingConnectionFactory;

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

    @Test
    public void doNotChangeConnectionFactoryWhenNotUsingXAConnectionFactory() throws Exception
    {
        muleContext.setTransactionManager(mock(TransactionManager.class));
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        JmsConnector connector = createConnectionFactoryWhenGettingConnection(mockConnectionFactory);
        assertThat(connector.getConnectionFactory(), instanceOf(CustomCachingConnectionFactory.class));
        assertThat(((CachingConnectionFactory) connector.getConnectionFactory()).getTargetConnectionFactory(),
                   is(mockConnectionFactory));
    }

    @Test
    public void doNotChangeConnectionFactoryWhenNotUsingTransactionManager() throws Exception
    {
        ConnectionFactory mockConnectionFactory = mock(TestXAConnectionFactory.class);
        JmsConnector connector = createConnectionFactoryWhenGettingConnection(mockConnectionFactory);
        assertThat(connector.getConnectionFactory(), is(mockConnectionFactory));
    }

    @Test
    public void createConnectionFactoryWrapperWhenUsingTransactionManager() throws Exception
    {
        muleContext.setTransactionManager(mock(TransactionManager.class));
        JmsConnector connector = createConnectionFactoryWhenGettingConnection(mock(TestXAConnectionFactory.class));
        assertThat(connector.getConnectionFactory(), instanceOf(DefaultXAConnectionFactoryWrapper.class));
    }
    
    @Test
    public void changesClassLoaderOnNotification() throws Exception
    {
        /**
         * Fetches a ClusterNodeNotificationListener added to a mock mule context
         */
        class NotificationAnswer implements Answer 
        {
            ClusterNodeNotificationListener listener;
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable 
            {
                listener = (ClusterNodeNotificationListener) invocation.getArguments()[0];
                return null;
            }
            
            public ClusterNodeNotificationListener getListener()
            {
                return listener;
            }
        };

        /**
         * Validates the classloader used in connector's connect is the app classloader
         */
        class ConnectClassLoaderCheckAnswer implements Answer
        {
            ClassLoader expectedClassLoader;
            
            public ConnectClassLoaderCheckAnswer(ClassLoader expectedClassLoader)
            {
                  this.expectedClassLoader = expectedClassLoader;
            }
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                ClassLoader connectClassLoader = Thread.currentThread().getContextClassLoader();
                assertThat(connectClassLoader, is(expectedClassLoader));
                return null;
            }
        };

        /**
         * On testing the muleContext.getExecutionClassLoader wont be different than the test thread.
         * This is used to check the class loader is effectively changed when performing the connection 
         */
        ClassLoader expectedClassLoader = new ClassLoader() 
        {
            String thismakesme = "different";
        };
        
        NotificationAnswer notificationAnswer = new NotificationAnswer();
        MuleContext muleContextSpy = spy(muleContext);
        doReturn(false).when(muleContextSpy).isPrimaryPollingInstance();
        doReturn(expectedClassLoader).when(muleContextSpy).getExecutionClassLoader();
        doAnswer(notificationAnswer).when(muleContextSpy).registerListener(any(ClusterNodeNotificationListener.class));
        
        JmsConnector connectorSpy = spy(createConnectionFactoryWhenGettingConnection(mock(TestXAConnectionFactory.class),
                muleContextSpy));
        connectorSpy.setClientId("MyClientId");
        connectorSpy.initialise();
        connectorSpy.connect();

        // Next time we connect is called will be checked to be using expectedClassLoader
        doAnswer(new ConnectClassLoaderCheckAnswer(expectedClassLoader)).when(connectorSpy).connect();

        ClassLoader preNotificationClassLoader = Thread.currentThread().getContextClassLoader();
        notificationAnswer.getListener().onNotification(mock(ClusterNodeNotification.class));
        ClassLoader afterNotificationClassLoader = Thread.currentThread().getContextClassLoader();

        assertThat(preNotificationClassLoader, is(afterNotificationClassLoader));
    }

    private JmsConnector createConnectionFactoryWhenGettingConnection(ConnectionFactory mockConnectionFactory) throws JMSException, MuleException
    {
        return createConnectionFactoryWhenGettingConnection(mockConnectionFactory, muleContext);
    }
    
    private JmsConnector createConnectionFactoryWhenGettingConnection(ConnectionFactory mockConnectionFactory, MuleContext muleContext) throws JMSException, MuleException
    {
        final Connection connection = mock(Connection.class);

        JmsSupport jmsSupport = mock(Jms11Support.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory>any())).thenReturn(connection);

        JmsConnector connector = new JmsConnector(muleContext);
        connector.setJmsSupport(jmsSupport);

        connector.setName("testConnector");
        connector.setConnectionFactory(mockConnectionFactory);
        connector.createConnection();
        return connector;
    }

    private interface TestXAConnectionFactory extends ConnectionFactory, XAConnectionFactory
    {
    }

}
