/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.reliability;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class JmsAcknowledgementTestCase extends AbstractJmsReliabilityTestCase
{
    @Rule
    public TestName testName = new TestName();

    private String queue;

    @Before
    public void createQueueName() {
        queue = testName.getMethodName();
    }

    @Override
    protected String getConfigFile()
    {
        return "reliability/activemq-config.xml";
    }

    @Test
    public void testAutoAckSync() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        putMessageOnQueue(queue);
        // Read message from queue
        Message msg = readMessageFromQueue(queue);
        assertNotNull(msg);
        // No more messages
        msg = readMessageFromQueue(queue);
        assertNull(msg);
    }

    @Test
    public void testClientAckSync() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
        putMessageOnQueue(queue);
        // Read message but don't acknowledge
        Message msg = readMessageFromQueue(queue);
        assertNotNull(msg);
        closeConsumer();

        // Message is still on queue
        msg = readMessageFromQueue(queue);
        assertNotNull(msg);
        // Acknowledge
        msg.acknowledge();
        closeConsumer();

        // Now message is gone
        msg = readMessageFromQueue(queue);
        assertNull(msg);
    }

    @Test
    public void testAutoAckAsync() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        final CountDownLatch received = new CountDownLatch(1);
        listenOnQueue(queue, new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                received.countDown();
            }
        });
        putMessageOnQueue(queue);
        received.await();
        closeConsumer();

        // Delivery was successful so message should be gone
        Message msg = readMessageFromQueue(queue);
        assertNull(msg);
    }

    @Test
    public void testAutoAckAsyncWithException() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        final CountDownLatch received = new CountDownLatch(1);
        listenOnQueue(queue, new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    received.countDown();
                    session.recover();
                } catch (JMSException e) {
                    fail(e.getMessage());
                }
            }
        });
        putMessageOnQueue(queue);
        received.await();
        closeConsumer();

        // Delivery failed so message should be back on the queue
        Message msg = readMessageFromQueue(queue);
        assertNotNull(msg);
    }

    @Test
    public void testClientAckAsync() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;

        listenOnQueue(queue, new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                try
                {
                    // Message is processed and acknowledged
                    message.acknowledge();
                }
                catch (JMSException e)
                {
                    fail(e.getMessage());
                }
            }
        });
        putMessageOnQueue(queue);
        Thread.sleep(500);
        closeConsumer();

        // Delivery was successful so message should be gone
        Message msg = readMessageFromQueue(queue);
        assertNull(msg);
    }

    @Test
    public void testClientAckAsyncWithException() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;

        listenOnQueue(queue, new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                // Exception occured, message is not acknowledged
            }
        });
        putMessageOnQueue(queue);
        Thread.sleep(500);
        closeConsumer();

        // Delivery failed so message should be back on the queue
        Message msg = readMessageFromQueue(queue);
        assertNotNull(msg);
    }
}
