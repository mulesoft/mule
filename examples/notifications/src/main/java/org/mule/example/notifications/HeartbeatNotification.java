/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.notifications;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.MuleContext;
import org.mule.context.notification.CustomNotification;

import javax.servlet.ServletContext;

/**
 * A simple notification that fires repeatedly to notify tha the Mule server is alive and well.
 */
public class HeartbeatNotification extends CustomNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3246036188011581121L;

    public static final int HEARTBEAT = CUSTOM_EVENT_ACTION_START_RANGE + 1300;

    static {
        registerAction("mule heartbeat", HEARTBEAT);
    }

    public HeartbeatNotification(MuleContext context)
    {
        super(getId(context), HEARTBEAT, context.getConfiguration().getId());
    }

    private static String getId(MuleContext context)
    {
        return context.getConfiguration().getDomainId() + "." + context.getConfiguration().getClusterId() + "." + context.getConfiguration().getId();
    }

    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
                + ", timestamp=" + timestamp + "}";
    }

}