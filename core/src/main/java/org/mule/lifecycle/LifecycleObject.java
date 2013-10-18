/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotification;
import org.mule.util.ClassUtils;

public class LifecycleObject
{
    
    private Class type;
    private ServerNotification preNotification;
    private ServerNotification postNotification;

    public LifecycleObject(Class type)
    {
        this.type = type;
    }

    public ServerNotification getPostNotification()
    {
        return postNotification;
    }

    public void setPostNotification(ServerNotification postNotification)
    {
        this.postNotification = postNotification;
    }

    public ServerNotification getPreNotification()
    {
        return preNotification;
    }

    public void setPreNotification(ServerNotification preNotification)
    {
        this.preNotification = preNotification;
    }

    public Class getType()
    {
        return type;
    }

    public void setType(Class type)
    {
        this.type = type;
    }

    public void firePreNotification(MuleContext context)
    {
        if(preNotification!=null)
        {
            context.fireNotification(preNotification);
        }
    }

    public void firePostNotification(MuleContext context)
    {
        if(postNotification!=null)
        {
            context.fireNotification(postNotification);
        }
    }

    @Override
    public String toString()
    {
        return super.toString() + " (" + ClassUtils.getSimpleName(type) + ")";
    }
        
}
