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
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.AdminNotificationListener;
import org.mule.api.context.notification.ComponentNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.ManagerNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.RegistryNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.config.MuleConfiguration;
import org.mule.context.notification.AdminNotification;
import org.mule.context.notification.ComponentNotification;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.ManagerNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.RegistryNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.context.notification.TransactionNotification;
import org.mule.lifecycle.GenericLifecycleManager;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.util.ClassUtils;
import org.mule.work.MuleWorkManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link MuleContextBuilder} that uses {@link DefaultMuleContext}
 * as the default {@link MuleContext} implementation and builds it with defaults
 * values for {@link MuleConfiguration}, {@link LifecycleManager},
 * {@link WorkManager} and {@link ServerNotificationManager}.
 */
public class DefaultMuleContextBuilder implements MuleContextBuilder
{

    protected static final Log logger = LogFactory.getLog(DefaultMuleContextBuilder.class);

    protected MuleConfiguration muleConfiguration;

    protected LifecycleManager lifecycleManager;

    protected WorkManager workManager;

    protected ServerNotificationManager notificationManager;

    /**
     * {@inheritDoc}
     */
    public MuleContext buildMuleContext()
    {
        logger.debug("Building new DefaultMuleContext instance with MuleContextBuilder: " + this);
        MuleContext muleContext = new DefaultMuleContext(getLifecycleManager());
        muleContext.setConfiguration(getMuleConfiguration());
        muleContext.setWorkManager(getWorkManager());
        muleContext.setNotificationManager(getNotificationManager());
        return muleContext;
    }

    public DefaultMuleContextBuilder setMuleConfiguration(MuleConfiguration muleConfiguration)
    {
        this.muleConfiguration = muleConfiguration;
        return this;
    }

    public DefaultMuleContextBuilder setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
        return this;
    }

    public DefaultMuleContextBuilder setNotificationManager(ServerNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
        return this;
    }

    public DefaultMuleContextBuilder setLifecycleManager(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
        return this;
    }

    protected MuleConfiguration getMuleConfiguration()
    {
        if (muleConfiguration != null)
        {
            return muleConfiguration;
        }
        else
        {
            return new MuleConfiguration();

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
            return new MuleWorkManager(getMuleConfiguration().getDefaultComponentThreadingProfile(),
                "MuleServer");
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
            notificationManager.addInterfaceToType(ManagerNotificationListener.class,
                ManagerNotification.class);
            notificationManager.addInterfaceToType(ModelNotificationListener.class, ModelNotification.class);
            notificationManager.addInterfaceToType(ComponentNotificationListener.class,
                ComponentNotification.class);
            notificationManager.addInterfaceToType(SecurityNotificationListener.class,
                SecurityNotification.class);
            notificationManager.addInterfaceToType(ManagementNotificationListener.class,
                ManagementNotification.class);
            notificationManager.addInterfaceToType(AdminNotificationListener.class, AdminNotification.class);
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
        return ClassUtils.getClassName(getClass()) + "{muleConfiguration=" + muleConfiguration
               + ", lifecycleManager=" + lifecycleManager + ", workManager=" + workManager
               + ", notificationManager=" + notificationManager + "}";
    }
}
