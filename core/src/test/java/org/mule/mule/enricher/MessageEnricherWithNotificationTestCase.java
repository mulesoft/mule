/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mule.enricher;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.enricher.MessageEnricher;
import org.mule.tck.SensingNullMessageProcessor;

public class MessageEnricherWithNotificationTestCase extends AbstractEnricherTestCase
{

    @BeforeClass
    public static void before()
    {
        System.setProperty(MuleProperties.MULE_HANDLE_COPY_OF_EVENT_IN_MESSAGE_PROCESSOR_NOTIFICATION, "true");
    }

    @Test
    public void testRaceConditionBetweenEnricherAndNotificationListener() throws Exception
    {
        muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class, MessageProcessorNotification.class);
        muleContext.getNotificationManager().addListener(new MessageProcessorNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                if (MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE == notification.getAction())
                {
                    final ThreadSafeAccess event = (ThreadSafeAccess) notification.getSource();
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            while (true)
                            {
                                event.resetAccessControl();
                            }
                        }
                    }).start();
                }
            }
        });
        MessageEnricher enricher = createEnricher(new SensingNullMessageProcessor());
        final MuleEvent in = createBlockingEvent();
        try
        {
            processEnricherInChain(enricher, in);
        }
        catch (Exception e)
        {
            fail("Enricher has lost the ownership of the thread");
        }
    }
}
