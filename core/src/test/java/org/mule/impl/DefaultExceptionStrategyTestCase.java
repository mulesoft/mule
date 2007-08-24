/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.impl.internal.notifications.ExceptionNotification;
import org.mule.impl.internal.notifications.ExceptionNotificationListener;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOServerNotification;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class DefaultExceptionStrategyTestCase extends AbstractMuleTestCase
{
    // MULE-1404
    public void testExceptions() throws Exception
    {
        InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy();
        strategy.setManagementContext(managementContext);
        strategy.exceptionThrown(new IllegalArgumentException("boom"));
        assertEquals(1, strategy.getCount());
    }

    // MULE-1627
    public void testExceptionNotifications() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger notificationCount = new AtomicInteger(0);

        managementContext.registerListener(new ExceptionNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == ExceptionNotification.EXCEPTION_ACTION)
                {
                    assertEquals("exception", notification.getActionName());
                    assertEquals("Wrong info type", UMOServerNotification.TYPE_ERROR, notification.getType());
                    notificationCount.incrementAndGet();
                    latch.countDown();
                }
            }
        });

        // throwing exception
        DefaultExceptionStrategy strategy = new DefaultExceptionStrategy();
        strategy.setManagementContext(managementContext);
        strategy.exceptionThrown(new IllegalArgumentException("boom"));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1, notificationCount.get());

    }

    private class InstrumentedExceptionStrategy extends DefaultExceptionStrategy
    {
        private volatile int count = 0;

        // @Override
        protected void defaultHandler(Throwable t)
        {
            count++;
            super.defaultHandler(t);
        }

        public int getCount()
        {
            return count;
        }

    }

}
