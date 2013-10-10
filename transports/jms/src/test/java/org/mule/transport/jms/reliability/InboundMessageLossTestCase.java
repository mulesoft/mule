/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.reliability;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.jms.redelivery.MessageRedeliveredException;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 *
 * In the case of JMS, this will cause the failed message to be redelivered if
 * JMSRedelivery is configured.
 */
public class InboundMessageLossTestCase extends AbstractJmsReliabilityTestCase
{
    protected Latch messageRedelivered;
    protected final int latchTimeout = 5000;

    @Override
    protected String getConfigResources()
    {
        return "reliability/activemq-config.xml, reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));

        // Tell us when a MessageRedeliverdException has been handled
        messageRedelivered = new Latch();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                if (notification.getException() instanceof MessageRedeliveredException)
                {
                    messageRedelivered.countDown();
                }
            }
        });
    }

    @Test
    public void testNoException() throws Exception
    {
        putMessageOnQueue("noException");

        // Delivery was successful
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTransformerException() throws Exception
    {
        putMessageOnQueue("transformerException");

        // Delivery failed so message should have been redelivered
        assertTrue("Message was not redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRouterException() throws Exception
    {
        putMessageOnQueue("routerException");

        // Delivery failed so message should have been redelivered
        assertTrue("Message was not redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testComponentException() throws Exception
    {
        putMessageOnQueue("componentException");

        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }
}
