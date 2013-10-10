/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.PipelineMessageNotificationListener;

import java.util.List;

public class PipelineMessageNotificationLogger extends PipelineAndAsyncMessageNotificationLogger
    implements PipelineMessageNotificationListener<PipelineMessageNotification>
{

    public synchronized void onNotification(PipelineMessageNotification notification)
    {
        notifications.addLast(notification);
    }

    public List getNotifications()
    {
        return notifications;
    }
}
