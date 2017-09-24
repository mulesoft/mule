/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.notification.AbstractServerNotification.TYPE_ERROR;
import static org.mule.runtime.api.notification.ExceptionNotification.EXCEPTION_ACTION;

import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultExceptionStrategyTestCase extends AbstractMuleContextTestCase {

  // MULE-1404
  @Test
  public void testExceptions() throws Exception {
    InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy(muleContext);
    strategy.setMuleContext(muleContext);
    strategy
        .setNotificationFirer(((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
    strategy.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    strategy.handleException(new IllegalArgumentException("boom"));
    assertEquals(1, strategy.getCount());
  }

  // MULE-1627
  @Test
  public void testExceptionNotifications() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicInteger notificationCount = new AtomicInteger(0);

    ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationListenerRegistry.class)
        .registerListener((ExceptionNotificationListener) notification -> {
          if (new IntegerAction(EXCEPTION_ACTION).equals(notification.getAction())) {
            assertEquals("exception", notification.getActionName());
            assertEquals("Wrong info type", TYPE_ERROR, notification.getType());
            notificationCount.incrementAndGet();
            latch.countDown();
          }
        });

    // throwing exception
    InstrumentedExceptionStrategy strategy = new InstrumentedExceptionStrategy(muleContext);
    strategy.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    strategy.setMuleContext(muleContext);
    strategy
        .setNotificationFirer(((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
    strategy.handleException(new IllegalArgumentException("boom"));

    // Wait for the notifcation event to be fired as they are queue
    latch.await(2000, MILLISECONDS);
    assertEquals(1, notificationCount.get());

  }

  private static class InstrumentedExceptionStrategy extends DefaultSystemExceptionStrategy {

    private volatile int count = 0;

    public InstrumentedExceptionStrategy(MuleContext muleContext) {
      super();
    }

    @Override
    public void handleException(Exception exception) {
      count++;
      super.handleException(exception);
    }

    public int getCount() {
      return count;
    }
  }
}
