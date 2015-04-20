/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to fire notifications of a specified type over a {@link org.mule.api.context.notification.ServerNotificationHandler}.
 */
public class NotificationHelper
{

    private static final Logger logger = LoggerFactory.getLogger(NotificationHelper.class);

    private ServerNotificationHandler serverNotificationHandler;

    private Class<? extends ServerNotification> notificationClass;

    private final boolean dynamicNotifications;
    private boolean notificationEnabled;

    /**
     * Creates a new {@link org.mule.context.notification.NotificationHelper} that emits notifications of the specified
     * class over the passed {@link org.mule.api.context.notification.ServerNotificationHandler}.
     * @param serverNotificationHandler The {@link org.mule.api.context.notification.ServerNotificationHandler} tu be used to fire notifications
     * @param notificationClass The {@link java.lang.Class} of the notifications to be fired by this helper
     * @param dynamicNotifications If true, notifications will be fired directly on the {@link org.mule.api.context.notification.ServerNotificationHandler}
     *                             received as parameter and the handler will be responsible to decide to emit it or not.
     *                             If false, at creation time the notification will be checked to be enable or not, if not any notification will be
     *                             discarded.
     */
    public NotificationHelper(ServerNotificationHandler serverNotificationHandler, Class<? extends ServerNotification> notificationClass, boolean dynamicNotifications)
    {
        this.dynamicNotifications = dynamicNotifications;
        this.notificationClass = notificationClass;
        this.serverNotificationHandler = serverNotificationHandler;
        if (!dynamicNotifications)
        {
            serverNotificationHandler = new OptimisedNotificationHandler(serverNotificationHandler, notificationClass);
            notificationEnabled = serverNotificationHandler.isNotificationEnabled(notificationClass);
        }
    }

    public boolean isNotificationEnabled()
    {
        return notificationEnabled || (dynamicNotifications && serverNotificationHandler.isNotificationEnabled(notificationClass));
    }

    public void fireNotification(MuleMessage message, String uri, FlowConstruct flowConstruct, int action)
    {
        try
        {
            if (isNotificationEnabled())
            {
                serverNotificationHandler.fireNotification(
                        new ConnectorMessageNotification(
                                message,
                                uri,
                                flowConstruct,
                                action));
            }
        }
        catch (Exception e)
        {
            logger.warn("Could not fire notification. Action: " + action, e);
        }

    }

    public void fireNotification(ServerNotification notification)
    {
        serverNotificationHandler.fireNotification(notification);
    }
}
