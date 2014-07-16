/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.context.notification.ServerNotificationManager.toClass;

/**
 * This acts as a synchronized collection. No call blocks and all are synchronized.
 */
class Configuration
{

    protected static Log logger = LogFactory.getLog(Configuration.class);
    private Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToTypes =
            new HashMap<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>>(); // map from interface to collection of events
    private Set<ListenerSubscriptionPair> listenerSubscriptionPairs = new HashSet<ListenerSubscriptionPair>();
    private Set<Class<? extends ServerNotificationListener>> disabledInterfaces = new HashSet<Class<? extends ServerNotificationListener>>();
    private Set<Class<? extends ServerNotification>> disabledNotificationTypes = new HashSet<Class<? extends ServerNotification>>();
    private volatile boolean dirty = true;
    private Policy policy;

    synchronized void addInterfaceToType(Class<? extends ServerNotificationListener> iface, Class<? extends ServerNotification> type)
    {
        dirty = true;
        if (!ServerNotification.class.isAssignableFrom(type))
        {
            throw new IllegalArgumentException(
                    CoreMessages.propertyIsNotSupportedType("type",
                            ServerNotification.class, type).getMessage());
        }
        if (!interfaceToTypes.containsKey(iface))
        {
            interfaceToTypes.put(iface, new HashSet<Class<? extends ServerNotification>>());
        }
        Set<Class<? extends ServerNotification>> events = interfaceToTypes.get(iface);
        events.add(type);
        if (logger.isDebugEnabled())
        {
            logger.debug("Registered event type: " + type);
            logger.debug("Binding listener type '" + iface + "' to event type '" + type + "'");
        }
    }

    /**
     * @param interfaceToTypes map from interace to a particular event
     * @throws ClassNotFoundException if the interface is a key, but the corresponding class cannot be loaded
     */
    synchronized void addAllInterfaceToTypes(Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToTypes) throws ClassNotFoundException
    {
        dirty = true;

        for (Iterator ifaces = interfaceToTypes.keySet().iterator(); ifaces.hasNext();)
        {
            Object iface = ifaces.next();
            addInterfaceToType(toClass(iface), toClass(interfaceToTypes.get(iface)));
        }
    }

    synchronized void addListenerSubscriptionPair(ListenerSubscriptionPair pair)
    {
        dirty = true;
        if (!listenerSubscriptionPairs.add(pair))
        {
            logger.warn(CoreMessages.notificationListenerSubscriptionAlreadyRegistered(pair));
        }
    }

    synchronized void addAllListenerSubscriptionPairs(Collection pairs)
    {
        dirty = true;
        for (Iterator listener = pairs.iterator(); listener.hasNext();)
        {
            addListenerSubscriptionPair((ListenerSubscriptionPair) listener.next());
        }
    }

    synchronized void removeListener(ServerNotificationListener listener)
    {
        dirty = true;
        Set<ListenerSubscriptionPair> toRemove = new HashSet<ListenerSubscriptionPair>();
        for (Iterator listeners = listenerSubscriptionPairs.iterator(); listeners.hasNext();)
        {
            ListenerSubscriptionPair pair = (ListenerSubscriptionPair) listeners.next();
            if (pair.getListener().equals(listener))
            {
                toRemove.add(pair);
            }
        }
        listenerSubscriptionPairs.removeAll(toRemove);
    }

    synchronized void removeAllListeners(Collection listeners)
    {
        dirty = true;
        for (Iterator listener = listeners.iterator(); listener.hasNext();)
        {
            removeListener((ServerNotificationListener) listener.next());
        }
    }

    synchronized void disableInterface(Class<? extends ServerNotificationListener> iface)
    {
        dirty = true;
        disabledInterfaces.add(iface);
    }

    synchronized void disabledAllInterfaces(Collection<Class<? extends ServerNotificationListener>> interfaces) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator iface = interfaces.iterator(); iface.hasNext();)
        {
            disableInterface(toClass(iface.next()));
        }
    }

    synchronized void disableType(Class<? extends ServerNotification> type)
    {
        dirty = true;
        disabledNotificationTypes.add(type);
    }

    synchronized void disableAllTypes(Collection types) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator event = types.iterator(); event.hasNext();)
        {
            disableType(toClass(event.next()));
        }
    }

    protected Policy getPolicy()
    {
        if (dirty)
        {
            synchronized (this)
            {
                if (dirty)
                {
                    policy = new Policy(interfaceToTypes, listenerSubscriptionPairs, disabledInterfaces, disabledNotificationTypes);
                    dirty = false;
                }
            }
        }
        return policy;
    }

    // for tests -------------------------------

    Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> getInterfaceToTypes()
    {
        return Collections.unmodifiableMap(interfaceToTypes);
    }

    Set<ListenerSubscriptionPair> getListeners()
    {
        return Collections.unmodifiableSet(listenerSubscriptionPairs);
    }

}
