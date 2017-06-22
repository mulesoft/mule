/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static org.mule.runtime.core.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapServiceDiscoverer;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.api.context.notification.ConnectionNotification;
import org.mule.runtime.core.api.context.notification.CustomNotification;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.context.notification.ExceptionNotification;
import org.mule.runtime.core.api.context.notification.ManagementNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.context.notification.RegistryNotification;
import org.mule.runtime.core.api.context.notification.RoutingNotification;
import org.mule.runtime.core.api.context.notification.SecurityNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.context.notification.TransactionNotification;
import org.mule.runtime.core.exception.DefaultSystemExceptionStrategy;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.internal.client.DefaultLocalMuleClient;
import org.mule.runtime.core.internal.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.registry.RegistryDelegatingInjector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link MuleContextBuilder} that uses {@link DefaultMuleContext} as the default {@link MuleContext}
 * implementation and builds it with defaults values for {@link MuleConfiguration}, {@link LifecycleManager} and
 * {@link ServerNotificationManager}.
 */
public class DefaultMuleContextBuilder implements MuleContextBuilder {

  protected static final Logger logger = LoggerFactory.getLogger(DefaultMuleContextBuilder.class);

  protected MuleConfiguration config;

  private MuleContextLifecycleManager lifecycleManager;

  protected ServerNotificationManager notificationManager;

  protected BootstrapServiceDiscoverer bootstrapDiscoverer;

  protected ClassLoader executionClassLoader;

  protected ObjectSerializer objectSerializer;

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext buildMuleContext() {
    logger.debug("Building new DefaultMuleContext instance with MuleContextBuilder: " + this);
    DefaultMuleContext muleContext = createDefaultMuleContext();
    muleContext.setMuleConfiguration(injectMuleContextIfRequired(getMuleConfiguration(), muleContext));
    muleContext.setNotificationManager(injectMuleContextIfRequired(getNotificationManager(), muleContext));
    muleContext.setLifecycleManager(injectMuleContextIfRequired(getLifecycleManager(), muleContext));

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

    getObjectSerializer(muleContext);
    ErrorTypeRepository defaultErrorTypeRepository = createDefaultErrorTypeRepository();
    muleContext.setErrorTypeRepository(defaultErrorTypeRepository);
    muleContext.setErrorTypeLocator(createDefaultErrorTypeLocator(defaultErrorTypeRepository));

    muleContext.setProcessorInterceptorManager(new DefaultProcessorInterceptorManager());

    return muleContext;
  }

  private void getObjectSerializer(DefaultMuleContext muleContext) {
    if (objectSerializer == null) {
      objectSerializer = new JavaObjectSerializer();
    }

    if (objectSerializer instanceof MuleContextAware) {
      ((MuleContextAware) objectSerializer).setMuleContext(muleContext);
    }

    muleContext.setObjectSerializer(objectSerializer);
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

  @Override
  public void setObjectSerializer(ObjectSerializer objectSerializer) {
    this.objectSerializer = objectSerializer;
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

  protected LifecycleManager getLifecycleManager() {
    if (lifecycleManager != null) {
      return lifecycleManager;
    } else {
      return createLifecycleManager();
    }
  }

  @Override
  public void setLifecycleManager(LifecycleManager manager) {
    if (!(manager instanceof MuleContextLifecycleManager)) {
      I18nMessage msg =
          I18nMessageFactory.createStaticMessage("lifecycle manager for MuleContext must be a MuleContextLifecycleManager");
      throw new MuleRuntimeException(msg);
    }

    lifecycleManager = (MuleContextLifecycleManager) manager;
  }

  protected ServerNotificationManager getNotificationManager() {
    if (notificationManager != null) {
      return notificationManager;
    } else {
      return createNotificationManager();
    }
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
    return new PropertiesBootstrapServiceDiscoverer(DefaultMuleContextBuilder.class.getClassLoader());
  }

  protected DefaultMuleConfiguration createMuleConfiguration() {
    return new DefaultMuleConfiguration();
  }

  private MuleContextLifecycleManager createLifecycleManager() {
    return new MuleContextLifecycleManager();
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
    manager.addInterfaceToType(ErrorHandlerNotificationListener.class, ErrorHandlerNotification.class);
    manager.addInterfaceToType(TransactionNotificationListener.class, TransactionNotification.class);
    manager.addInterfaceToType(PipelineMessageNotificationListener.class, PipelineMessageNotification.class);
    manager.addInterfaceToType(AsyncMessageNotificationListener.class, AsyncMessageNotification.class);
    manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
    return manager;
  }

  @Override
  public String toString() {
    return ClassUtils.getClassName(getClass()) + "{muleConfiguration=" + config + ", lifecycleManager=" + lifecycleManager
        + ", notificationManager=" + notificationManager + "}";
  }
}
