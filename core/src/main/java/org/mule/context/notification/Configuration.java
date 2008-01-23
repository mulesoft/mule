/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

/**
 * This acts as a synchronized collection.  No call blocks and all are synchronized.
 */
class Configuration
{

    protected Log logger = LogFactory.getLog(getClass());
    private Map interfaceToTypes = new HashMap(); // map from interface to collection of events
    private Set listenerSubscriptionPairs = new HashSet();
    private Set disabledInterfaces = new HashSet();
    private Set disabledNotificationTypes = new HashSet();
    private boolean dirty = true;
    private Policy policy;

    synchronized void addInterfaceToType(Class iface, Class type)
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
            interfaceToTypes.put(iface, new HashSet());
        }
        Set events = (Set) interfaceToTypes.get(iface);
        events.add(type);
        if (logger.isDebugEnabled())
        {
            logger.debug("Registered event type: " + type);
            logger.debug("Binding listener type '" + iface + "' to event type '" + type + "'");
        }
    }

    /**
     * @param interfaceToTypes map from interace to a particular event
     * @throws ClassNotFoundException
     */
    synchronized void addAllInterfaceToTypes(Map interfaceToTypes) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator ifaces = interfaceToTypes.keySet().iterator(); ifaces.hasNext();)
        {
            Object iface = ifaces.next();
            addInterfaceToType(ServerNotificationManager.toClass(iface), ServerNotificationManager.toClass(interfaceToTypes.get(iface)));
        }
    }

    synchronized void addListenerSubscriptionPair(ListenerSubscriptionPair pair)
    {
        dirty = true;
        listenerSubscriptionPairs.add(pair);
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
        Set toRemove = new HashSet();
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

    synchronized void disableInterface(Class iface)
    {
        dirty = true;
        disabledInterfaces.add(iface);
    }

    synchronized void disabledAllInterfaces(Collection interfaces) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator iface = interfaces.iterator(); iface.hasNext();)
        {
            disableInterface(ServerNotificationManager.toClass(iface.next()));
        }
    }

    synchronized void disableType(Class type)
    {
        dirty = true;
        disabledNotificationTypes.add(type);
    }

    synchronized void disableAllTypes(Collection types) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator event = types.iterator(); event.hasNext();)
        {
            disableType(ServerNotificationManager.toClass(event.next()));
        }
    }

    synchronized Policy getPolicy()
    {
        if (dirty)
        {
            policy = new Policy(interfaceToTypes, listenerSubscriptionPairs, disabledInterfaces, disabledNotificationTypes);
            dirty = false;
        }
        return policy;
    }

    // for tests -------------------------------

    Map getInterfaceToTypes()
    {
        return Collections.unmodifiableMap(interfaceToTypes);
    }

    Collection getListeners()
    {
        return Collections.unmodifiableCollection(listenerSubscriptionPairs);
    }

}
