
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.context.notification.ServerNotificationManager;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.SmartFactoryBean;

public class ServerNotificationManagerConfigurator implements MuleContextAware, SmartFactoryBean
{

    private MuleContext muleContext;

    private Boolean dynamic;
    private Map interfaceToEvents;
    private Collection interfaces;
    private Collection pairs;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public Object getObject() throws Exception
    {
        ServerNotificationManager notificationManager = muleContext.getNotificationManager();
        if (dynamic != null)
        {
            notificationManager.setNotificationDynamic(dynamic.booleanValue());
        }
        if (interfaceToEvents != null)
        {
            notificationManager.setInterfaceToTypes(interfaceToEvents);
        }
        if (interfaces != null)
        {
            notificationManager.setDisabledInterfaces(interfaces);
        }
        if (pairs != null)
        {
            notificationManager.setAllListenerSubscriptionPairs(pairs);
        }
        return notificationManager;
    }

    public Class getObjectType()
    {
        return ServerNotificationManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setNotificationDynamic(boolean dynamic)
    {
        this.dynamic = new Boolean(dynamic);
    }

    public void setInterfaceToTypes(Map interfaceToEvents) throws ClassNotFoundException
    {
        this.interfaceToEvents = interfaceToEvents;
    }

    public void setAllListenerSubscriptionPairs(Collection pairs)
    {
        this.pairs = pairs;
    }

    public void setDisabledInterfaces(Collection interfaces) throws ClassNotFoundException
    {
        this.interfaces = interfaces;
    }

    public boolean isEagerInit()
    {
        return true;
    }

    public boolean isPrototype()
    {
        return false;
    }

}
