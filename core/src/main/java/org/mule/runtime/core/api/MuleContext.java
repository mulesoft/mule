/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.interception.ProcessorInterceptorProvider;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.locator.ConfigurationComponentLocator;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.management.stats.AllStatistics;
import org.mule.runtime.core.management.stats.ProcessingTimeWatcher;
import org.mule.runtime.core.util.queue.QueueManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

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
   * Registers an intenal server event listener. The listener will be notified when a particular event happens within the server.
   * Typically this is not an event in the same sense as an MuleEvent (although there is nothing stopping the implementation of
   * this class triggering listeners when a MuleEvent is received).
   * <p/>
   * The types of notifications fired is entirely defined by the implementation of this class
   *
   * @param l the listener to register
   */
  void registerListener(ServerNotificationListener l) throws NotificationException;

  /**
   * Registers an intenal server event listener. The listener will be notified when a particular event happens within the server.
   * Typically this is not an event in the same sense as an MuleEvent (although there is nothing stopping the implementation of
   * this class triggering listeners when a MuleEvent is received).
   * <p/>
   * The types of notifications fired is entirely defined by the implementation of this class
   *
   * @param l the listener to register
   * @param resourceIdentifier a particular resource name for the given type of listener For example, the resourceName could be
   *        the name of a service if the listener was a ServiceNotificationListener
   */
  void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException;

  /**
   * Unregisters a previously registered listener. If the listener has not already been registered, this method should return
   * without exception
   *
   * @param l the listener to unregister
   */
  void unregisterListener(ServerNotificationListener l);

  /**
   * Fires a server notification to all regiistered listeners
   *
   * @param notification the notification to fire
   */
  void fireNotification(ServerNotification notification);

  /**
   * Sets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @param securityManager the security manager used by this Mule instance to authenticate and authorise incoming and outgoing
   *        event traffic and service invocations
   * @throws RegistrationException
   */
  void setSecurityManager(SecurityManager securityManager) throws InitialisationException, RegistrationException;

  /**
   * Gets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @return he security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   *         service invocations
   */
  SecurityManager getSecurityManager();

  SchedulerService getSchedulerService();

  /**
   * Sets the queue manager used by mule for queuing events. This is used for service queues
   *
   * @param queueManager
   * @throws RegistrationException
   *
   */
  void setQueueManager(QueueManager queueManager) throws RegistrationException;

  /**
   * Gets the queue manager used by mule for queuing events. This is used for service queues.
   */
  QueueManager getQueueManager();

  ObjectStoreManager getObjectStoreManager();

  ExtensionManager getExtensionManager();

  /**
   * The instance of {@link org.mule.runtime.core.api.serialization.ObjectSerializer} to be used to serialize/deserealize objects
   *
   * @return a {@link org.mule.runtime.core.api.serialization.ObjectSerializer}
   * @since 3.7.0
   */
  ObjectSerializer getObjectSerializer();

  AllStatistics getStatistics();

  LifecycleManager getLifecycleManager();

  MuleRegistry getRegistry();

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
  public StreamCloserService getStreamCloserService();

  /**
   * @deprecated as of 3.7.0. This will be removed in Mule 4.0
   */
  @Deprecated
  void addRegistry(Registry registry);

  /**
   * @deprecated as of 3.7.0. This will be removed in Mule 4.0
   */
  @Deprecated
  void removeRegistry(Registry registry);

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

  MuleClient getClient();

  SystemExceptionHandler getExceptionListener();

  void setExceptionListener(SystemExceptionHandler exceptionListener);

  void setObjectStore(String name, ListableObjectStore<Serializable> store) throws RegistrationException;

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
   * Return all annotations seen in the configuration
   */
  Map<QName, Set<Object>> getConfigurationAnnotations();

  /**
   * @return default exception strategy. If no default exception strategy was configured it returns
   *         {@link org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy}
   */
  MessagingExceptionHandler getDefaultErrorHandler();

  /**
   * @return single resource transaction factory manager. Used to retrieve a transaction factory for each transactional resource
   *         (i.e jdbc DataSource, jms Connection)
   */
  SingleResourceTransactionFactoryManager getTransactionFactoryManager();

  /**
   * @return a non null {@link DataTypeConversionResolver} instance to resolve implicit data type
   *         conversions
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
   * Gets application wide instance of {@link TransformationService} used for applying
   * {@link org.mule.runtime.core.api.transformer.Transformer}'s to a {@link Message} and for obtaining different representations
   * of the message payload.
   *
   * @return transformation service
   */
  TransformationService getTransformationService();

  /**
   * Sets application wide instance of {@link TransformationService}
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
   * @return a locator for discovering {@link org.mule.runtime.api.message.ErrorType}s related to exceptions and components.
   */
  ErrorTypeLocator getErrorTypeLocator();

  /**
   * @return an error type repository to get access to the {@link org.mule.runtime.api.message.ErrorType} instances of this
   *         artifact.
   */
  ErrorTypeRepository getErrorTypeRepository();

  // TODO MULE-11521 Define if this will remain here
  ProcessorInterceptorProvider getProcessorInterceptorManager();

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

}

