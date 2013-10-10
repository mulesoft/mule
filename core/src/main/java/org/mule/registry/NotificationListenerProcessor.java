/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.registry.PreInitProcessor;

/**
 * Will register any {@link org.mule.api.context.notification.ServerNotificationListener} instances with the MuleContext
 * to receive notifications
 */
public class NotificationListenerProcessor implements PreInitProcessor
{
    private MuleContext context;

    public NotificationListenerProcessor(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if (object instanceof ServerNotificationListener)
        {
            context.getNotificationManager().addListener((ServerNotificationListener) object);
        }
        return object;
    }
}
