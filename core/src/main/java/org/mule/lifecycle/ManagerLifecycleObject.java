/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.ManagerNotification;

/**
 * TODO
 */
public class ManagerLifecycleObject extends NotificationLifecycleObject
{
    private Class notificationClass;

    public ManagerLifecycleObject(Class type, Class notificationClass)
    {
        super(type);
        this.notificationClass = notificationClass;
    }

    protected ServerNotification createNotification(MuleContext context, String action)
    {
        //return ClassUtils.instanciateClass(notificationClass, new Object[]{context, action});

        return new ManagerNotification(context, action);
    }
}
