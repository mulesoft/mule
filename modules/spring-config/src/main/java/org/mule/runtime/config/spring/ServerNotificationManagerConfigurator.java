/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.context.notification.ServerNotificationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationContext;

public class ServerNotificationManagerConfigurator
    implements SmartFactoryBean
{

    @Inject
    private MuleContext muleContext;
    @Inject
    private ApplicationContext applicationContext;

    private Boolean dynamic;
    private List<Notification> enabledNotifications = new ArrayList<>();
    private List<Notification> disabledNotifications = new ArrayList<>();
    private Collection<ListenerSubscriptionPair> notificationListeners;

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
        enableNotifications(notificationManager);
        disableNotifications(notificationManager);

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
            else
            {
                notificationManager.removeListener(sub.getListener());
                notificationManager.addListenerSubscriptionPair(sub);
            }
        }
        return notificationManager;
    }

    private void disableNotifications(ServerNotificationManager notificationManager)
    {
        for (Notification disabledNotification : disabledNotifications)
        {
            BiConsumer<DisableNotificationTask, Class> disableNotificationFunction = (disableFunction, type) -> {
                try
                {
                    disableFunction.run();
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(createStaticMessage(format("Fail trying to disable a notification of type %s since such type does not exists", type)), e);
                }
            };
            if (disabledNotification.isInterfaceExplicitlyConfigured())
            {
                disableNotificationFunction.accept(() -> {
                    notificationManager.disableInterface(disabledNotification.getInterfaceClass().get());
                }, disabledNotification.getInterfaceClass().get());
            }
            if (disabledNotification.isEventExplicitlyConfigured())
            {
                disableNotificationFunction.accept(() -> {
                    notificationManager.disableType(disabledNotification.getEventClass().get());
                }, disabledNotification.getEventClass().get());
            }
        }
    }



    private void enableNotifications(ServerNotificationManager notificationManager)
    {
        for (Notification notification : enabledNotifications)
        {
            notificationManager.addInterfaceToType(notification.getInterfaceClass().get(), notification.getEventClass().get());
        }
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

        if (notificationListeners != null)
        {
            mergedListeners.addAll(notificationListeners);

            for (ListenerSubscriptionPair candidate : adhocListeners)
            {
                boolean explicityDefined = false;
                for (ListenerSubscriptionPair explicitListener : notificationListeners)
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

    public void setEnabledNotifications(List<Notification> enabledNotifications)
    {
        this.enabledNotifications = enabledNotifications;
    }

    public void setNotificationListeners(Collection<ListenerSubscriptionPair> notificationListeners)
    {
        this.notificationListeners = notificationListeners;
    }

    public void setDisabledNotifications(List<Notification> disabledNotifications)
    {
        this.disabledNotifications = disabledNotifications;
    }

    interface DisableNotificationTask
    {
        void run() throws ClassNotFoundException;
    }
}
