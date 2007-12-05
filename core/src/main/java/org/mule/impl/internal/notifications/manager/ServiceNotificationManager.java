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

import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.util.ClassUtils;
import org.mule.impl.internal.notifications.BlockingServerEvent;
import org.mule.config.MuleConfiguration;
import org.mule.RegistryContext;

import java.util.Map;
import java.util.Collection;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkException;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A reworking of the event manager that allows efficient behaviour without global on/off
 * switches in the config.
 */
public class ServiceNotificationManager implements Work, Disposable
{

    public static final String NULL_SUBSCRIPTION = "NULL";
    protected Log logger = LogFactory.getLog(getClass());
    private boolean dynamic = false;
    private Configuration configuration = new Configuration();
    private AtomicBoolean disposed = new AtomicBoolean(false);
    private WorkListener workListener = null;
    private BlockingDeque eventQueue = new LinkedBlockingDeque();

    public boolean isDynamic()
    {
        return dynamic;
    }

    public void setDynamic(boolean dynamic)
    {
        this.dynamic = dynamic;
    }

    public void start(UMOWorkManager workManager) throws LifecycleException
    {
        try
        {
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, getWorkListener());
        }
        catch (WorkException e)
        {
            throw new LifecycleException(e, this);
        }
    }

    public void addInterfaceToEvent(Class iface, Class event)
    {
        configuration.addInterfaceToEvent(iface, event);
    }

    public void setInterfaceToEvents(Map interfaceToEvents) throws ClassNotFoundException
    {
        configuration.addAllInterfaceToEvents(interfaceToEvents);
    }

    public void addListenerSubscriptionPair(ListenerSubscriptionPair pair)
    {
        configuration.addListenerSubscriptionPair(pair);
    }

    public void addListener(UMOServerNotificationListener listener)
    {
        configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener));
    }

    public void addListenerSubscription(UMOServerNotificationListener listener, String subscription)
    {
        configuration.addListenerSubscriptionPair(new ListenerSubscriptionPair(listener, subscription));
    }

    public void setAllListenerSubscriptionPairs(Collection pairs)
    {
        configuration.addAllListenerSubscriptionPairs(pairs);
    }

    public void removeListener(UMOServerNotificationListener listener)
    {
        configuration.removeListener(listener);
    }

    public void removeAllListeners(Collection listeners)
    {
        configuration.removeAllListeners(listeners);
    }

    public void disableInterface(Class iface) throws ClassNotFoundException
    {
        configuration.disableInterface(iface);
    }

    public void setDisabledInterfaces(Collection interfaces) throws ClassNotFoundException
    {
        configuration.disabledAllInterfaces(interfaces);
    }

    public void disableEvent(Class event) throws ClassNotFoundException
    {
        configuration.disableEvent(event);
    }

    public void setDisabledEvents(Collection events) throws ClassNotFoundException
    {
        configuration.disableAllEvents(events);
    }

    public void fireEvent(UMOServerNotification notification)
    {
        if (!disposed.get())
        {
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
                    if (!disposed.get())
                    {
                        logger.error("Failed to queue notification: " + notification, e);
                    }
                }
            }
        }
    }

    public boolean isEventEnabled(Class event)
    {
        return configuration.getPolicy().isEventEnabled(event);
    }

    /**
     * Provide a cacheable decision for connectors and the like, so that they can be
     * as efficient as possible (in the non-dynamic case; when dynamic we still try to
     * be relatively efficient using the descision cache in the policy).
     *
     * @param event
     * @return An oracle that controls whether or not the event should be dispatched
     */
    public EventDecision getEventDecision(Class event)
    {
        if (dynamic)
        {
            return new EventDecision(this, event);
        }
        else
        {
            return new EventDecision(isEventEnabled(event));
        }
    }

    public void dispose()
    {
        disposed.set(true);
        configuration = null;
        workListener = null;
    }

    protected void notifyListeners(UMOServerNotification notification)
    {
        if (!disposed.get())
        {
            configuration.getPolicy().dispatch(notification);
        }
    }

    public void release()
    {
        dispose();
    }

    public void run()
    {
        while (!disposed.get())
        {
            try
            {
                UMOServerNotification notification = (UMOServerNotification) eventQueue.take();
                notifyListeners(notification);
            }
            catch (InterruptedException e)
            {
                // ignore - we just loop round
            }
        }
    }

    public WorkListener getWorkListener()
    {
        if (null == workListener)
        {
            MuleConfiguration config = RegistryContext.getConfiguration();
            if (null != config)
            {
                workListener = config.getDefaultWorkListener();
            }
        }
        return workListener;
    }

    public void setWorkListener(WorkListener workListener)
    {
        if (null == workListener)
        {
            throw new IllegalArgumentException("workListener may not be null");
        }
        this.workListener = workListener;
    }


    /**
     * Support string or class parameters
     */
    public static Class toClass(Object value) throws ClassNotFoundException
    {
        Class clazz;
        if (value instanceof String)
        {
            clazz = ClassUtils.loadClass(value.toString(), value.getClass());
        }
        else if(value instanceof Class)
        {
            clazz = (Class)value;
        }
        else
        {
           throw new IllegalArgumentException("Notification types and listeners must be a Class with fully qualified class name. Value is: " + value);
        }
        return clazz;
    }

    // for tests -------------------------------------------------------

    Policy getPolicy()
    {
        return configuration.getPolicy();
    }

    public Map getInterfaceToEvents()
    {
        return configuration.getInterfaceToEvents();
    }

    public Collection getListeners()
    {
        return configuration.getListeners();
    }

}
