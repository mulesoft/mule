/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.apache.activemq.command.ActiveMQMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.exception.MessageRedeliveredException;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractJmsRedeliveryTestCase extends AbstractServiceAndFlowTestCase
{

    protected static final String JMS_INPUT_QUEUE = "jms://in?connector=jmsConnectorLimitedRedelivery";
    protected static final String JMS_INPUT_QUEUE2 = "jms://in2?connector=jmsConnectorNoRedelivery";
    protected static final String JMS_DEAD_LETTER = "jms://dead.letter?connector=jmsConnectorNoRedelivery";
    protected final int timeout = getTestTimeoutSecs() * 1000 / 4;

    protected MuleClient client;
    protected Latch messageRedeliveryExceptionFired;
    protected CounterCallback callback;

    public AbstractJmsRedeliveryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        System.setProperty("maxRedelivery", String.valueOf(getMaxRedelivery()));
        System.setProperty("maxRedeliveryAttempts", String.valueOf(getMaxRedeliveryAttempts()));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.SERVICE, "jms-redelivery-service.xml"},
                {ConfigVariant.FLOW, "jms-redelivery-flow.xml"}});
    }

    @Before
    public void setUp() throws Exception
    {
        client = muleContext.getClient();
        messageRedeliveryExceptionFired = new Latch();
        registerEventListener(messageRedeliveryExceptionFired);
        purgeQueue();
        setupCallback();
    }

    protected void assertMessageInDlq() throws MuleException
    {
        MuleMessage dl = client.request(JMS_DEAD_LETTER, 1000);
        assertNotNull(dl);
        assertTrue(dl.getPayload() instanceof ExceptionMessage);
        ExceptionMessage em = (ExceptionMessage) dl.getPayload();
        assertNotNull(em.getException());
        assertTrue(em.getException() instanceof MessageRedeliveredException);
    }

    protected void assertMessageInDlqRollbackEs() throws Exception
    {
        MuleMessage dl = client.request(JMS_DEAD_LETTER, 1000);
        assertNotNull(dl);        
        assertTrue(dl.getPayloadAsString().equals(TEST_MESSAGE));
    }

    protected void purgeQueue() throws MuleException
    {
        // required if broker is not restarted with the test - it tries to deliver those messages to the client
        // purge the queue
        while (client.request(JMS_INPUT_QUEUE, 1000) != null)
        {
            logger.warn("Destination " + JMS_INPUT_QUEUE + " isn't empty, draining it");
        }
    }

    protected void setupCallback() throws Exception
    {
        callback = createCallback();
        FunctionalTestComponent ftc = getFunctionalTestComponent("Bouncer");
        FunctionalTestComponent ftc2 = getFunctionalTestComponent("Bouncer2");
        ftc.setEventCallback(callback);
        ftc2.setEventCallback(callback);
    }

    private CounterCallback createCallback()
    {
        // enhance the counter callback to count, then throw an exception
        return new CounterCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object Component) throws Exception
            {
                final int count = incCallbackCount();
                logger.info("Message Delivery Count is: " + count);
                throw new FunctionalTestException();
            }
        };
    }

    private void registerEventListener(final Latch messageRedeliveryExceptionFired) throws NotificationException
    {
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            public void onNotification(ExceptionNotification notification)
            {
                if (notification.getException() instanceof MessageRedeliveredException)
                {
                    messageRedeliveryExceptionFired.countDown();
                }
            }
        });
    }

    protected void assertNoMessageInDlq(String location) throws MuleException
    {
        assertNull(client.request(location, 1000));
    }

    @After
    public void cleanUpMaxRedelivery()
    {
        System.clearProperty("maxRedelivery");
        System.clearProperty("maxRedeliveryAttempts");
    }

    protected abstract int getMaxRedelivery();

    protected abstract int getMaxRedeliveryAttempts();
}
