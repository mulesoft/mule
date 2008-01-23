/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationHandler;

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

    /**
     * This returns a very "conservative" value - it is true if the notification or any subclass would be
     * accepted.  So if it returns false then you can be sure that there is no need to send the
     * notification.  On the other hand, if it returns true there is no guarantee that the notification
     * "really" will be dispatched to any listener.
     *
     * @param notfnClass Either the notification class being generated or some superclass
     * @return false if there is no need to dispatch the notification
     */
    public boolean isNotificationEnabled(Class notfnClass)
    {
        if ((!dynamic) && type.isAssignableFrom(notfnClass))
        {
            return enabled;
        }
        else
        {
            return delegate.isNotificationEnabled(notfnClass);
        }
    }

    public void fireNotification(ServerNotification notification)
    {
        if (isNotificationEnabled(notification.getClass()))
        {
            delegate.fireNotification(notification);
        }
    }

}
