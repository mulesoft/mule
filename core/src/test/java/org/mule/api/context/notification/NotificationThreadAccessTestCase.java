/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.context.notification;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NotificationThreadAccessTestCase extends AbstractMuleContextTestCase
{

    private final CountDownLatch exceptionLatch = new CountDownLatch(1);
    private final CatchMessagingExceptionStrategy exceptionStrategy = new CatchMessagingExceptionStrategy();


    @Test
    public void raceConditionForThreadOwnership() throws Exception
    {
        muleContext.getNotificationManager().addInterfaceToType(ExceptionStrategyNotificationListener.class, ExceptionStrategyNotification.class);
        muleContext.getNotificationManager().addListener(new ExceptionStrategyNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                while (!currentThread().isInterrupted())
                {
                    try
                    {
                        ((ThreadSafeAccess) notification.getSource()).resetAccessControl();
                    }
                    catch (Throwable e)
                    {
                        exceptionLatch.countDown();
                    }
                }
            }
        });
        final MuleEvent event = getTestEvent(null);

        MessageProcessor threadAccessMessageProcessor = new MessageProcessor()
        {
            @Override
            public MuleEvent process(final MuleEvent event)
            {

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        while (!currentThread().isInterrupted())
                        {
                            try
                            {
                                final ThreadSafeAccess threadSafeAccess = (ThreadSafeAccess) event;
                                threadSafeAccess.resetAccessControl();
                            }
                            catch (Throwable e)
                            {
                                exceptionLatch.countDown();
                            }
                        }
                    }
                }).start();
                return event;
            }
        };
        exceptionStrategy.setMessageProcessors(asList(threadAccessMessageProcessor));
        exceptionStrategy.setMuleContext(muleContext);
        exceptionStrategy.initialise();

        exceptionStrategy.handleException(new RuntimeException("TestException"), event);

        assertThat("An exception caused by a race condition for getting the message ownership was triggered", exceptionLatch.await(5000, TimeUnit.MILLISECONDS), is(false));
    }
}
