/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.jms.connection.SingleConnectionFactory;

/**
 * Tests that JMS message are correctly sent when caching elements
 */
@RunWith(MockitoJUnitRunner.class)
public class OutboundSessionAndProducerReuseTestCase extends AbstractMuleContextTestCase
{

    public static final String TEST_MESSAGE_1 = "test1";

    private JmsConnector connector;
    @Mock
    private QueueConnectionFactory connectionFactory;
    @Mock
    private QueueConnection connection;
    @Mock
    private Queue queue;


    private ExceptionListener connectionExceptionListener;
    private String connectionClientId;
    private OutboundEndpoint outboundEndpoint;
    private CountDownLatch messageSentLatch = new CountDownLatch(1);


    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connectionFactory.createQueueConnection()).thenReturn(connection);
        when(connection.createSession(false, 1)).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return createSessionMock();
            }
        });
        when(connection.createQueueSession(false, 1)).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return createSessionMock();
            }
        });
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                connectionExceptionListener = (ExceptionListener) invocation.getArguments()[0];
                return null;
            }
        }).when(connection).setExceptionListener(Mockito.any(ExceptionListener.class));
        when(connection.getExceptionListener()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return connectionExceptionListener;
            }
        });
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                connectionClientId = (String) invocation.getArguments()[0];
                return null;
            }
        }).when(connection).setClientID(Mockito.any(String.class));
        when(connection.getClientID()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return connectionClientId;
            }
        });

        connector = new JmsConnector(muleContext);
        connector.setConnectionFactory(connectionFactory);
        SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate();
        retryPolicyTemplate.setMuleContext(muleContext);
        connector.setRetryPolicyTemplate(retryPolicyTemplate);
        connector.setJmsSupport(new Jms11Support(connector));

        EndpointBuilder epBuilder = new EndpointURIEndpointBuilder("jms://out", muleContext);
        epBuilder.setConnector(connector);
        outboundEndpoint = epBuilder.buildOutboundEndpoint();
    }

    private QueueSession createSessionMock() throws JMSException
    {
        QueueSession mock = mock(QueueSession.class);
        when(mock.createProducer(Mockito.any(Destination.class))).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return createProducerMock();
            }
        });
        when(mock.createConsumer(Mockito.any(Destination.class))).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return mock(MessageConsumer.class);
            }
        });
        when(mock.createTextMessage(Mockito.anyString())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                ActiveMQTextMessage msg = new ActiveMQTextMessage();
                msg.setText((String) invocation.getArguments()[0]);
                return msg;
            }
        });
        return mock;
    }

    private MessageProducer createProducerMock() throws JMSException
    {
        MessageProducer mock = mock(MessageProducer.class);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                messageSentLatch.countDown();
                return null;
            }
        }).when(mock).send(Mockito.any(Message.class),anyInt(),anyInt(), anyLong());
        return mock;
    }

    @Test
    public void connectionFactory() throws Exception
    {
        assertThat(connector.getConnectionFactory(), is(CoreMatchers.<ConnectionFactory>equalTo(connectionFactory)));
        connector.initialise();
        connector.connect();
        assertThat(connector.getConnectionFactory(), is(CoreMatchers.<ConnectionFactory>instanceOf(SingleConnectionFactory.class)));
        assertThat(((SingleConnectionFactory)connector.getConnectionFactory()).getTargetConnectionFactory(),
                   CoreMatchers.<ConnectionFactory>is(connectionFactory));
    }

    @Test
    public void connection() throws Exception
    {
        connector.initialise();
        connector.connect();
        assertThat(connector.getConnection(), is(not(CoreMatchers.<Connection>equalTo(connection))));
    }


    @Test
    public void clientId() throws Exception
    {
        String clientId = "foo";
        connector.setClientId(clientId);
        connector.initialise();
        connector.connect();
        assertThat(connector.getConnection().getClientID(), is(clientId));
    }

    @Test
    public void sessionReuse() throws Exception
    {
        connector.initialise();
        connector.connect();
        Session session1 = connector.createSession(outboundEndpoint);
        session1.close();
        Session session2 = connector.createSession(outboundEndpoint);

        assertThat(session1, equalTo(session2));
    }

    @Test
    public void producersReused() throws Exception
    {
        connector.initialise();
        connector.connect();
        Session session = connector.createSession(outboundEndpoint);
        MessageProducer producer1 = session.createProducer(queue);
        producer1.close();
        MessageProducer producer2 = session.createProducer(queue);
        producer2.close();

        assertThat(producer1.toString(), equalTo(producer2.toString()));
    }

    @Test
    public void consumersNotReused() throws Exception
    {
        connector.initialise();
        connector.connect();
        Session session = connector.createSession(outboundEndpoint);
        MessageConsumer consumer1 = session.createConsumer(queue);
        consumer1.close();
        MessageConsumer consumer2 = session.createConsumer(queue);
        consumer2.close();

        assertThat(consumer1.toString(), not(equalTo(consumer2.toString())));
    }


    @Test
    public void send() throws Exception
    {
        connector.initialise();
        connector.connect();
        connector.start();

        verify(connectionFactory, times(1)).createConnection();


        outboundEndpoint.process(getTestEvent(TEST_MESSAGE_1));

        verify(connectionFactory, times(1)).createConnection();
        verify(connection, times(1)).createSession(anyBoolean(), anyInt());

        assertTrue(messageSentLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
