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
package org.mule.impl.internal.events;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.routing.filters.WildcardFilter;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.manager.UMOServerEventListener;
import org.mule.umo.manager.UMOWorkManager;

/**
 * <code>ServerEventManager</code> manages all server listeners for a Mule
 * Instance
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ServerEventManager implements Work, Disposable
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ServerEventManager.class);

    public static final String NULL_SUBSCRIPTION = "NULL";

    private Map listenersMap = null;
    private Map eventsMap = null;
    private BlockingQueue eventQueue;
    private boolean disposed = false;

    private Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2)
        {
            return (o1.equals(o2) ? 0 : 1);
        }
    };

    public ServerEventManager(UMOWorkManager workManager)
    {
        init();
        try {
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, null);
        } catch (WorkException e) {
            throw new MuleRuntimeException(new Message(Messages.FAILED_TO_SCHEDULE_WORK), e);
        }
    }

    private synchronized void init()
    {
        listenersMap = new ConcurrentHashMap();
        eventsMap = new ConcurrentHashMap();
        eventQueue = new ArrayBlockingQueue(1000);
    }

    public void registerEventType(Class eventType, Class listenerType)
    {
        if (UMOServerEvent.class.isAssignableFrom(eventType)) {
            if (!listenersMap.containsKey(eventType)) {
                listenersMap.put(eventType, new TreeMap(comparator));
                eventsMap.put(listenerType, eventType);
                if (logger.isDebugEnabled()) {
                    logger.debug("Registered event type: " + eventType);
                    logger.debug("Binding listener type '" + listenerType + "' to event type '" + eventType + "'");
                }
            }
        } else {
            throw new IllegalArgumentException(new Message(Messages.PROPERTY_X_IS_NOT_SUPPORTED_TYPE_X_IT_IS_TYPE_X,
                                                           "eventType",
                                                           UMOServerEvent.class.getName(),
                                                           eventType.getName()).getMessage());
        }
    }

    public void registerListener(UMOServerEventListener listener)
    {
        registerListener(listener, null);
    }

    public void registerListener(UMOServerEventListener listener, String subscription)
    {
        if (subscription == null) {
            subscription = NULL_SUBSCRIPTION;
        }
        TreeMap listeners = getListeners(listener.getClass());
        synchronized (listeners) {
            listeners.put(listener, subscription);
        }
    }

    public void unregisterListener(UMOServerEventListener listener)
    {
        TreeMap listeners = getListeners(listener.getClass());
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void clearListeners(Class listenerClass)
    {
        if (listenerClass == null) {
            return;
        }
        TreeMap listeners = getListeners(listenerClass);
        synchronized (listeners) {
            listeners.clear();
        }
    }

    public void clear()
    {
        for (Iterator iterator = listenersMap.values().iterator(); iterator.hasNext();) {
            TreeMap set = (TreeMap) iterator.next();
            synchronized (set) {
                set.clear();
            }
        }
        listenersMap.clear();
        init();
    }

    protected TreeMap getListeners(Class listenerClass)
    {
        if (listenerClass == null) {
            throw new NullPointerException("Listener class cannot be null");
        }
        Class eventType = null;
        for (Iterator iterator = eventsMap.keySet().iterator(); iterator.hasNext();) {
            Class clazz = (Class) iterator.next();
            if (clazz.isAssignableFrom(listenerClass)) {
                eventType = (Class) eventsMap.get(clazz);
                break;
            }
        }

        if (eventType != null) {
            return (TreeMap) listenersMap.get(eventType);
        } else {
            throw new IllegalArgumentException(new Message(Messages.PROPERTY_X_IS_NOT_SUPPORTED_TYPE_X_IT_IS_TYPE_X,
                                                           "Listener Type",
                                                           "Registered Type",
                                                           listenerClass.getName()).getMessage());
        }
    }

    public void fireEvent(UMOServerEvent event)
    {
        if (disposed) {
            return;
        }
        if (event instanceof BlockingServerEvent) {
            notifyListeners(event);
            return;
        }
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            logger.error("Failed to queue event: " + event, e);
        }
    }

    public void dispose()
    {
        disposed = true;
        clear();
    }

    protected void notifyListeners(UMOServerEvent event)
    {
        TreeMap listeners;
        String subscription = null;
        Class listenerClass = null;

        // determine the listewner class type for the current event
        Map.Entry entry = null;
        for (Iterator iterator = eventsMap.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            Class eventClass = (Class) entry.getValue();
            if (event.getClass().isAssignableFrom(eventClass)) {
                listenerClass = (Class) entry.getKey();
                break;
            }
        }

        if (listenerClass == null) {
            throw new IllegalArgumentException(new Message(Messages.EVENT_TYPE_X_NOT_RECOGNISED, event.getClass()
                                                                                                      .getName()).getMessage());
        }

        listeners = getListeners(listenerClass);
        UMOServerEventListener l;
        synchronized (listeners) {
            for (Iterator iterator = listeners.keySet().iterator(); iterator.hasNext();) {
                l = (UMOServerEventListener) iterator.next();
                subscription = (String) listeners.get(l);
                if (subscription == null) {
                    subscription = NULL_SUBSCRIPTION;
                }
                // If the listener has a resource id associated with it, make
                // sure the event
                // is only fired if the event resource id and listener resource
                // id match
                if (NULL_SUBSCRIPTION.equals(subscription)
                        || new WildcardFilter(subscription).accept(event.getResourceIdentifier())) {
                    l.onEvent(event);
                } else {
                    logger.trace("Resource id '" + subscription + "' for listener " + l.getClass().getName()
                            + " does not match Resource id '" + event.getResourceIdentifier()
                            + "' for event, not firing event for this listener");
                }
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
		UMOServerEvent event;
		while (!disposed)
		{
			try
			{
				event = (UMOServerEvent)eventQueue.poll(5000, TimeUnit.MILLISECONDS);
				if (event != null)
				{
					notifyListeners(event);
				}
			}
			catch (InterruptedException e)
			{
				if (!disposed)
				{
					logger.error("Failed to take event from server event queue", e);
				}
			}
		}
	}
}
