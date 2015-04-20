/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationHelper
{

    private static final Logger logger = LoggerFactory.getLogger(NotificationHelper.class);

    private ServerNotificationHandler serverNotificationHandler;

    private Class<? extends ServerNotification> notificationClass;

    private final boolean dynamicNotifications;
    private boolean notificationEnabled;


    public NotificationHelper(MuleContext muleContext, Class<? extends ServerNotification> notificationClass, boolean dynamicNotifications)
    {
        this.dynamicNotifications = dynamicNotifications;
        this.notificationClass = notificationClass;
        serverNotificationHandler = muleContext.getNotificationManager();
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
                        new MessageExchangeNotification(
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
