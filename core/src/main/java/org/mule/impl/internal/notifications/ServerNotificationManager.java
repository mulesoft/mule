/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
public class ServerNotificationManager implements Work, Disposable
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ServerNotificationManager.class);

    public static final String NULL_SUBSCRIPTION = "NULL";

    private Map eventsMap;
    private BlockingDeque eventQueue;
    private List listeners;
    private WorkListener workListener;
    private volatile boolean disposed = false;

    public ServerNotificationManager()
    {
        // listenersMap = new ConcurrentHashMap();
        eventsMap = new ConcurrentHashMap();
        eventQueue = new LinkedBlockingDeque();
        listeners = new CopyOnWriteArrayList();
        workListener = MuleManager.getConfiguration().getDefaultWorkListener();
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

    public void registerEventType(Class eventType, Class listenerType)
    {
        if (UMOServerNotification.class.isAssignableFrom(eventType))
        {
            if (!eventsMap.containsKey(listenerType))
            {
                // listenersMap.put(eventType, new TreeMap(comparator));
                eventsMap.put(listenerType, eventType);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Registered event type: " + eventType);
                    logger.debug("Binding listener type '" + listenerType + "' to event type '" + eventType
                                    + "'");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException(new Message(
                Messages.PROPERTY_X_IS_NOT_SUPPORTED_TYPE_X_IT_IS_TYPE_X, "eventType",
                UMOServerNotification.class.getName(), eventType.getName()).getMessage());
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

    public void unregisterListener(UMOServerNotificationListener listener)
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            Listener l = (Listener)iterator.next();
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
            notifyListeners(notification);
        }
        else
        {
            try
            {
                eventQueue.put(notification);
            }
            catch (InterruptedException e)
            {
                logger.error("Failed to queue notification: " + notification, e);
            }
        }
    }

    public void dispose()
    {
        disposed = true;
        // listenersMap.clearErrors();
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
            Listener listener = (Listener)iterator.next();
            if (listener.matches(notification))
            {
                listener.getListenerObject().onNotification(notification);
            }
        }
    }

    public void release()
    {
        dispose();
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
                UMOServerNotification notification = (UMOServerNotification)eventQueue.take();
                if (notification != null)
                {
                    notifyListeners(notification);
                }
            }
            catch (InterruptedException e)
            {
                if (!disposed)
                {
                    logger.error("Failed to take notificationication from server notificationication queue",
                        e);
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
                Class clazz = (Class)iterator.next();
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
                    Class notificationClass = (Class)iterator.next();
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
        return workListener;
    }

    public void setWorkListener(WorkListener workListener)
    {
        if (workListener == null)
        {
            throw new NullPointerException("workListener");
        }
        this.workListener = workListener;
    }
}
