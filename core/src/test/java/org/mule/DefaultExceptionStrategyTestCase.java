/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.tck.AbstractMuleTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class DefaultExceptionStrategyTestCase extends AbstractMuleTestCase
{
    // MULE-1404
    public void testExceptions() throws Exception
    {
        InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy();
        strategy.setMuleContext(muleContext);
        strategy.exceptionThrown(new IllegalArgumentException("boom"));
        assertEquals(1, strategy.getCount());
    }

    // MULE-1627
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
        InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy();
        strategy.setMuleContext(muleContext);
        strategy.exceptionThrown(new IllegalArgumentException("boom"));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1, notificationCount.get());

    }

    private static class InstrumentedExceptionStrategy extends DefaultExceptionStrategy
    {
        private volatile int count = 0;

        public InstrumentedExceptionStrategy()
        {
            super();
        }
        
        @Override
        protected void defaultHandler(Throwable t)
        {
            count++;
            super.defaultHandler(t);
        }

        @Override
        protected void logException(Throwable t)
        {
            // do not log anything here, we're running as part of a unit test
        }

        public int getCount()
        {
            return count;
        }
    }
}
