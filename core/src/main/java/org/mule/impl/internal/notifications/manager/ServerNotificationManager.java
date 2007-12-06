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
 *
 * <p>The configuration and resulting policy are separate; the policy
 * is a summary of the configuration that contains information to decide whether a particular
 * message can be handled, and which updates that with experience gained handling messages.
 * When the configuration is changed the policy is rebuilt.  In this way we get a fairly
 * efficient system without needing controls elsewhere.
 *
 * <p>However, measurements showed that there was still a small impact on speed in some
 * cases.  To improve behaviour further the
 * {@link org.mule.impl.internal.notifications.manager.OptimisedNotificationHandler} was
 * added.  This allows a service that generates notifications to cache locally a handler
 * optimised for a particular class.
 *
 * <p>The dynamic flag stops this caching from occurring.  This reduces efficiency slightly
 * (about 15% cost on simple VM messages, less on other transports)</p>
 *
 * <p>Note that, because of subclass relationships, we need to be very careful about exactly
 * what is enabled and disabled:
 * <ul>
 * <li>Disabling an event or interface disables all uses of that class or any subclass.</li>
 * <li>Enquiring whether an event is enabled returns true if any subclass is enabled.</li>
 * </ul>
 */
public class ServerNotificationManager implements Work, Disposable, ServerNotificationHandler
{

    public static final String NULL_SUBSCRIPTION = "NULL";
    protected Log logger = LogFactory.getLog(getClass());
    private boolean dynamic = false;
    private Configuration configuration = new Configuration();
    private AtomicBoolean disposed = new AtomicBoolean(false);
    private WorkListener workListener = null;
    private BlockingDeque eventQueue = new LinkedBlockingDeque();

    public boolean isNotificationDynamic()
    {
        return dynamic;
    }

    public void setNotificationDynamic(boolean dynamic)
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

    public void addInterfaceToType(Class iface, Class event)
    {
        configuration.addInterfaceToType(iface, event);
    }

    public void setInterfaceToTypes(Map interfaceToEvents) throws ClassNotFoundException
    {
        configuration.addAllInterfaceToTypes(interfaceToEvents);
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

    /**
     * This removes *all* registrations that reference this listener
     */
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

    public void disableType(Class type) throws ClassNotFoundException
    {
        configuration.disableType(type);
    }

    public void setDisabledTypes(Collection types) throws ClassNotFoundException
    {
        configuration.disableAllTypes(types);
    }

    public void fireNotification(UMOServerNotification notification)
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

    public boolean isNotificationEnabled(Class type)
    {
        return configuration.getPolicy().isNotificationEnabled(type);
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

    public Map getInterfaceToTypes()
    {
        return configuration.getInterfaceToTypes();
    }

    public Collection getListeners()
    {
        return configuration.getListeners();
    }

}
