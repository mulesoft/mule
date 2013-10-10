/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.notifications;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.context.notification.CustomNotification;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A simple notification that fires repeatedly to notify tha the Mule server is alive
 * and well.
 */
public class HeartbeatNotification extends CustomNotification implements BlockingServerEvent
{
    private static final long serialVersionUID = -3246036188011581121L;

    public static final int HEARTBEAT = CUSTOM_EVENT_ACTION_START_RANGE + 1300;

    static
    {
        registerAction("mule heartbeat", HEARTBEAT);
    }

    public HeartbeatNotification(MuleContext context)
    {
        super(getHostInfo(), HEARTBEAT, context.getConfiguration().getId());
    }

    protected static String getHostInfo()
    {
        try
        {
            InetAddress host = InetAddress.getLocalHost();
            return host.getHostName() + " (" + host.getHostAddress() + ")";
        }
        catch (UnknownHostException e)
        {
            return "unknown";
        }
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
               + ", timestamp=" + timestamp + "}";
    }
}
