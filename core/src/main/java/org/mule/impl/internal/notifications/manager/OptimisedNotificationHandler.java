/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

import org.mule.umo.manager.UMOServerNotification;

/**
 * Optimized to make a quick decision on a particular class of messages.
 */
public class OptimisedNotificationHandler implements ServerNotificationHandler
{

    private ServerNotificationHandler delegate;
    private Class type;
    private boolean dynamic = false;
    private boolean enabled = false;

    public OptimisedNotificationHandler(ServerNotificationHandler delegate, Class type)
    {
        this.delegate = delegate;
        this.type = type;
        dynamic = delegate.isNotificationDynamic();
        enabled = delegate.isNotificationEnabled(type);
    }

    public boolean isNotificationDynamic()
    {
        return dynamic;
    }

    public boolean isNotificationEnabled(Class type)
    {
        if ((!dynamic) && this.type.isAssignableFrom(type))
        {
            return enabled;
        }
        else
        {
            return delegate.isNotificationEnabled(type);
        }
    }

    public void fireNotification(UMOServerNotification notification)
    {
        if (isNotificationEnabled(notification.getClass()))
        {
            delegate.fireNotification(notification);
        }
    }

}
