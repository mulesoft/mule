
package org.mule.config.spring;

import org.mule.impl.ManagementContextAware;
import org.mule.impl.internal.notifications.manager.ServerNotificationManager;
import org.mule.umo.UMOManagementContext;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.SmartFactoryBean;

public class ServerNotificationManagerConfigurator implements ManagementContextAware, SmartFactoryBean
{

    private UMOManagementContext managementContext;

    private Boolean dynamic;
    private Map interfaceToEvents;
    private Collection interfaces;
    private Collection pairs;

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public Object getObject() throws Exception
    {
        ServerNotificationManager notificationManager = managementContext.getNotificationManager();
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
