/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.ClassUtils;
import org.mule.util.concurrent.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manage all server listeners for a Mule instance.
 */
public class ServerNotificationManager implements Work, Disposable
{

    protected static final Log logger = LogFactory.getLog(ServerNotificationManager.class);

    public static final String NULL_SUBSCRIPTION = "NULL";

    /**
     * This defines which event is associated with a particular listener interface.
     * Only a single event is associated with any particular listener interface,
     * and it is the first type registers (subsequent registrations are discarded)
     */
    private ConcurrentMap eventsMap;

    /**
     * This is used to allow asynchronous dispatching of events
     */
    private BlockingDeque eventQueue;

    /**
     * This is the set of listeners that may receive an event.
     * When a listener is registered {@link #eventsMap} is checked to see what events it will receive
     * (it can receive more than one if it implements more than one interface).
     */
    private Set listeners;

    /**
     * This is a doohickey needed by the work manager which seems to be what manages the thread
     * that does the asynchronous notifications.
     */
    private WorkListener workListener;
    
    private volatile boolean disposed = false;

    public ServerNotificationManager()
    {
        eventsMap = new ConcurrentHashMap();
        eventQueue = new LinkedBlockingDeque();
        listeners = new ConcurrentHashSet();
    }

    public void start(UMOWorkManager workManager) throws LifecycleException
    {
        try
        {
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, workListener);
        }
        catch (WorkException e)
        {
            throw new LifecycleException(e, this);
        }
    }

    // this appends, since we configure a single instance multiple times
    public void setEventTypes(Map eventTypes) throws ClassNotFoundException
    {
        for (Iterator iterator = eventTypes.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            registerEventType(getClassFromValue(entry.getKey()), getClassFromValue(entry.getValue()));
        }
    }

    private Class getClassFromValue(Object value) throws ClassNotFoundException
    {
        Class theClass;
        if(value instanceof String)
        {
            theClass = ClassUtils.loadClass(value.toString(), getClass());
        }
        else if(value instanceof Class)
        {
            theClass = (Class)value;
        }
        else
        {
           throw new IllegalArgumentException("Notification types and listeners must be a Class of fully qualified class name. Value is: " + value); 
        }
        return theClass;
    }

    public Map getEventTypes()
    {
        return Collections.unmodifiableMap(eventsMap);
    }

    public void registerEventType(Class listenerType, Class eventType)
    {
        if (UMOServerNotification.class.isAssignableFrom(eventType))
        {
            if (null == eventsMap.putIfAbsent(listenerType, eventType))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Registered event type: " + eventType);
                    logger.debug("Binding listener type '" + listenerType + "' to event type '" + eventType + "'");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException(
                    CoreMessages.propertyIsNotSupportedType("eventType",
                            UMOServerNotification.class, eventType).getMessage());
        }
    }

    public void registerListener(UMOServerNotificationListener listener) throws NotificationException
    {
        registerListener(listener, null);
    }

    public void registerListener(UMOServerNotificationListener listener, String subscription)
        throws NotificationException
    {
        listeners.add(new Listener(listener, subscription));
    }

    public void setListeners(Collection listeners) throws NotificationException
    {
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            registerListener((UMOServerNotificationListener) iterator.next());
        }
    }

    public Collection getListeners()
    {
        return Collections.unmodifiableCollection(listeners);
    }

    public void unregisterListener(UMOServerNotificationListener listener)
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            Listener l = (Listener) iterator.next();
            if (l.getListenerObject().equals(listener))
            {
                listeners.remove(l);
                break;
            }
        }
    }

    public void fireEvent(UMOServerNotification notification)
    {
        if (disposed)
        {
            return;
        }

        if (notification instanceof BlockingServerEvent)
        {
            this.notifyListeners(notification);
        }
        else
        {
            try
            {
                eventQueue.put(notification);
            }
            catch (InterruptedException e)
            {
                if (!disposed)
                {
                    // TODO MULE-863: Is this sufficient?  Necessary?
                    logger.error("Failed to queue notification: " + notification, e);
                }
            }
        }
    }

    public void dispose()
    {
        disposed = true;
        eventsMap.clear();
        eventQueue.clear();
        listeners.clear();
        workListener = null;
    }

    /**
     * Exceptions should not be thrown from this method
     * 
     * @param notification
     */
    protected void notifyListeners(UMOServerNotification notification)
    {
        if (disposed)
        {
            return;
        }

        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            Listener listener = (Listener) iterator.next();
            if (listener.matches(notification))
            {
                listener.getListenerObject().onNotification(notification);
            }
        }
    }

    public void release()
    {
        this.dispose();
    }

    public void run()
    {
        while (!disposed)
        {
            try
            {
                UMOServerNotification notification = (UMOServerNotification) eventQueue.take();
                if (notification != null)
                {
                    this.notifyListeners(notification);
                }
            }
            catch (InterruptedException e)
            {
                if (!disposed)
                {
                    // TODO MULE-863: Is this sufficient?  Necessary? 
                    logger.error("Failed to take notification from queue", e);
                }
            }
        }
    }

    protected class Listener
    {

        private final UMOServerNotificationListener listener;
        private final List notificationClasses;
        private final String subscription;
        private final WildcardFilter subscriptionFilter;

        public Listener(UMOServerNotificationListener listener, String subscription)
        {
            this.listener = listener;
            this.subscription = (subscription == null ? NULL_SUBSCRIPTION : subscription);

            subscriptionFilter = new WildcardFilter(this.subscription);
            subscriptionFilter.setCaseSensitive(false);

            notificationClasses = new ArrayList();

            for (Iterator iterator = eventsMap.keySet().iterator(); iterator.hasNext();)
            {
                Class clazz = (Class) iterator.next();
                if (clazz.isAssignableFrom(listener.getClass()))
                {
                    notificationClasses.add(eventsMap.get(clazz));
                }
            }
        }

        public UMOServerNotificationListener getListenerObject()
        {
            return listener;
        }

        public List getNotificationClasses()
        {
            return notificationClasses;
        }

        public String getSubscription()
        {
            return subscription;
        }

        public boolean matches(UMOServerNotification notification)
        {
            if (subscriptionMatches(notification))
            {
                for (Iterator iterator = notificationClasses.iterator(); iterator.hasNext();)
                {
                    Class notificationClass = (Class) iterator.next();
                    if (notificationClass.isAssignableFrom(notification.getClass()))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean subscriptionMatches(UMOServerNotification notification)
        {
            String resourceId = notification.getResourceIdentifier();
            return NULL_SUBSCRIPTION.equals(subscription) || subscriptionFilter.accept(resourceId);
        }
    }

    public WorkListener getWorkListener()
    {
        if (workListener == null)
        {
            MuleConfiguration config = RegistryContext.getConfiguration();
            if (config != null)
            {
                workListener = config.getDefaultWorkListener();
            }
        }
        return workListener;
    }

    public void setWorkListener(WorkListener workListener)
    {
        if (workListener == null)
        {
            throw new IllegalArgumentException("workListener may not be null");
        }
        this.workListener = workListener;
    }

}
