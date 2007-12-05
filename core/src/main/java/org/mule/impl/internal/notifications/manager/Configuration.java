/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;

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
    private Map interfaceToEvents = new HashMap(); // map from interface to collection of events
    private Set listenerSubscriptionPairs = new HashSet();
    private Set disabledInterfaces = new HashSet();
    private Set disabledEvents = new HashSet();
    private boolean dirty = true;
    private Policy policy;

    synchronized void addInterfaceToEvent(Class iface, Class event)
    {
        dirty = true;
        if (!UMOServerNotification.class.isAssignableFrom(event))
        {
            throw new IllegalArgumentException(
                    CoreMessages.propertyIsNotSupportedType("event",
                            UMOServerNotification.class, event).getMessage());
        }
        if (!interfaceToEvents.containsKey(iface))
        {
            interfaceToEvents.put(iface, new HashSet());
        }
        Set events = (Set) interfaceToEvents.get(iface);
        events.add(event);
        if (logger.isDebugEnabled())
        {
            logger.debug("Registered event type: " + event);
            logger.debug("Binding listener type '" + iface + "' to event type '" + event + "'");
        }
    }

    /**
     * @param interfaceToEvents map from interace to a particular event
     * @throws ClassNotFoundException
     */
    synchronized void addAllInterfaceToEvents(Map interfaceToEvents) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator ifaces = interfaceToEvents.keySet().iterator(); ifaces.hasNext();)
        {
            Object iface = ifaces.next();
            addInterfaceToEvent(ServerNotificationManager.toClass(iface), ServerNotificationManager.toClass(interfaceToEvents.get(iface)));
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

    /**
     * We only remove one listener, event though several may be registered
     * - this is historical behaviour, tested elsewhere.
     */
    synchronized void removeListener(UMOServerNotificationListener listener)
    {
        dirty = true;
        Set toRemove = new HashSet();
        for (Iterator listeners = listenerSubscriptionPairs.iterator(); listeners.hasNext();)
        {
            ListenerSubscriptionPair pair = (ListenerSubscriptionPair) listeners.next();
            if (pair.getListener().equals(listener))
            {
                toRemove.add(pair);
                break; // see above - remove just one listener
            }
        }
        listenerSubscriptionPairs.removeAll(toRemove);
    }

    synchronized void removeAllListeners(Collection listeners)
    {
        dirty = true;
        for (Iterator listener = listeners.iterator(); listener.hasNext();)
        {
            removeListener((UMOServerNotificationListener) listener.next());
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

    synchronized void disableEvent(Class event)
    {
        dirty = true;
        disabledEvents.add(event);
    }

    synchronized void disableAllEvents(Collection events) throws ClassNotFoundException
    {
        dirty = true;
        for (Iterator event = events.iterator(); event.hasNext();)
        {
            disableEvent(ServerNotificationManager.toClass(event.next()));
        }
    }

    synchronized Policy getPolicy()
    {
        if (dirty)
        {
            policy = new Policy(interfaceToEvents, listenerSubscriptionPairs, disabledInterfaces, disabledEvents);
            dirty = false;
        }
        return policy;
    }

    // for tests -------------------------------

    Map getInterfaceToEvents()
    {
        return Collections.unmodifiableMap(interfaceToEvents);
    }

    Collection getListeners()
    {
        return Collections.unmodifiableCollection(listenerSubscriptionPairs);
    }

}
