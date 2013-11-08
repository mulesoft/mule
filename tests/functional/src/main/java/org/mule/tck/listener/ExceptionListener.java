/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.listener;

import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Listener for exception thrown by a message source or flow.
 */
public class ExceptionListener
{

    private CountDownLatch exceptionThrownLatch = new Latch();
    private int timeout = 10000;

    /**
     * Constructor for creating a listener for any exception thrown within a flow or message source.
     */
    public ExceptionListener(MuleContext muleContext)
    {
        try
        {
            muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
            {
                @Override
                public void onNotification(ExceptionNotification notification)
                {
                    exceptionThrownLatch.countDown();
                }
            });
        }
        catch (NotificationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void waitUntilAllNotificationsAreReceived()
    {
        try
        {
            if (!exceptionThrownLatch.await(timeout, TimeUnit.MILLISECONDS))
            {
                fail("An exception was never thrown");
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
    public ExceptionListener setNumberOfExecutionsRequired(int numberOfExecutionsRequired)
    {
        this.exceptionThrownLatch = new CountDownLatch(numberOfExecutionsRequired);
        return this;
    }

    public ExceptionListener setTimeoutInMillis(int timeout)
    {
        this.timeout = timeout;
        return this;
    }
}
