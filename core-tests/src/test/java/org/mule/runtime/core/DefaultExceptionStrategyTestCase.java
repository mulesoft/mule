/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class DefaultExceptionStrategyTestCase extends AbstractMuleContextTestCase
{
    // MULE-1404
    @Test
    public void testExceptions() throws Exception
    {
        InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy(muleContext);
        strategy.setMuleContext(muleContext);
        strategy.handleException(new IllegalArgumentException("boom"));
        assertEquals(1, strategy.getCount());
    }

    // MULE-1627
    @Test
    public void testExceptionNotifications() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger notificationCount = new AtomicInteger(0);

        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            public void onNotification(ExceptionNotification notification)
            {
                if (notification.getAction() == ExceptionNotification.EXCEPTION_ACTION)
                {
                    assertEquals("exception", notification.getActionName());
                    assertEquals("Wrong info type", ServerNotification.TYPE_ERROR, notification.getType());
                    notificationCount.incrementAndGet();
                    latch.countDown();
                }
            }
        });

        // throwing exception
        InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy(muleContext);
        strategy.setMuleContext(muleContext);
        strategy.handleException(new IllegalArgumentException("boom"));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1, notificationCount.get());

    }

    private static class InstrumentedExceptionStrategy extends DefaultSystemExceptionStrategy
    {
        private volatile int count = 0;

        public InstrumentedExceptionStrategy(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public void handleException(Exception exception)
        {
            count++;
            super.handleException(exception);
        }

        public int getCount()
        {
            return count;
        }
    }
}
