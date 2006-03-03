/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.notifications;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>ServerNotificationManager</code> manages all server listeners for a Mule
 * Instance
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ServerNotificationManager implements Work, Disposable
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ServerNotificationManager.class);

    public static final String NULL_SUBSCRIPTION = "NULL";

    private Map eventsMap = null;
    private LinkedBlockingQueue eventQueue;
    private boolean disposed = false;
    private List listeners;


    public ServerNotificationManager()
    {
        init();
    }

    private synchronized void init()
    {
        //listenersMap = new ConcurrentHashMap();
        eventsMap = new ConcurrentHashMap();
        eventQueue = new LinkedBlockingQueue();
        listeners = new CopyOnWriteArrayList();
    }

    public void start(UMOWorkManager workManager) throws LifecycleException {
        try {
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, null);
        } catch (WorkException e) {
            throw new LifecycleException(e, this);
        }
    }

    public void registerEventType(Class eventType, Class listenerType)
    {
        if (UMOServerNotification.class.isAssignableFrom(eventType)) {
            if (!eventsMap.containsKey(listenerType)) {
                //listenersMap.put(eventType, new TreeMap(comparator));
                eventsMap.put(listenerType, eventType);
                if (logger.isDebugEnabled()) {
                    logger.debug("Registered event type: " + eventType);
                    logger.debug("Binding listener type '" + listenerType + "' to event type '" + eventType + "'");
                }
            }
        } else {
            throw new IllegalArgumentException(new Message(Messages.PROPERTY_X_IS_NOT_SUPPORTED_TYPE_X_IT_IS_TYPE_X,
                                                           "eventType",
                                                           UMOServerNotification.class.getName(),
                                                           eventType.getName()).getMessage());
        }
    }

    public void registerListener(UMOServerNotificationListener listener) throws NotificationException {
        registerListener(listener, null);
    }

    public void registerListener(UMOServerNotificationListener listener, String subscription) throws NotificationException
    {
        listeners.add(new Listener(listener, subscription));
    }

    public void unregisterListener(UMOServerNotificationListener listener)
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            Listener l = (Listener) iterator.next();
            if(l.equals(listener)) {
                listeners.remove(l);
                break;
            }
        }
    }

    public void clear()
    {
        listeners.clear();
        init();
    }

    public void fireEvent(UMOServerNotification notification)
    {
        if (disposed) {
            return;
        }
        
        if (notification instanceof BlockingServerEvent) {
            notifyListeners(notification);
            return;
        }
        try {

            eventQueue.put(notification);

        } catch (InterruptedException e) {
            logger.error("Failed to queue notification: " + notification, e);
        }
    }

    public void dispose()
    {
        disposed = true;
        clear();
    }

    /**
     * Exceptions should not be thrown from this method
     * @param notification
     */
    protected void notifyListeners(UMOServerNotification notification)
    {
        if(disposed) {
            return;
        }
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            Listener listener = (Listener) iterator.next();
            if(listener.matches(notification)) {
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
		UMOServerNotification notification = null;
		while (!disposed)
		{
			try
			{
				notification = (UMOServerNotification)eventQueue.take();
				if (notification != null)
				{
					notifyListeners(notification);
				}
			}
			catch (InterruptedException e)
			{
				if (!disposed)
				{
					logger.error("Failed to take notificationication from server notificationication queue", e);
				}
			}
		}
	}

    protected class Listener {

        private UMOServerNotificationListener listener;
        private List notificationClasses;
        private String subscription;
        private WildcardFilter subscriptionFilter;

        public Listener(UMOServerNotificationListener listener, String subscription) {
            this.listener = listener;
            this.subscription = (subscription==null ? NULL_SUBSCRIPTION : subscription);

            subscriptionFilter = new WildcardFilter(this.subscription);
            subscriptionFilter.setCaseSensitive(false);

            notificationClasses = new ArrayList();

            for (Iterator iterator = eventsMap.keySet().iterator(); iterator.hasNext();)
            {
                Class clazz = (Class) iterator.next();
                if (clazz.isAssignableFrom(listener.getClass())) {
                    notificationClasses.add(eventsMap.get(clazz));
                }
            }
        }

        public UMOServerNotificationListener getListenerObject() {
            return listener;
        }

        public List getNotificationClasses() {
            return notificationClasses;
        }

        public String getSubscription() {
            return subscription;
        }

        public boolean matches(UMOServerNotification notification) {
            if(subscriptionMatches(notification)) {
                for (Iterator iterator = notificationClasses.iterator(); iterator.hasNext();) {
                    Class notificationClass = (Class) iterator.next();

                    if (notificationClass.isAssignableFrom(notification.getClass())) {
                        return true;
                    }
                }
            } else {
//                if(logger.isTraceEnabled()) {
//                        logger.trace("Resource id '" + subscription + "' for listener " + l.getClass().getName()
//                            + " does not match Resource id '" + notification.getResourceIdentifier()
//                            + "' for notificationication, not firing notificationication for this listener. Listener " + i + " of " + listeners.size());
//                    }
            }
            return false;
        }

        public boolean subscriptionMatches(UMOServerNotification notification)
        {
            String resourceId = notification.getResourceIdentifier();
            if (NULL_SUBSCRIPTION.equals(subscription) || subscriptionFilter.accept(resourceId)) {
                return true;
            } else {
                return false;
            }
        }
    }
}
