/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.ProcessingTimeWatcher;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.api.util.queue.QueueManager;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import javax.transaction.TransactionManager;

/**
 * @deprecated {@link MuleContext} interface will be replaced in future major version of mule.
 *             <p/>
 *             For customizing an application or domain in mule you can use the {@link CustomizationService} which can be accessed
 *             by registering a deployment listener in a server plugin.
 *             <p/>
 *             For accessing application or domain services like {@link #getTransformationService()} or
 *             {@link #getErrorTypeRepository()} etc. you can use JSR330 annotations for dependency injection. In case you need to
 *             access dynamically to objects within the application/domain you can inject
 *             {@link org.mule.runtime.api.artifact.Registry} or if you want to access configuration elements you can use
 *             {@link ConfigurationComponentLocator}.
 */
@Deprecated
public interface MuleContext extends Lifecycle {

  /**
   * Sets the Jta Transaction Manager to use with this Mule server instance
   *
   * @param manager the manager to use
   * @throws Exception
   * @deprecated Use only for test cases.
   */
  @Deprecated
  void setTransactionManager(TransactionManager manager) throws Exception;

  /**
   * Returns the Jta transaction manager used by this Mule server instance. or null if a transaction manager has not been set
   *
   * @return the Jta transaction manager used by this Mule server instance. or null if a transaction manager has not been set
   */
  TransactionManager getTransactionManager();

  ServerNotificationManager getNotificationManager();

  /**
   * Determines if the server has been started
   *
   * @return true if the server has been started
   */
  boolean isStarted();

  /**
   * Determines if the server has been initialised
   *
   * @return true if the server has been initialised
   */
  boolean isInitialised();

  /**
   * Determines if the server is being initialised
   *
   * @return true if the server is beening initialised
   */
  boolean isInitialising();

  boolean isDisposed();

  boolean isDisposing();

  /**
   * Sets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @param securityManager the security manager used by this Mule instance to authenticate and authorise incoming and outgoing
   *        event traffic and service invocations
   */
  void setSecurityManager(SecurityManager securityManager) throws InitialisationException;

  /**
   * Gets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @return he security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   *         service invocations
   */
  SecurityManager getSecurityManager();

  SchedulerService getSchedulerService();

  SchedulerConfig getSchedulerBaseConfig();

  /**
   * Sets the queue manager used by mule for queuing events. This is used for service queues
   *
   * @param queueManager
   */
  void setQueueManager(QueueManager queueManager);

  /**
   * Gets the queue manager used by mule for queuing events. This is used for service queues.
   */
  QueueManager getQueueManager();

  ObjectStoreManager getObjectStoreManager();

  ExtensionManager getExtensionManager();

  /**
   * The instance of {@link ObjectSerializer} to be used to serialize/deserealize objects
   *
   * @return a {@link ObjectSerializer}
   * @since 3.7.0
   */
  ObjectSerializer getObjectSerializer();

  AllStatistics getStatistics();

  LifecycleManager getLifecycleManager();

  /**
   * Returns a {@link Injector} capable of injecting dependencies into objects
   *
   * @return a {@link Injector}
   * @since 3.7.0
   */
  Injector getInjector();

  MuleConfiguration getConfiguration();

  /**
   * Returns the configured {@link org.mule.runtime.core.api.util.StreamCloserService}
   *
   * @return a {@link org.mule.runtime.core.api.util.StreamCloserService}
   * @since 3.5.0
   */
  StreamCloserService getStreamCloserService();

  /**
   * Returns the date when the server was started.
   *
   * @return the date when the server was started.
   */
  long getStartDate();

  void setExecutionClassLoader(ClassLoader cl);

  ClassLoader getExecutionClassLoader();

  boolean isStopped();

  boolean isStopping();

  boolean isStarting();

  /**
   * Runs a command synchronized by the lock for the context's lifecycle.
   * <p>
   * Use this method to run code that depends on this context's lifecycle state not changing while the command is running.
   *
   * @param command the command to run with the lock for this context's lifecycle taken.
   */
  void withLifecycleLock(Runnable command);

  SystemExceptionHandler getExceptionListener();

  void setExceptionListener(SystemExceptionHandler exceptionListener);

  void handleException(Exception e, RollbackSourceCallback rollbackMethod);

  void handleException(Exception e);

  /**
   * @return the ID of the cluster the current instance belongs to. Returns the empty string if this instance isn't part of a
   *         cluster.
   */
  String getClusterId();

  /**
   * @return the cluster node ID for the current instance belongs to. Returns 0 if this instance isn't part of a cluster.
   */
  int getClusterNodeId();

  /**
   * @return true if this instance in the designated poller. This will always be true unless the instance is part of a cluster.
   */
  boolean isPrimaryPollingInstance();

  /**
   * Generate a unique ID string; this will begin with the cluster node ID followed by a dash, e.g. "3-XXXYYY"
   */
  String getUniqueIdString();

  /**
   * @return default exception strategy. If no default error handler was configured it returns one with a catch all
   *         <on-error-propagate> element.
   */
  FlowExceptionHandler getDefaultErrorHandler(Optional<String> rootContainerName);

  /**
   * @return single resource transaction factory manager. Used to retrieve a transaction factory for each transactional resource
   *         (i.e jdbc DataSource, jms Connection)
   */
  SingleResourceTransactionFactoryManager getTransactionFactoryManager();

  /**
   * @return a non null {@link DataTypeConversionResolver} instance to resolve implicit data type conversions
   */
  DataTypeConversionResolver getDataTypeConverterResolver();

  /**
   * @return an {@link ExtendedExpressionManager} for evaluating expressions using Mule as the context
   */
  ExtendedExpressionManager getExpressionManager();

  /**
   * Factory for creating locks for synchronizing mule components.
   *
   * Synchronization must be done using LockFactory locks in order for mule components to work in single servers as in a cluster
   *
   * @return a factory for creating locks
   */
  LockFactory getLockFactory();

  /**
   * @return {@link {ProcessingTimeWatcher} used to compute processing time of finalized events
   */
  ProcessingTimeWatcher getProcessorTimeWatcher();

  /**
   * Makes the caller wait until the {@link MuleContext} was started
   *
   * @param timeout maximum number of milliseconds that will be waiting
   * @return true if the context started before the timeout, false otherwise
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  boolean waitUntilStarted(int timeout) throws InterruptedException;

  /**
   * The {@link ArtifactType} indicating if this configuration object is for an application or a domain.
   */
  ArtifactType getArtifactType();

  /**
   * @return the callbacks for notifying when a flow call from another flow is started or completed.
   *
   * @since 3.8.0
   */
  FlowTraceManager getFlowTraceManager();

  /**
   * @return the providers for additional context information for exceptions.
   *
   * @since 3.8.0
   */
  Collection<ExceptionContextProvider> getExceptionContextProviders();

  /**
   * Gets application wide instance of an implementation of {@link TransformationService} used for applying
   * {@link org.mule.runtime.core.api.transformer.Transformer}'s to a {@link Message} and for obtaining different representations
   * of the message payload.
   *
   * @return transformation service
   */
  TransformationService getTransformationService();

  /**
   * Sets application wide instance of an implementation of {@link TransformationService}
   *
   * @param transformationService transformation service instance
   */
  void setTransformationService(TransformationService transformationService);

  /**
   * @return {@link BootstrapServiceDiscoverer} used to bootstrap this {@link MuleContext}
   */
  // TODO(pablo.kraan): remove this reference and use dependency injection (MULE-9157)
  BootstrapServiceDiscoverer getRegistryBootstrapServiceDiscoverer();

  /**
   * Provides access to a {@link CustomizationService} to change the default services provided by the {@code MuleContext}.
   *
   * Any usage of this service after the {@code MuleContext} initialization will be disregarded.
   *
   * @return a customization service.
   */
  CustomizationService getCustomizationService();

  /**
   * @return a unique identifier for this context.
   *
   * @since 4.0
   */
  String getId();

  /**
   * @return an error type repository to get access to the {@link org.mule.runtime.api.message.ErrorType} instances of this
   *         artifact.
   */
  ErrorTypeRepository getErrorTypeRepository();

  /**
   * Sets application wide instance of {@link BootstrapServiceDiscoverer}
   *
   * @param bootstrapServiceDiscoverer bootstrap service discoverer instance. Non null.
   */
  void setBootstrapServiceDiscoverer(BootstrapServiceDiscoverer bootstrapServiceDiscoverer);

  /**
   * @return locator for accessing runtime object created by the mule configuration.
   */
  ConfigurationComponentLocator getConfigurationComponentLocator();

  /**
   * Sets artifact wide instance of {@link ExtensionManager}
   *
   * @param extensionManager manages the extensions available on the artifact. Non null.
   */
  void setExtensionManager(ExtensionManager extensionManager);

  /**
   * @return the deployment properties.
   */
  Properties getDeploymentProperties();
}

