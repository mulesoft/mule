/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
