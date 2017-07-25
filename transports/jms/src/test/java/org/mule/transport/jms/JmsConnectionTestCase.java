/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transport.jms.JmsConnector.CONNECTION_STOPPING_ERROR_MESSAGE;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * TestCase to prevent sessions be created when connection is being stopped.
 * This case avoids a deadlock can take place.
 */
public class JmsConnectionTestCase extends AbstractMuleContextTestCase
{
    private JmsConnector jmsConnector = mock(JmsConnector.class);
    private QueueConnectionFactory connectionFactory = mock(QueueConnectionFactory.class);
    private QueueConnection connection = mock(QueueConnection.class);
    private final ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class, RETURNS_DEEP_STUBS);
    private Latch connectionLatch = new Latch();


    @Before
    public void setUp() throws Exception
    {
        jmsConnector = new JmsConnector(muleContext);
        jmsConnector.setConnectionFactory(connectionFactory);
        when(connectionFactory.createQueueConnection()).thenReturn(connection);
        when(endpoint.getEndpointURI().toString()).thenReturn("");
        jmsConnector.initialise();
        jmsConnector.doConnect();
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                connectionLatch.await();
                return null;
            }
        }).when(connection).stop();

    }

    @Test
    public void testCreateSessionWhenConnectionIsStopping() throws Exception
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    jmsConnector.doStop();
                }
                catch (Exception e)
                {
                    // Ignoring exception
                }
            }
        });
        thread.start();
        try
        {
            jmsConnector.createSession(endpoint);
            fail("An exception must be triggered because it is not possible to create a session when the connection is being stopped");
        }
        catch (Exception e)
        {
            assertThat(e.getMessage(), is(CONNECTION_STOPPING_ERROR_MESSAGE));
        }
        finally
        {
            connectionLatch.countDown();
        }
    }
}
