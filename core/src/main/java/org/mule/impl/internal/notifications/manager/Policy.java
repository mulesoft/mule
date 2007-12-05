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

import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

/**
 * For a particular configuration, this describes what events should be delivered where.
 * It is read-only and a lazy instance is cached by the
 * {@link Configuration}
 */
class Policy
{

    // map from event to set of senders
    private Map eventToSenders = new HashMap();
    // this is cumulative - it should never change, it's just a cache of known info
    private ConcurrentMap knownEvents = new ConcurrentHashMap();

    /**
     * For each listener, we check each interface and see what events can be delivered.
     *
     * @param interfaceToEvents
     * @param listenerSubscriptionPairs
     * @param disabledInterfaces
     * @param disabledEvents
     */
    Policy(Map interfaceToEvents, Set listenerSubscriptionPairs, Set disabledInterfaces, Set disabledEvents)
    {
        for (Iterator pairs = listenerSubscriptionPairs.iterator(); pairs.hasNext();)
        {
            ListenerSubscriptionPair pair = (ListenerSubscriptionPair) pairs.next();
            UMOServerNotificationListener listener = pair.getListener();
            for (Iterator interfaces = interfaceToEvents.keySet().iterator(); interfaces.hasNext();)
            {
                Class iface = (Class) interfaces.next();
                if (notASubclassOfAnyClassInSet(disabledInterfaces, iface))
                {
                    if (iface.isAssignableFrom(listener.getClass()))
                    {
                        for (Iterator events = ((Collection) interfaceToEvents.get(iface)).iterator(); events.hasNext();)
                        {
                            Class event = (Class) events.next();
                            if (notASubclassOfAnyClassInSet(disabledEvents, event))
                            {
                                if (!eventToSenders.containsKey(event))
                                {
                                    eventToSenders.put(event, new HashSet());
                                }
                                ((Collection) eventToSenders.get(event)).add(new Sender(pair));
                            }
                        }
                    }
                }
            }
        }
    }

    protected static boolean notASubclassOfAnyClassInSet(Set set, Class clazz)
    {
        for (Iterator iterator = set.iterator(); iterator.hasNext();)
        {
            Class disabled = (Class) iterator.next();
            if (disabled.isAssignableFrom(clazz))
            {
                return false;
            }
        }
        return true;
    }

    void dispatch(UMOServerNotification notification)
    {
        if (null != notification)
        {
            boolean dispatched = false;
            Class clazz = notification.getClass();
            if (!knownEvents.containsKey(clazz) || ((Boolean) knownEvents.get(clazz)).booleanValue())
            {
                for (Iterator events = eventToSenders.keySet().iterator(); events.hasNext();)
                {
                    Class event = (Class) events.next();
                    if (event.isAssignableFrom(clazz))
                    {
                        for (Iterator senders = ((Collection) eventToSenders.get(event)).iterator(); senders.hasNext();)
                        {
                            ((Sender) senders.next()).dispatch(notification);
                        }
                        dispatched = true;
                    }
                }
                knownEvents.put(clazz, Boolean.valueOf(dispatched));
            }
        }
    }

    boolean isEventEnabled(Class clazz)
    {
        if (knownEvents.containsKey(clazz))
        {
            return ((Boolean) knownEvents.get(clazz)).booleanValue();
        }
        else
        {
            for (Iterator events = eventToSenders.keySet().iterator(); events.hasNext();)
            {
                Class event = (Class) events.next();
                if (event.isAssignableFrom(clazz))
                {
                    knownEvents.put(clazz, Boolean.TRUE);
                    return true;
                }
            }
            knownEvents.put(clazz, Boolean.FALSE);
            return false;
        }
    }

}
