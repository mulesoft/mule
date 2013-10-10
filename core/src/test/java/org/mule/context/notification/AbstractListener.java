/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;

public abstract class AbstractListener<T extends ServerNotification> implements ServerNotificationListener<T>
{

    private T notification = null;

    public void onNotification(T notification)
    {
        this.notification = notification;
    }

    public boolean isNotified()
    {
        return null != notification;
    }

}
