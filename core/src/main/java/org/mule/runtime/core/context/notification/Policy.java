/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * For a particular configuration, this describes what events should be delivered where.
 * It is read-only and a lazy instance is cached by the
 * {@link Configuration}
 */
class Policy
{

    // map from event to set of senders
    private Map<Class<? extends ServerNotification>, Collection<Sender>> eventToSenders = new HashMap<Class<? extends ServerNotification>, Collection<Sender>>();

    // these are cumulative - set values should never change, they are just a cache of known info
    // they are co and contra-variant wrt to exact event type (see code below).
    private ConcurrentMap knownEventsExact = new ConcurrentHashMap();
    private ConcurrentMap knownEventsSuper = new ConcurrentHashMap();

    /**
     * For each listener, we check each interface and see what events can be delivered.
     */
    Policy(Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> interfaceToEvents, 
        Set<ListenerSubscriptionPair> listenerSubscriptionPairs, 
        Set<Class<? extends ServerNotificationListener>> disabledInterfaces, 
        Set<Class<? extends ServerNotification>> disabledEvents)
    {
        for (ListenerSubscriptionPair pair : listenerSubscriptionPairs)
        {
            ServerNotificationListener listener = pair.getListener();
            for (Class<? extends ServerNotificationListener> iface : interfaceToEvents.keySet())
            {
                if (notASubclassOfAnyClassInSet(disabledInterfaces, iface))
                {
                    if (iface.isAssignableFrom(listener.getClass()))
                    {
                        Set<Class<? extends ServerNotification>> events = interfaceToEvents.get(iface);
                        for (Class<? extends ServerNotification> event : events)
                        {
                            if (notASubclassOfAnyClassInSet(disabledEvents, event))
                            {
                                knownEventsExact.put(event, Boolean.TRUE);
                                knownEventsSuper.put(event, Boolean.TRUE);
                                if (!eventToSenders.containsKey(event))
                                {
                                    // use a collection with predictable iteration order
                                    eventToSenders.put(event, new ArrayList<Sender>());
                                }
                                eventToSenders.get(event).add(new Sender(pair));
                            }
                        }
                    }
                }
            }
        }
    }

    protected static boolean notASubclassOfAnyClassInSet(Set set,  Class clazz)
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

    protected static boolean notASuperclassOfAnyClassInSet(Set set, Class clazz)
    {
        for (Iterator iterator = set.iterator(); iterator.hasNext();)
        {
            Class disabled = (Class) iterator.next();
            if (clazz.isAssignableFrom(disabled))
            {
                return false;
            }
        }
        return true;
    }

    void dispatch(ServerNotification notification)
    {
        if (null != notification)
        {
            Class notfnClass = notification.getClass();
            // search if we don't know about this event, or if we do know it is used
            if (!knownEventsExact.containsKey(notfnClass))
            {
                boolean found = doDispatch(notification, notfnClass);
                knownEventsExact.put(notfnClass, Boolean.valueOf(found));
            }
            else if (((Boolean) knownEventsExact.get(notfnClass)).booleanValue())
            {
                boolean found = doDispatch(notification, notfnClass);
                // reduce contention on the map by not writing the same value over and over again.
                if (!found)
                {
                    knownEventsExact.put(notfnClass, Boolean.valueOf(found));
                }
            }
        }
    }

    protected boolean doDispatch(ServerNotification notification, Class<? extends ServerNotification> notfnClass)
    {
        boolean found = false;
        for (Class<? extends ServerNotification> event : eventToSenders.keySet())
        {
            if (event.isAssignableFrom(notfnClass))
            {
                found = true;
                for (Sender sender : eventToSenders.get(event))
                {
                    sender.dispatch(notification);
                }
            }
        }
        return found;
    }

    /**
     * This returns a very "conservative" value - it is true if the notification or any subclass would be
     * accepted.  So if it returns false then you can be sure that there is no need to send the
     * notification.  On the other hand, if it returns true there is no guarantee that the notification
     * "really" will be dispatched to any listener.
     *
     * @param notfnClass Either the notification class being generated or some superclass
     * @return false if there is no need to dispatch the notification
     */
    boolean isNotificationEnabled(Class notfnClass)
    {
        if (!knownEventsSuper.containsKey(notfnClass))
        {
            boolean found = false;
            // this is exhaustive because we initialise to include all events handled.
            for (Iterator events = knownEventsSuper.keySet().iterator(); events.hasNext() && !found;)
            {
                Class event = (Class) events.next();
                found = ((Boolean) knownEventsSuper.get(event)).booleanValue() && notfnClass.isAssignableFrom(event);
            }
            knownEventsSuper.put(notfnClass, Boolean.valueOf(found));
        }
        if (!knownEventsExact.containsKey(notfnClass))
        {
            boolean found = false;
            for (Iterator events = eventToSenders.keySet().iterator(); events.hasNext() && !found;)
            {
                Class event = (Class) events.next();
                found = event.isAssignableFrom(notfnClass);
            }
            knownEventsExact.put(notfnClass, Boolean.valueOf(found));

        }
        return ((Boolean) knownEventsSuper.get(notfnClass)).booleanValue()
                || ((Boolean) knownEventsExact.get(notfnClass)).booleanValue();
    }

}
