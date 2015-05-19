/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.listener;

import static org.junit.Assert.fail;
import org.mule.api.MuleContext;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Listener for connection notifications.
 */
public class ConnectionListener
{

    private CountDownLatch notificationReceivedLatch = new Latch();
    private int timeout = 10000;
    private int expectedAction = ConnectionNotification.CONNECTION_CONNECTED;

    public
    ConnectionListener(MuleContext muleContext)
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

}
