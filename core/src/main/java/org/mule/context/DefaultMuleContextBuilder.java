/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.AsyncMessageNotificationListener;
import org.mule.api.context.notification.ClusterNodeNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ExceptionStrategyNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.PipelineMessageNotificationListener;
import org.mule.api.context.notification.RegistryNotificationListener;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.ImmutableThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.AsyncMessageNotification;
import org.mule.context.notification.ClusterNodeNotification;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.context.notification.RegistryNotification;
import org.mule.context.notification.RoutingNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.context.notification.ServiceNotification;
import org.mule.context.notification.TransactionNotification;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.expression.DefaultExpressionManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.registry.RegistryDelegatingInjector;
import org.mule.serialization.internal.JavaObjectSerializer;
import org.mule.util.ClassUtils;
import org.mule.util.SplashScreen;
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
    public static final String MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE = "mule.context.workmanager.maxthreadsactive";

    protected MuleConfiguration config;

    protected MuleContextLifecycleManager lifecycleManager;

    protected WorkManager workManager;

    protected WorkListener workListener;

    protected ServerNotificationManager notificationManager;

    protected SplashScreen startupScreen;

    protected SplashScreen shutdownScreen;

    /**
     * {@inheritDoc}
     */
    public MuleContext buildMuleContext()
    {
        logger.debug("Building new DefaultMuleContext instance with MuleContextBuilder: " + this);
        DefaultMuleContext muleContext = createDefaultMuleContext();
        muleContext.setMuleConfiguration(injectMuleContextIfRequired(getMuleConfiguration(), muleContext));
        muleContext.setWorkManager(injectMuleContextIfRequired(getWorkManager(), muleContext));
        muleContext.setworkListener(getWorkListener());
        muleContext.setNotificationManager(injectMuleContextIfRequired(getNotificationManager(), muleContext));
        muleContext.setLifecycleManager(injectMuleContextIfRequired(getLifecycleManager(), muleContext));
        muleContext.setExpressionManager(injectMuleContextIfRequired(new DefaultExpressionManager(), muleContext));

        DefaultRegistryBroker registryBroker = new DefaultRegistryBroker(muleContext);
        muleContext.setRegistryBroker(registryBroker);
        MuleRegistryHelper muleRegistry = new MuleRegistryHelper(registryBroker, muleContext);
        muleContext.setMuleRegistry(muleRegistry);
        muleContext.setInjector(new RegistryDelegatingInjector(muleRegistry));

        muleContext.setLocalMuleClient(new DefaultLocalMuleClient(muleContext));
        muleContext.setExceptionListener(new DefaultSystemExceptionStrategy(muleContext));
        muleContext.setExecutionClassLoader(Thread.currentThread().getContextClassLoader());

        JavaObjectSerializer defaultObjectSerializer = new JavaObjectSerializer();
        defaultObjectSerializer.setMuleContext(muleContext);
        muleContext.setObjectSerializer(defaultObjectSerializer);

        return muleContext;
    }

    protected DefaultMuleContext createDefaultMuleContext()
    {
        return new DefaultMuleContext();
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

    protected MuleConfiguration getMuleConfiguration()
    {
        if (config != null)
        {
            return config;
        }
        else
        {
            return createMuleConfiguration();
        }
    }

    public <T> T injectMuleContextIfRequired(T object, MuleContext muleContext)
    {
        if (object instanceof MuleContextAware)
        {
            ((MuleContextAware) object).setMuleContext(muleContext);
        }
        return object;
    }

    protected MuleContextLifecycleManager getLifecycleManager()
    {
        if (lifecycleManager != null)
        {
            return lifecycleManager;
        }
        else
        {
            return createLifecycleManager();
        }
    }

    public void setLifecycleManager(LifecycleManager manager)
    {
        if (!(manager instanceof MuleContextLifecycleManager))
        {
            Message msg = MessageFactory.createStaticMessage(
                    "lifecycle manager for MuleContext must be a MuleContextLifecycleManager");
            throw new MuleRuntimeException(msg);
        }

        lifecycleManager = (MuleContextLifecycleManager) manager;
    }

    protected WorkManager getWorkManager()
    {
        if (workManager != null)
        {
            return workManager;
        }
        else
        {
            return createWorkManager();
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
            return createWorkListener();
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
            return createNotificationManager();
        }
    }

    public SplashScreen getStartupScreen()
    {
        return startupScreen;
    }

    public void setStartupScreen(SplashScreen startupScreen)
    {
        this.startupScreen = startupScreen;
    }

    public SplashScreen getShutdownScreen()
    {
        return shutdownScreen;
    }

    public void setShutdownScreen(SplashScreen shutdownScreen)
    {
        this.shutdownScreen = shutdownScreen;
    }

    protected DefaultMuleConfiguration createMuleConfiguration()
    {
        return new DefaultMuleConfiguration();
    }

    protected MuleContextLifecycleManager createLifecycleManager()
    {
        return new MuleContextLifecycleManager();
    }

    protected MuleWorkManager createWorkManager()
    {
        final MuleConfiguration config = getMuleConfiguration();
        // still can be embedded, but in container mode, e.g. in a WAR
        final String threadPrefix = config.isContainerMode()
                                    ? String.format("[%s].Mule", config.getId())
                                    : "MuleServer";
        ImmutableThreadingProfile threadingProfile = createMuleWorkManager();
        return new MuleWorkManager(threadingProfile, threadPrefix, config.getShutdownTimeout());
    }

    protected ImmutableThreadingProfile createMuleWorkManager()
    {
        return new ImmutableThreadingProfile(
                Integer.valueOf(System.getProperty(MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE, String.valueOf(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE))),
                ThreadingProfile.DEFAULT_MAX_THREADS_IDLE,
                ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE,
                ThreadingProfile.DEFAULT_MAX_THREAD_TTL,
                ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT,
                ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION,
                ThreadingProfile.DEFAULT_DO_THREADING,
                null,
                null
        );
    }

    protected DefaultWorkListener createWorkListener()
    {
        return new DefaultWorkListener();
    }

    protected ServerNotificationManager createNotificationManager()
    {
        return createDefaultNotificationManager();
    }

    public static ServerNotificationManager createDefaultNotificationManager()
    {
        ServerNotificationManager manager = new ServerNotificationManager();
        manager.addInterfaceToType(MuleContextNotificationListener.class,
                                   MuleContextNotification.class);
        manager.addInterfaceToType(ModelNotificationListener.class, ModelNotification.class);
        manager.addInterfaceToType(RoutingNotificationListener.class, RoutingNotification.class);
        manager.addInterfaceToType(ServiceNotificationListener.class,
                                   ServiceNotification.class);
        manager.addInterfaceToType(SecurityNotificationListener.class,
                                   SecurityNotification.class);
        manager.addInterfaceToType(ManagementNotificationListener.class,
                                   ManagementNotification.class);
        manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
        manager.addInterfaceToType(ConnectionNotificationListener.class,
                                   ConnectionNotification.class);
        manager.addInterfaceToType(RegistryNotificationListener.class,
                                   RegistryNotification.class);
        manager.addInterfaceToType(ExceptionNotificationListener.class,
                                   ExceptionNotification.class);
        manager.addInterfaceToType(ExceptionStrategyNotificationListener.class,
                                   ExceptionStrategyNotification.class);
        manager.addInterfaceToType(TransactionNotificationListener.class,
                                   TransactionNotification.class);
        manager.addInterfaceToType(PipelineMessageNotificationListener.class,
                                   PipelineMessageNotification.class);
        manager.addInterfaceToType(AsyncMessageNotificationListener.class,
                                   AsyncMessageNotification.class);
        manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
        return manager;
    }

    @Override
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
