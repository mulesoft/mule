/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.registry.PreInitProcessor;

/**
 * Will register any {@link org.mule.api.context.notification.ServerNotificationListener} instances with the MuleContext
 * to receive notifications
 *
 * @deprecated as of 3.7.0 since these are only used by {@link org.mule.registry.TransientRegistry} which is also deprecated. Use post processors
 * for currently supported registries instead (i.e: {@link org.mule.config.spring.SpringRegistry})
 */
@Deprecated
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
