/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.AsyncMessageNotificationListener;

import java.util.List;

public class AsyncMessageNotificationLogger extends PipelineAndAsyncMessageNotificationLogger
    implements AsyncMessageNotificationListener<AsyncMessageNotification>, NotificationLogger
{

    public synchronized void onNotification(AsyncMessageNotification notification)
    {
        notifications.addLast(notification);
    }

    public List getNotifications()
    {
        return notifications;
    }
}
