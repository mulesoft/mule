/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context;

import static org.mule.runtime.core.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;

import javax.resource.spi.work.WorkListener;

import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionStrategyNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.client.DefaultLocalMuleClient;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.ImmutableThreadingProfile;
import org.mule.runtime.core.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.config.bootstrap.PropertiesBootstrapServiceDiscoverer;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.RegistryNotification;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.exception.DefaultSystemExceptionStrategy;
import org.mule.runtime.core.expression.DefaultExpressionManager;
import org.mule.runtime.core.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.registry.RegistryDelegatingInjector;
import org.mule.runtime.core.serialization.internal.JavaObjectSerializer;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.SplashScreen;
import org.mule.runtime.core.work.DefaultWorkListener;
import org.mule.runtime.core.work.MuleWorkManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link MuleContextBuilder} that uses {@link DefaultMuleContext} as the default {@link MuleContext}
 * implementation and builds it with defaults values for {@link MuleConfiguration}, {@link LifecycleManager}, {@link WorkManager},
 * {@link WorkListener} and {@link ServerNotificationManager}.
 */
public class DefaultMuleContextBuilder implements MuleContextBuilder {

  protected static final Logger logger = LoggerFactory.getLogger(DefaultMuleContextBuilder.class);
  public static final String MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE = "mule.context.workmanager.maxthreadsactive";

  protected MuleConfiguration config;

  protected MuleContextLifecycleManager lifecycleManager;

  protected WorkManager workManager;

  protected WorkListener workListener;

  protected ServerNotificationManager notificationManager;

  protected SplashScreen startupScreen;

  protected SplashScreen shutdownScreen;

  protected BootstrapServiceDiscoverer bootstrapDiscoverer;

  protected ClassLoader executionClassLoader;

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext buildMuleContext() {
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
    muleContext.setExceptionListener(createExceptionListener(muleContext));
    muleContext.setExecutionClassLoader(getExecutionClassLoader());
    muleContext
        .setBootstrapServiceDiscoverer(injectMuleContextIfRequired(getBootstrapPropertiesServiceDiscoverer(), muleContext));

    JavaObjectSerializer defaultObjectSerializer = new JavaObjectSerializer();
    defaultObjectSerializer.setMuleContext(muleContext);
    muleContext.setObjectSerializer(defaultObjectSerializer);
    muleContext.setErrorTypeLocator(createDefaultErrorTypeLocator(createDefaultErrorTypeRepository()));

    return muleContext;
  }

  protected SystemExceptionHandler createExceptionListener(DefaultMuleContext muleContext) {
    SystemExceptionHandler systemExceptionHandler = muleContext.getRegistry().get("_exceptionListenerFactory");
    if (systemExceptionHandler == null) {
      systemExceptionHandler = new DefaultSystemExceptionStrategy();
    }
    return systemExceptionHandler;
  }

  protected DefaultMuleContext createDefaultMuleContext() {
    return new DefaultMuleContext();
  }

  @Override
  public void setMuleConfiguration(MuleConfiguration config) {
    this.config = config;
  }

  @Override
  public void setWorkManager(WorkManager workManager) {
    this.workManager = workManager;
  }

  @Override
  public void setWorkListener(WorkListener workListener) {
    this.workListener = workListener;
  }

  @Override
  public void setNotificationManager(ServerNotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }

  protected MuleConfiguration getMuleConfiguration() {
    if (config != null) {
      return config;
    } else {
      return createMuleConfiguration();
    }
  }

  @Override
  public void setExecutionClassLoader(ClassLoader executionClassLoader) {
    this.executionClassLoader = executionClassLoader;
  }

  protected ClassLoader getExecutionClassLoader() {
    if (executionClassLoader != null) {
      return executionClassLoader;
    } else {
      return Thread.currentThread().getContextClassLoader();
    }
  }

  public <T> T injectMuleContextIfRequired(T object, MuleContext muleContext) {
    if (object instanceof MuleContextAware) {
      ((MuleContextAware) object).setMuleContext(muleContext);
    }
    return object;
  }

  protected MuleContextLifecycleManager getLifecycleManager() {
    if (lifecycleManager != null) {
      return lifecycleManager;
    } else {
      return createLifecycleManager();
    }
  }

  @Override
  public void setLifecycleManager(LifecycleManager manager) {
    if (!(manager instanceof MuleContextLifecycleManager)) {
      Message msg = MessageFactory.createStaticMessage("lifecycle manager for MuleContext must be a MuleContextLifecycleManager");
      throw new MuleRuntimeException(msg);
    }

    lifecycleManager = (MuleContextLifecycleManager) manager;
  }

  protected WorkManager getWorkManager() {
    if (workManager != null) {
      return workManager;
    } else {
      return createWorkManager();
    }
  }

  protected WorkListener getWorkListener() {
    if (workListener != null) {
      return workListener;
    } else {
      return createWorkListener();
    }
  }

  protected ServerNotificationManager getNotificationManager() {
    if (notificationManager != null) {
      return notificationManager;
    } else {
      return createNotificationManager();
    }
  }

  public SplashScreen getStartupScreen() {
    return startupScreen;
  }

  public void setStartupScreen(SplashScreen startupScreen) {
    this.startupScreen = startupScreen;
  }

  public SplashScreen getShutdownScreen() {
    return shutdownScreen;
  }

  public void setShutdownScreen(SplashScreen shutdownScreen) {
    this.shutdownScreen = shutdownScreen;
  }

  public void setBootstrapPropertiesServiceDiscoverer(BootstrapServiceDiscoverer bootstrapDiscoverer) {
    this.bootstrapDiscoverer = bootstrapDiscoverer;
  }

  public BootstrapServiceDiscoverer getBootstrapPropertiesServiceDiscoverer() {
    if (bootstrapDiscoverer != null) {
      return bootstrapDiscoverer;
    } else {
      return createBootstrapDiscoverer();
    }
  }

  protected BootstrapServiceDiscoverer createBootstrapDiscoverer() {
    return new PropertiesBootstrapServiceDiscoverer(Thread.currentThread().getContextClassLoader());
  }

  protected DefaultMuleConfiguration createMuleConfiguration() {
    return new DefaultMuleConfiguration();
  }

  protected MuleContextLifecycleManager createLifecycleManager() {
    return new MuleContextLifecycleManager();
  }

  protected MuleWorkManager createWorkManager() {
    final MuleConfiguration config = getMuleConfiguration();
    // still can be embedded, but in container mode, e.g. in a WAR
    final String threadPrefix = config.isContainerMode() ? String.format("[%s].Mule", config.getId()) : "MuleServer";
    ImmutableThreadingProfile threadingProfile = createMuleWorkManager();
    return new MuleWorkManager(threadingProfile, threadPrefix, config.getShutdownTimeout());
  }

  protected ImmutableThreadingProfile createMuleWorkManager() {
    return new ImmutableThreadingProfile(Integer.valueOf(System
        .getProperty(MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE, String.valueOf(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE))),
                                         ThreadingProfile.DEFAULT_MAX_THREADS_IDLE, ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE,
                                         ThreadingProfile.DEFAULT_MAX_THREAD_TTL, ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT,
                                         ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION, ThreadingProfile.DEFAULT_DO_THREADING,
                                         null, null);
  }

  protected DefaultWorkListener createWorkListener() {
    return new DefaultWorkListener();
  }

  protected ServerNotificationManager createNotificationManager() {
    return createDefaultNotificationManager();
  }

  public static ServerNotificationManager createDefaultNotificationManager() {
    ServerNotificationManager manager = new ServerNotificationManager();
    manager.addInterfaceToType(MuleContextNotificationListener.class, MuleContextNotification.class);
    manager.addInterfaceToType(RoutingNotificationListener.class, RoutingNotification.class);
    manager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);
    manager.addInterfaceToType(ManagementNotificationListener.class, ManagementNotification.class);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
    manager.addInterfaceToType(ConnectionNotificationListener.class, ConnectionNotification.class);
    manager.addInterfaceToType(RegistryNotificationListener.class, RegistryNotification.class);
    manager.addInterfaceToType(ExceptionNotificationListener.class, ExceptionNotification.class);
    manager.addInterfaceToType(ExceptionStrategyNotificationListener.class, ExceptionStrategyNotification.class);
    manager.addInterfaceToType(TransactionNotificationListener.class, TransactionNotification.class);
    manager.addInterfaceToType(PipelineMessageNotificationListener.class, PipelineMessageNotification.class);
    manager.addInterfaceToType(AsyncMessageNotificationListener.class, AsyncMessageNotification.class);
    manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
    return manager;
  }

  @Override
  public String toString() {
    return ClassUtils.getClassName(getClass()) + "{muleConfiguration=" + config + ", lifecycleManager=" + lifecycleManager
        + ", workManager=" + workManager + ", workListener=" + workListener + ", notificationManager=" + notificationManager
        + "}";
  }
}
