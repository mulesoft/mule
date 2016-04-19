/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.transport.jms.Jms11Support;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.junit.Test;

public class Jms11SupportTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testNoLocalCalledForDurableTopic() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(muleContext));
        Topic topic = mock(Topic.class);
        String durableName = "durableName";
        boolean noLocal = true;
        Session session = mock(Session.class);

        jmsSupport.createConsumer(session, topic, null, noLocal, durableName, true, getTestInboundEndpoint("test"));
        verify(session).createDurableSubscriber(eq(topic), eq(durableName), isNull(String.class), eq(true));
    }

    @Test
    public void testNoLocalCalledForNonDurableTopic() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(muleContext));
        Topic topic = mock(Topic.class);
        boolean noLocal = true;
        Session session = mock(Session.class);

        jmsSupport.createConsumer(session, topic, null, noLocal, null, true, getTestInboundEndpoint("test"));
        verify(session).createConsumer(eq(topic), isNull(String.class), eq(true));
    }

    @Test
    public void testNoLocalNotCalledForQueue() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(muleContext));
        Queue queue = mock(Queue.class);
        boolean noLocal = true;
        Session session = mock(Session.class);

        jmsSupport.createConsumer(session, queue, null, noLocal, null, false, getTestInboundEndpoint("test"));
        verify(session).createConsumer(eq(queue), isNull(String.class));
    }

    @Test
    public void testDurableWithQueueThrowsException() throws Exception
    {
        Jms11Support jmsSupport = new Jms11Support(new JmsConnector(muleContext));
        Queue queue = mock(Queue.class);
        String durableName = "durableName";
        boolean noLocal = true;
        Session session = mock(Session.class);

        try
        {
            jmsSupport.createConsumer(session, queue, null, noLocal, durableName, false, getTestInboundEndpoint("test"));
        }
        catch (JMSException jmsex)
        {
            // expected
            assertEquals("Wrong exception text.",
                "A durable subscriber name was set but the destination was not a Topic", jmsex.getMessage());
        }
    }
}
