/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.RegistryNotificationListener;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.RegistryNotification;
import org.mule.context.notification.RoutingNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.context.notification.ServiceNotification;
import org.mule.context.notification.TransactionNotification;
import org.mule.lifecycle.GenericLifecycleManager;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.util.ClassUtils;
import org.mule.work.DefaultWorkListener;
import org.mule.work.MuleWorkManager;

import javax.resource.spi.work.WorkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link MuleContextBuilder} that uses {@link DefaultMuleContext}
 * as the default {@link MuleContext} implementation and builds it with defaults
 * values for {@link MuleConfiguration}, {@link LifecycleManager}, {@link WorkManager}, 
 * {@link WorkListener} and {@link ServerNotificationManager}.
 */
public class DefaultMuleContextBuilder implements MuleContextBuilder
{
    protected static final Log logger = LogFactory.getLog(DefaultMuleContextBuilder.class);
    
    protected MuleConfiguration config;

    protected LifecycleManager lifecycleManager;

    protected WorkManager workManager;

    protected WorkListener workListener;

    protected ServerNotificationManager notificationManager;

    /**
     * {@inheritDoc}
     */
    public MuleContext buildMuleContext()
    {
        logger.debug("Building new DefaultMuleContext instance with MuleContextBuilder: " + this);
        MuleContext muleContext = new DefaultMuleContext(getMuleConfiguration(),
                                                         getWorkManager(),
                                                         getWorkListener(),
                                                         getLifecycleManager(),
                                                         getNotificationManager());
        return muleContext;
    }

    public void setMuleConfiguration(MuleConfiguration config)
    {
        this.config = config;
    }
    
    public void setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    public void setWorkListener(WorkListener workListener)
    {
        this.workListener = workListener;
    }
    
    public void setNotificationManager(ServerNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }
    
    protected MuleConfiguration getMuleConfiguration()
    {
        if (config != null)
        {
            return config;
        }
        else
        {
            return new DefaultMuleConfiguration();
        }
    }

    protected LifecycleManager getLifecycleManager()
    {
        if (lifecycleManager != null)
        {
            return lifecycleManager;
        }
        else
        {
            LifecycleManager lifecycleManager = new GenericLifecycleManager();
            lifecycleManager.registerLifecycle(new MuleContextInitialisePhase());
            lifecycleManager.registerLifecycle(new MuleContextStartPhase());
            lifecycleManager.registerLifecycle(new MuleContextStopPhase());
            lifecycleManager.registerLifecycle(new MuleContextDisposePhase());
            return lifecycleManager;
        }
    }

    protected WorkManager getWorkManager()
    {
        if (workManager != null)
        {
            return workManager;
        }
        else
        {
            return new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, "MuleServer", getMuleConfiguration().getShutdownTimeout());
        }
    }

    protected WorkListener getWorkListener()
    {
        if (workListener != null)
        {
            return workListener;
        }
        else
        {
            return new DefaultWorkListener();
        }
    }

    protected ServerNotificationManager getNotificationManager()
    {
        if (notificationManager != null)
        {
            return notificationManager;
        }
        else
        {
            ServerNotificationManager notificationManager = new ServerNotificationManager();
            notificationManager.addInterfaceToType(MuleContextNotificationListener.class,
                MuleContextNotification.class);
            notificationManager.addInterfaceToType(ModelNotificationListener.class, ModelNotification.class);
            notificationManager.addInterfaceToType(RoutingNotificationListener.class, RoutingNotification.class);
            notificationManager.addInterfaceToType(ServiceNotificationListener.class,
                ServiceNotification.class);
            notificationManager.addInterfaceToType(SecurityNotificationListener.class,
                SecurityNotification.class);
            notificationManager.addInterfaceToType(ManagementNotificationListener.class,
                ManagementNotification.class);
            notificationManager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
            notificationManager.addInterfaceToType(ConnectionNotificationListener.class,
                ConnectionNotification.class);
            notificationManager.addInterfaceToType(RegistryNotificationListener.class,
                RegistryNotification.class);
            notificationManager.addInterfaceToType(ExceptionNotificationListener.class,
                ExceptionNotification.class);
            notificationManager.addInterfaceToType(TransactionNotificationListener.class,
                TransactionNotification.class);
            return notificationManager;
        }
    }

    public String toString()
    {
        return ClassUtils.getClassName(getClass()) + 
            "{muleConfiguration=" + config +
            ", lifecycleManager=" + lifecycleManager + 
            ", workManager=" + workManager + 
            ", workListener=" + workListener + 
            ", notificationManager=" + notificationManager + "}";
    }
}
