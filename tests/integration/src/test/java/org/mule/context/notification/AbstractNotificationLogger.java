/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractNotificationLogger<T extends ServerNotification> implements NotificationLogger
{

    private LinkedList notifications = new LinkedList();

    public synchronized void onNotification(T notification)
    {
        notifications.addLast(notification);
    }

    public List getNotifications()
    {
        return notifications;
    }

}
