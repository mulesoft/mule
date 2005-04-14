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

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.manager.UMOServerEventListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>ServerEventManager</code> manages all server listeners for a Mule Instance
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ServerEventManager implements Runnable, Disposable
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ServerEventManager.class);

    private Map listenersMap = null;
    private BoundedBuffer eventQueue;
    private Thread eventLoop;
    private boolean disposed = false;

    public ServerEventManager()
    {
        init();
    }

    private synchronized void init()
    {
        listenersMap = new ConcurrentHashMap(4);
        listenersMap.put(ManagerEvent.class, new HashSet());
        listenersMap.put(ModelEvent.class, new HashSet());
        listenersMap.put(ComponentEvent.class, new HashSet());
        listenersMap.put(SecurityEvent.class, new HashSet());
        listenersMap.put(ManagementEvent.class, new HashSet());
        listenersMap.put(AdminEvent.class, new HashSet());
        listenersMap.put(CustomEvent.class, new HashSet());
        eventQueue = new BoundedBuffer(1000);
        eventLoop = new Thread(this, "Event Loop");
        eventLoop.start();
    }

    public void registerListener(UMOServerEventListener listener)
    {
        HashSet listeners = getListeners(listener.getClass());
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    public void unregisterListener(UMOServerEventListener listener)
    {
        HashSet listeners = getListeners(listener.getClass());
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    public void clearListeners(Class listenerClass)
    {
        if (listenerClass == null) return;
        HashSet listeners = getListeners(listenerClass);
        synchronized (listeners)
        {
            listeners.clear();
        }
    }

    public void clear()
    {
        for (Iterator iterator = listenersMap.values().iterator(); iterator.hasNext();)
        {
            HashSet set = (HashSet) iterator.next();
            synchronized (set)
            {
                set.clear();
            }
        }
        listenersMap.clear();
        init();
    }

    protected HashSet getListeners(Class listenerClass)
    {
        if (listenerClass == null)
        {
            throw new NullPointerException("Listener class cannot be null");
        }
        if (ManagerEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(ManagerEvent.class);
        } else if (ModelEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(ModelEvent.class);
        } else if (ComponentEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(ComponentEvent.class);
        } else if (ManagementEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(ManagementEvent.class);
        } else if (SecurityEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(SecurityEvent.class);
        } else if (CustomEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(CustomEvent.class);
        } else if (AdminEventListener.class.isAssignableFrom(listenerClass))
        {
            return (HashSet) listenersMap.get(AdminEvent.class);
        } else
        {
            throw new IllegalArgumentException("Listener type not recognised: " + listenerClass.getName());
        }
    }

    public void fireEvent(UMOServerEvent event)
    {
        if (event instanceof BlockingServerEvent)
        {
            notifyListeners(event);
            return;
        }
        try
        {
            eventQueue.put(event);
        } catch (InterruptedException e)
        {
            logger.error("Failed to queue event: " + event, e);
        }
    }

    public void dispose()
    {
        clear();
        disposed = true;
    }

    protected void notifyListeners(UMOServerEvent event)
    {
        HashSet listeners;
        if (event instanceof ManagerEvent)
        {
            listeners = getListeners(ManagerEventListener.class);
            ManagerEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (ManagerEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof ModelEvent)
        {
            listeners = getListeners(ModelEventListener.class);
            ModelEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (ModelEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof ComponentEvent)
        {
            listeners = getListeners(ComponentEventListener.class);
            ComponentEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (ComponentEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof ManagementEvent)
        {
            listeners = getListeners(ManagementEventListener.class);
            ManagementEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (ManagementEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof SecurityEvent)
        {
            listeners = getListeners(SecurityEventListener.class);
            SecurityEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (SecurityEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof CustomEvent)
        {
            listeners = getListeners(CustomEventListener.class);
            CustomEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (CustomEventListener) iterator.next();
                l.onEvent(event);
            }
        } else if (event instanceof CustomEvent)
        {
            listeners = getListeners(CustomEventListener.class);
            AdminEventListener l;
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
            {
                l = (AdminEventListener) iterator.next();
                l.onEvent(event);
            }
        } else
        {
            throw new IllegalArgumentException("Event type not recognised: " + event.getClass().getName());
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
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
                event = (UMOServerEvent) eventQueue.take();
                notifyListeners(event);
            } catch (InterruptedException e)
            {
                logger.error("Failed to take event from server event queue", e);
            }
        }
    }
}
