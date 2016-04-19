/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.reliability;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.exception.DefaultSystemExceptionStrategy;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.transport.jms.redelivery.MessageRedeliveredException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsNull;
import org.junit.Test;

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
    protected String[] getConfigFiles()
    {
        return new String[] {
                "reliability/activemq-config.xml",
                "reliability/inbound-message-loss-flow.xml"
        };
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

        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse("Message should not have been redelivered",
                    messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRouterException() throws Exception
    {
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse("Message should not have been redelivered",
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

    @Test
    public void testCatchExceptionStrategyConsumesMessage() throws Exception
    {
        putMessageOnQueue("exceptionHandled");

        // Exception occurs using catch-exception-strategy that will always consume the message
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRollbackExceptionStrategyConsumesMessage() throws Exception
    {
        final CountDownLatch exceptionStrategyListener = new CountDownLatch(4);
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                exceptionStrategyListener.countDown();
            }
        });
        putMessageOnQueue("rollbackOnException");
        if (!exceptionStrategyListener.await(RECEIVE_TIMEOUT,TimeUnit.MILLISECONDS))
        {
            fail("Message should have been redelivered");
        }
        assertThat(muleContext.getClient().request("jms://rollbackOnException?connector=jmsConnectorNoRedelivery", RECEIVE_TIMEOUT / 10), IsNull.<Object>nullValue());
    }

    @Test
    public void testDefaultExceptionStrategyConsumesMessage() throws Exception
    {
        putMessageOnQueue("commitOnException");

        // Exception occurs using catch-exception-strategy that will always consume the message
        assertFalse("Message should not have been redelivered",
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }

}
