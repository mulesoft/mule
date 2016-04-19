/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.reliability;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.junit.Test;

public class JmsAcknowledgementTestCase extends AbstractJmsReliabilityTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "reliability/activemq-config.xml";
    }

    @Test
    public void testAutoAckSync() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        putMessageOnQueue("sanity");
        // Read message from queue
        Message msg = readMessageFromQueue("sanity");
        assertNotNull(msg);
        // No more messages
        msg = readMessageFromQueue("sanity");
        assertNull(msg);
    }

    @Test
    public void testClientAckSync() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
        putMessageOnQueue("sanity");
        // Read message but don't acknowledge
        Message msg = readMessageFromQueue("sanity");
        assertNotNull(msg);
        closeConsumer();

        // Message is still on queue
        msg = readMessageFromQueue("sanity");
        assertNotNull(msg);
        // Acknowledge
        msg.acknowledge();
        closeConsumer();

        // Now message is gone
        msg = readMessageFromQueue("sanity");
        assertNull(msg);
    }

    @Test
    public void testAutoAckAsync() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

        listenOnQueue("sanity", new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                // Message is processed normally
            }
        });
        putMessageOnQueue("sanity");
        Thread.sleep(500);
        closeConsumer();

        // Delivery was successful so message should be gone
        Message msg = readMessageFromQueue("sanity");
        assertNull(msg);
    }

    @Test
    public void testAutoAckAsyncWithException() throws Exception
    {
        acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

        listenOnQueue("sanity", new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                try
                {
                    session.recover();
                }
                catch (JMSException e)
                {
                    fail(e.getMessage());
                }
            }
        });
        putMessageOnQueue("sanity");
        Thread.sleep(500);
        closeConsumer();

        // Delivery failed so message should be back on the queue
        Message msg = readMessageFromQueue("sanity");
        assertNotNull(msg);
    }

    @Test
    public void testClientAckAsync() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;

        listenOnQueue("sanity", new MessageListener()
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
        putMessageOnQueue("sanity");
        Thread.sleep(500);
        closeConsumer();

        // Delivery was successful so message should be gone
        Message msg = readMessageFromQueue("sanity");
        assertNull(msg);
    }

    @Test
    public void testClientAckAsyncWithException() throws Exception
    {
        acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;

        listenOnQueue("sanity", new MessageListener()
        {
            @Override
            public void onMessage(Message message)
            {
                // Exception occured, message is not acknowledged
            }
        });
        putMessageOnQueue("sanity");
        Thread.sleep(500);
        closeConsumer();

        // Delivery failed so message should be back on the queue
        Message msg = readMessageFromQueue("sanity");
        assertNotNull(msg);
        msg.acknowledge();
    }
}
