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
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Listener for exceptions managed by the {@link org.mule.api.exception.SystemExceptionHandler}.
 */
public class SystemExceptionListener
{

    private CountDownLatch exceptionThrownLatch = new Latch();
    private int timeout = 10000;
    private List<ExceptionNotification> exceptionNotifications = new ArrayList<>();

    public SystemExceptionListener(MuleContext muleContext)
    {
        try
        {
            final SystemExceptionHandler exceptionListener = muleContext.getExceptionListener();
            muleContext.setExceptionListener(new SystemExceptionHandler()
            {
                @Override
                public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
                {
                    try
                    {
                        exceptionListener.handleException(exception, rollbackMethod);
                    }
                    finally
                    {
                        exceptionThrownLatch.countDown();
                    }
                }

                @Override
                public void handleException(Exception exception)
                {
                    try
                    {
                        exceptionListener.handleException(exception);
                    }
                    finally
                    {
                        exceptionThrownLatch.countDown();
                    }
                }
            });
            muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
            {
                @Override
                public void onNotification(ExceptionNotification notification)
                {
                    exceptionNotifications.add(notification);

                }
            });
        }
        catch (NotificationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public SystemExceptionListener waitUntilAllNotificationsAreReceived()
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
        return this;
    }

    /**
     * @param numberOfExecutionsRequired number of times that the listener must be notified before releasing the latch.
     */
    public SystemExceptionListener setNumberOfExecutionsRequired(int numberOfExecutionsRequired)
    {
        this.exceptionThrownLatch = new CountDownLatch(numberOfExecutionsRequired);
        return this;
    }

    /**
     * @param timeout milliseconds to wait when calling {@link #waitUntilAllNotificationsAreReceived()} for an exception to be handled
     */
    public SystemExceptionListener setTimeoutInMillis(int timeout)
    {
        this.timeout = timeout;
        return this;
    }
}
