/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.context.notification.ListenerSubscriptionPair;
import org.mule.context.notification.ServerNotificationManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ServerNotificationManagerConfigurator
    implements MuleContextAware, SmartFactoryBean, ApplicationContextAware
{

    private MuleContext muleContext;
    private ApplicationContext applicationContext;

    private Boolean dynamic;
    private Map interfaceToEvents;
    private Collection interfaces;
    private Collection<ListenerSubscriptionPair> pairs;

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

        // Merge:
        // i) explicitly configured notification listeners,
        // ii) any singleton beans defined in spring that implement ServerNotificationListener.
        Set<ListenerSubscriptionPair> subs = getMergedListeners(notificationManager);
        for (ListenerSubscriptionPair sub : subs)
        {
            // Do this to avoid warnings when the Spring context is refreshed
            if(!notificationManager.isListenerRegistered(sub.getListener()))
            {
                notificationManager.addListenerSubscriptionPair(sub);
            }
        }
        return notificationManager;
    }

    protected Set<ListenerSubscriptionPair> getMergedListeners(ServerNotificationManager notificationManager)
    {
        Set<ListenerSubscriptionPair> mergedListeners = new HashSet<ListenerSubscriptionPair>();

        // Any singleton bean defined in spring that implements
        // ServerNotificationListener or a subclass.
        String[] listenerBeans = applicationContext.getBeanNamesForType(ServerNotificationListener.class,
            false, true);
        Set<ListenerSubscriptionPair> adhocListeners = new HashSet<ListenerSubscriptionPair>();
        for (String name : listenerBeans)
        {
            adhocListeners.add(new ListenerSubscriptionPair(
                (ServerNotificationListener<?>) applicationContext.getBean(name), null));
        }

        if (pairs != null)
        {
            mergedListeners.addAll(pairs);

            for (ListenerSubscriptionPair candidate : adhocListeners)
            {
                boolean explicityDefined = false;
                for (ListenerSubscriptionPair explicitListener : pairs)
                {
                    if (candidate.getListener().equals(explicitListener.getListener()))
                    {
                        explicityDefined = true;
                        break;
                    }
                }
                if (!explicityDefined)
                {
                    mergedListeners.add(candidate);
                }
            }
        }
        else
        {
            mergedListeners.addAll(adhocListeners);
        }

        return mergedListeners;
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;

    }

}
