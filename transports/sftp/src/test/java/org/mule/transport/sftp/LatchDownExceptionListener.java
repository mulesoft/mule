/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;

import java.util.concurrent.CountDownLatch;

/**
 * @author alejandrosequeira Exception listener that decreases a latch when notified.
 */
@SuppressWarnings("rawtypes")
public class LatchDownExceptionListener implements ExceptionNotificationListener
{

    private final CountDownLatch latch;

    public LatchDownExceptionListener(CountDownLatch latch)
    {
        this.latch = latch;
    }

    /**
     * @see org.mule.api.context.notification.ServerNotificationListener#onNotification(org.mule.api.context.notification.ServerNotification)
     */
    @Override
    public void onNotification(ServerNotification notification)
    {
        latch.countDown();
    }

}
