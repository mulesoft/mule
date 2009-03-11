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
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.util.ClassUtils;

import java.lang.reflect.Constructor;

/**
 * TODO
 */
public class NotificationLifecycleObject extends LifecycleObject
{
    private String preNotificationName;
    private String postNotificationName;
    private Constructor ctor;

    public NotificationLifecycleObject(Class type)
    {
        super(type);
    }

    public NotificationLifecycleObject(Class type, Class notificationClass)
    {
        super(type);

        if (notificationClass==null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("notificationClass").toString());
        }

        // MULE-2903: make sure the notifiactionClass is properly loaded and initialized
        notificationClass = ClassUtils.initializeClass(notificationClass);

        if (!ServerNotification.class.isAssignableFrom(notificationClass))
        {
            throw new IllegalArgumentException("Notification class must be of type: " + ServerNotification.class.getName());
        }

        ctor = ClassUtils.getConstructor(notificationClass, new Class[]{Object.class, String.class});
        if(ctor==null)
        {
            throw new IllegalArgumentException("No constructor defined in Notification class: " + notificationClass + " with arguments (Object.class, String.class)");
        }
    }

    public NotificationLifecycleObject(Class type, Class notificationClass, int preNotification, int postNotification)
    {
        this(type, notificationClass);
        setPreNotificationName(MuleContextNotification.getActionName(preNotification));
        setPostNotificationName(MuleContextNotification.getActionName(postNotification));
    }

    public String getPostNotificationName()
    {
        return postNotificationName;
    }

    public void setPostNotificationName(String postNotificationName)
    {
        this.postNotificationName = postNotificationName;
    }

    public String getPreNotificationName()
    {
        return preNotificationName;
    }

    public void setPreNotificationName(String preNotificationName)
    {
        this.preNotificationName = preNotificationName;
    }

    @Override
    public void firePreNotification(MuleContext context)
    {
        if(getPreNotificationName()!=null)
        {
            setPreNotification(createNotification(context, getPreNotificationName()));
        }
        super.firePreNotification(context);

    }

    @Override
    public void firePostNotification(MuleContext context)
    {
        if(getPostNotificationName()!=null)
        {
            setPostNotification(createNotification(context, getPostNotificationName()));
        }
        super.firePostNotification(context);
    }

    protected ServerNotification createNotification(MuleContext context, String action)
    {
        try
        {
            return (ServerNotification)ctor.newInstance(context, action);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate("Notification:" + action) ,e);
        }
    }
}