/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
