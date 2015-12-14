/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.listener;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MuleContext;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.Preconditions;
import org.mule.util.concurrent.Latch;

import com.google.common.base.Optional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Listener for connection notifications.
 */
public class ConnectionListener
{

    private CountDownLatch notificationReceivedLatch = new Latch();
    private int timeout = 10000;
    private Optional<Long> previousNotificationTimestamp = Optional.absent();
    private Optional<Long> minimumTimeBetweenNotifications = Optional.absent();
    private int expectedAction = ConnectionNotification.CONNECTION_CONNECTED;

    public ConnectionListener(MuleContext muleContext)
    {
        try
        {
            muleContext.registerListener(new ConnectionNotificationListener<ConnectionNotification>()
            {
                @Override
                public void onNotification(ConnectionNotification notification)
                {
                    if (notification.getAction() == expectedAction)
                    {
                        long currentNotificationTimestamp = System.currentTimeMillis();
                        if (previousNotificationTimestamp.isPresent())
                        {
                            long timeBetweenNotifications = currentNotificationTimestamp - previousNotificationTimestamp.get();
                            if (!minimumTimeBetweenNotifications.isPresent() || minimumTimeBetweenNotifications.get() > timeBetweenNotifications)
                            {
                                minimumTimeBetweenNotifications = Optional.of(timeBetweenNotifications);
                            }
                        }
                        previousNotificationTimestamp = Optional.of(currentNotificationTimestamp);
                        notificationReceivedLatch.countDown();
                    }
                }
            });
        }
        catch (NotificationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void waitUntilNotificationsAreReceived()
    {
        try
        {
            if (!notificationReceivedLatch.await(timeout, TimeUnit.MILLISECONDS))
            {
                fail("Expected notifications were not received");
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param numberOfExecutionsRequired number of times that the listener must be notified before releasing the latch.
     */
    public ConnectionListener setNumberOfExecutionsRequired(int numberOfExecutionsRequired)
    {
        this.notificationReceivedLatch = new CountDownLatch(numberOfExecutionsRequired);
        return this;
    }

    public ConnectionListener setTimeoutInMillis(int timeout)
    {
        this.timeout = timeout;
        return this;
    }

    public ConnectionListener setExpectedAction(int expectedAction)
    {
        this.expectedAction = expectedAction;
        return this;
    }

    /**
     * @return resets the listener state so it can be reused
     */
    public ConnectionListener reset()
    {
        this.notificationReceivedLatch = new Latch();
        return this;
    }

    /**
     * @return the minimum time tracked between all received notifications
     * @throws IllegalStateException if there were less than two notifications received
     */
    public void assertMinimumTimeBetweenNotifications(long expectedTimeBetweenNotifications)
    {
        Preconditions.checkState(minimumTimeBetweenNotifications.isPresent(), "At least two notifications must be received in order to get the minimum time between notifications");
        assertThat(minimumTimeBetweenNotifications.get(), greaterThan(expectedTimeBetweenNotifications));
    }
}
