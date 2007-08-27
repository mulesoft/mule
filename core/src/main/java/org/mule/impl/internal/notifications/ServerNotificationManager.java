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
import org.mule.impl.ManagementContextAware;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.ClassUtils;
import org.mule.util.concurrent.ConcurrentHashSet;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ServerNotificationManager</code> manages all server listeners for a Mule
 * instance.
 */
public class ServerNotificationManager implements Work, Disposable, ManagementContextAware
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ServerNotificationManager.class);

    public static final String NULL_SUBSCRIPTION = "NULL";

    private ConcurrentMap eventsMap;
    private BlockingDeque eventQueue;
    private Set listeners;
    private WorkListener workListener;
    private volatile boolean disposed = false;
    private UMOManagementContext managementContext;

    public ServerNotificationManager()
    {
        eventsMap = new ConcurrentHashMap();
        eventQueue = new LinkedBlockingDeque();
        listeners = new ConcurrentHashSet();
    }


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
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

    public void setEventTypes(Map eventTypes) throws ClassNotFoundException
    {
        eventsMap = new ConcurrentHashMap(eventTypes.size());

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
            Class previousEventType = (Class) eventsMap.putIfAbsent(listenerType, eventType);
            if (previousEventType != null)
            {
                eventType = previousEventType;
            }
            else
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
        this.registerListener(listener, null);
    }

    public void registerListener(UMOServerNotificationListener listener, String subscription)
        throws NotificationException
    {
        listeners.add(new Listener(listener, subscription));
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

    /**
     * When an object implementing interface <code>Runnable</code> is used to
     * create a thread, starting the thread causes the object's <code>run</code>
     * method to be called in that separately executing thread. <p/> The general
     * contract of the method <code>run</code> is that it may take any action
     * whatsoever.
     * 
     * @see Thread#run()
     */
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
            if (this.subscriptionMatches(notification))
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
            if (NULL_SUBSCRIPTION.equals(subscription) || subscriptionFilter.accept(resourceId))
            {
                return true;
            }
            else
            {
                return false;
            }
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
