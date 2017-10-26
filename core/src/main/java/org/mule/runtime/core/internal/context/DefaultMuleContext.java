/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.SystemUtils.JAVA_VERSION;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_COMPONENT_INITIAL_STATE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLLING_CONTROLLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMATION_SERVICE;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.invalidJdk;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSING;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_INITIALISED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_INITIALISING;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTING;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPING;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.getSupportedJdks;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
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
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.config.ClusterConfiguration;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.runtime.core.internal.config.NullClusterConfiguration;
import org.mule.runtime.core.internal.connector.DefaultSchedulerController;
import org.mule.runtime.core.internal.connector.SchedulerController;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.ErrorHandlerFactory;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.internal.registry.RegistryBroker;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.runtime.core.internal.util.splash.ApplicationShutdownSplashScreen;
import org.mule.runtime.core.internal.util.splash.ApplicationStartupSplashScreen;
import org.mule.runtime.core.internal.util.splash.ServerShutdownSplashScreen;
import org.mule.runtime.core.internal.util.splash.ServerStartupSplashScreen;
import org.mule.runtime.core.internal.util.splash.SplashScreen;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import reactor.core.publisher.Hooks;

public class DefaultMuleContext implements MuleContextWithRegistries, PrivilegedMuleContext {

  /**
   * TODO: Remove these constants. These constants only make sense until we have a reliable solution for durable persistence in
   * Clustering. These are not part of Mule's API and you should not use them in applications or extensions
   */
  public static final String LOCAL_OBJECT_STORE_MANAGER_KEY = LOCAL_OBJECT_STORE_MANAGER;
  public static final String LOCAL_QUEUE_MANAGER_KEY = "_localQueueManager";

  public static final ThreadLocal<MuleContext> currentMuleContext = new ThreadLocal<>();

  /**
   * logger used by this class
   */
  private static Logger logger = getLogger(DefaultMuleContext.class);

  private CustomizationService customizationService = new DefaultCustomizationService();

  /**
   * Internal registry facade which delegates to other registries.
   */
  private RegistryBroker registryBroker;

  /**
   * Simplified Mule configuration interface
   */
  private MuleRegistry muleRegistryHelper;

  /**
   * Default component to perform dependency injection
   *
   * @since 3.7.0
   */
  private Injector injector;

  /**
   * stats used for management
   */
  private AllStatistics stats = new AllStatistics();

  private volatile SchedulerService schedulerService;

  /**
   * LifecycleManager for the MuleContext. Note: this is NOT the same lifecycle manager as the one in the Registry.
   */
  private MuleContextLifecycleManager lifecycleManager;
  private Object lifecycleStateLock = new Object();

  private ServerNotificationManager notificationManager;

  private MuleConfiguration config;
  private String id;

  /**
   * the date in milliseconds from when the server was started
   */
  private long startDate;

  private volatile StreamCloserService streamCloserService;
  private Object streamCloserServiceLock = new Object();

  private ClassLoader executionClassLoader;

  /**
   * Global exception handler which handles "system" exceptions (i.e., when no message is involved).
   */
  protected SystemExceptionHandler exceptionListener;

  private SchedulerController schedulerController = new DefaultSchedulerController();

  private ClusterConfiguration clusterConfiguration = new NullClusterConfiguration();

  private SingleResourceTransactionFactoryManager singleResourceTransactionFactoryManager =
      new SingleResourceTransactionFactoryManager();

  private LockFactory lockFactory;

  private ExtendedExpressionManager expressionManager;

  private ProcessingTimeWatcher processingTimeWatcher;

  private final Latch startLatch = new Latch();

  private QueueManager queueManager;

  private ExtensionManager extensionManager;

  private ObjectSerializer objectSerializer;
  private volatile DataTypeConversionResolver dataTypeConversionResolver;
  private Object dataTypeConversionResolverLock = new Object();

  private volatile FlowTraceManager flowTraceManager;
  private Object flowTraceManagerLock = new Object();

  private volatile Collection<ExceptionContextProvider> exceptionContextProviders;
  private Object exceptionContextProvidersLock = new Object();

  private TransformationService transformationService;

  private BootstrapServiceDiscoverer bootstrapServiceDiscoverer;

  private Properties deploymentProperties;

  private LifecycleInterceptor lifecycleInterceptor = new MuleLifecycleInterceptor();

  @Inject
  private ComponentInitialStateManager componentInitialStateManager;

  /**
   * The {@link ArtifactType} indicating if this configuration object is for an application or a domain.
   */
  private ArtifactType artifactType;

  private ErrorTypeLocator errorTypeLocator;
  private ErrorTypeRepository errorTypeRepository;

  private ConfigurationComponentLocator componentLocator;

  static {
    // Log dropped events/errors
    Hooks.onErrorDropped(error -> logger.debug("ERROR DROPPED " + error));
    Hooks.onNextDropped(event -> logger.debug("EVENT DROPPED " + event));
  }

  public DefaultMuleContext() {
    transformationService = new ExtendedTransformationService(this);
  }

  @Override
  public void initialise() throws InitialisationException {
    synchronized (lifecycleStateLock) {
      lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

      if (getNotificationManager() == null) {
        throw new MuleRuntimeException(objectIsNull(OBJECT_NOTIFICATION_MANAGER));
      }

      try {
        JdkVersionUtils.validateJdk();
      } catch (RuntimeException e) {
        throw new InitialisationException(invalidJdk(JAVA_VERSION, getSupportedJdks()), this);
      }

      try {
        id = getConfiguration().getDomainId() + "." + getClusterId() + "." + getConfiguration().getId();

        // Initialize the helper, this only initialises the helper class and does not call the registry lifecycle manager
        // The registry lifecycle is called below using 'getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);'
        getRegistry().initialise();

        fireNotification(new MuleContextNotification(this, CONTEXT_INITIALISING));
        getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, CONTEXT_INITIALISED));

        initialiseIfNeeded(getExceptionListener(), true, this);

        getNotificationManager().initialise();

        // refresh object serializer reference in case a default one was redefined in the config.
        objectSerializer = registryBroker.get(DEFAULT_OBJECT_SERIALIZER_NAME);
      } catch (InitialisationException e) {
        dispose();
        throw e;
      } catch (Exception e) {
        dispose();
        throw new InitialisationException(e, this);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    synchronized (lifecycleStateLock) {
      getLifecycleManager().checkPhase(Startable.PHASE_NAME);

      if (getQueueManager() == null) {
        throw new MuleRuntimeException(objectIsNull("queueManager"));
      }

      componentInitialStateManager = muleRegistryHelper.get(OBJECT_COMPONENT_INITIAL_STATE_MANAGER);
      startDate = System.currentTimeMillis();

      startIfNeeded(extensionManager);
      fireNotification(new MuleContextNotification(this, CONTEXT_STARTING));
      getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);
      overridePollingController();
      overrideClusterConfiguration();
      startMessageSources();

      fireNotification(new MuleContextNotification(this, CONTEXT_STARTED));

      startLatch.release();

      if (logger.isInfoEnabled()) {
        SplashScreen startupScreen = buildStartupSplash();
        logger.info(startupScreen.toString());
      }
    }
  }

  private void startMessageSources() throws LifecycleException {
    startPipelineMessageSources();
  }

  private void startPipelineMessageSources() throws LifecycleException {
    for (Pipeline pipeline : this.getRegistry().lookupObjectsForLifecycle(Pipeline.class)) {
      if (pipeline.getLifecycleState().isStarted()) {
        MessageSource messageSource = pipeline.getSource();
        if (messageSource != null && componentInitialStateManager.mustStartMessageSource(messageSource)) {
          startMessageSource(messageSource);
        }
      }
    }
  }

  private void startMessageSource(MessageSource messageSource) throws LifecycleException {
    try {
      startIfNeeded(messageSource);
    } catch (ConnectException e) {
      exceptionListener.handleException(e);
    } catch (LifecycleException le) {
      throw le;
    } catch (Exception e) {
      throw new LifecycleException(e, messageSource);
    }
  }

  /**
   * Stops the <code>MuleContext</code> which stops all sessions and connectors
   *
   * @throws MuleException if either any of the sessions or connectors fail to stop
   */
  @Override
  public void stop() throws MuleException {
    synchronized (lifecycleStateLock) {
      startLatch.release();

      stopIfNeeded(extensionManager);
      getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);
      fireNotification(new MuleContextNotification(this, CONTEXT_STOPPING));
      getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
      fireNotification(new MuleContextNotification(this, CONTEXT_STOPPED));
    }
  }

  @Override
  public void dispose() {
    synchronized (lifecycleStateLock) {
      if (isStarted() || (lifecycleManager.getLastPhaseExecuted() != null
          && (lifecycleManager.getLastPhaseExecuted().equals(Startable.PHASE_NAME)
              && lifecycleManager.isLastPhaseExecutionFailed()))) {
        try {
          stop();
        } catch (MuleException e) {
          logger.error("Failed to stop Mule context", e);
        }
      }

      getLifecycleManager().checkPhase(Disposable.PHASE_NAME);

      fireNotification(new MuleContextNotification(this, CONTEXT_DISPOSING));

      disposeIfNeeded(getExceptionListener(), logger);

      try {
        getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);

        // THis is a little odd. I find the relationship between the MuleRegistry Helper and the registry broker, too much
        // abstraction?
        if (muleRegistryHelper != null) {
          safely(() -> muleRegistryHelper.dispose());
        }
      } catch (Exception e) {
        logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
      }

      notificationManager.fireNotification(new MuleContextNotification(this, CONTEXT_DISPOSED));

      disposeManagers();

      if ((getStartDate() > 0) && logger.isInfoEnabled()) {
        SplashScreen shutdownScreen = buildShutdownSplash();
        logger.info(shutdownScreen.toString());
      }

      // registryBroker.dispose();

      setExecutionClassLoader(null);
    }
  }

  private void disposeManagers() {
    safely(() -> {
      disposeIfNeeded(getFlowTraceManager(), logger);
      notificationManager.dispose();
    });
  }

  /**
   * Determines if the server has been initialised
   *
   * @return true if the server has been initialised
   */
  @Override
  public boolean isInitialised() {
    return getLifecycleManager().getState().isInitialised();
  }

  /**
   * Determines if the server is being initialised
   *
   * @return true if the server is beening initialised
   */
  @Override
  public boolean isInitialising() {
    return getLifecycleManager().getState().isInitialising();
  }

  @Override
  public boolean isStopped() {
    return getLifecycleManager().getState().isStopped();
  }

  @Override
  public boolean isStopping() {
    return getLifecycleManager().getState().isStopping();
  }

  /**
   * Determines if the server has been started
   *
   * @return true if the server has been started
   */
  @Override
  public boolean isStarted() {
    return getLifecycleManager().isPhaseComplete(Startable.PHASE_NAME);
  }

  @Override
  public boolean isStarting() {
    return getLifecycleManager().getState().isStarting();
  }

  @Override
  public boolean isDisposed() {
    return getLifecycleManager().getState().isDisposed();
  }

  @Override
  public boolean isDisposing() {
    return getLifecycleManager().getState().isDisposing();
  }

  @Override
  public void withLifecycleLock(Runnable command) {
    synchronized (lifecycleStateLock) {
      command.run();
    }
  }

  @Override
  public LifecycleManager getLifecycleManager() {
    return lifecycleManager;
  }

  /**
   * Gets all statistics for this instance
   *
   * @return all statistics for this instance
   */
  @Override
  public AllStatistics getStatistics() {
    return stats;
  }

  /**
   * Fires a server notification to all registered {@link CustomNotificationListener} notificationManager.
   *
   * @param notification the notification to fire. This must be of type {@link CustomNotification} otherwise an exception will be
   *        thrown.
   * @throws UnsupportedOperationException if the notification fired is not a {@link CustomNotification}
   */
  private void fireNotification(AbstractServerNotification notification) {
    ServerNotificationManager notificationManager = getNotificationManager();
    if (notificationManager != null) {
      notificationManager.fireNotification(notification);
    } else if (logger.isDebugEnabled()) {
      logger.debug("MuleEvent Manager is not enabled, ignoring notification: " + notification);
    }
  }

  /**
   * Sets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @param securityManager the security manager used by this Mule instance to authenticate and authorise incoming and outgoing
   *        event traffic and service invocations
   */
  @Override
  public void setSecurityManager(SecurityManager securityManager) {
    checkLifecycleForPropertySet(OBJECT_SECURITY_MANAGER, Initialisable.PHASE_NAME);
    try {
      registryBroker.registerObject(OBJECT_SECURITY_MANAGER, securityManager);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Gets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @return he security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   *         service invocations
   */
  @Override
  public SecurityManager getSecurityManager() {
    SecurityManager securityManager = registryBroker.lookupObject(OBJECT_SECURITY_MANAGER);
    if (securityManager == null) {
      Collection<SecurityManager> temp = registryBroker.lookupObjects(SecurityManager.class);
      if (temp.size() > 0) {
        securityManager = (temp.iterator().next());
      }
    }
    if (securityManager == null) {
      throw new MuleRuntimeException(objectIsNull("securityManager"));
    }
    return securityManager;
  }

  @Override
  public SchedulerService getSchedulerService() {
    if (this.schedulerService == null) {
      try {
        this.schedulerService = this.getRegistry().lookupObject(SchedulerService.class);
        requireNonNull(schedulerService);
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }

    return this.schedulerService;
  }

  @Override
  public SchedulerConfig getSchedulerBaseConfig() {
    return this.getRegistry().lookupObject(OBJECT_SCHEDULER_BASE_CONFIG);
  }

  @Override
  public QueueManager getQueueManager() {
    if (queueManager == null) {
      queueManager = registryBroker.lookupObject(OBJECT_QUEUE_MANAGER);
      if (queueManager == null) {
        Collection<QueueManager> temp = registryBroker.lookupObjects(QueueManager.class);
        if (temp.size() > 0) {
          queueManager = temp.iterator().next();
        }
      }
    }
    return queueManager;
  }

  @Override
  public ExtensionManager getExtensionManager() {
    return extensionManager;
  }

  public LifecycleInterceptor getLifecycleInterceptor() {
    return lifecycleInterceptor;
  }

  @Override
  public ObjectStoreManager getObjectStoreManager() {
    return this.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectSerializer getObjectSerializer() {
    return objectSerializer;
  }

  /**
   * When running in clustered mode, it returns a {@link ObjectStoreManager} that creates {@link ObjectStore} instances which are
   * only local to the current node. This is just a workaround until we introduce a solution for durable persistent stores in HA.
   * This is not part of Mule's API and you should not use this in your apps or extensions
   *
   * @return a {@link ObjectStoreManager}
   * @since 3.5.0
   */
  public ObjectStoreManager getLocalObjectStoreManager() {
    return this.getRegistry().lookupObject(LOCAL_OBJECT_STORE_MANAGER_KEY);
  }

  /**
   * When running in clustered mode, it returns a {@link QueueManager} that creates {@link Queue} instances which are only local
   * to the current node. This is just a workaround until we introduce a solution for durable persistent queues in HA. This is not
   * part of Mule's API and you should not use this in your apps or extensions
   *
   * @return a {@link QueueManager}
   * @since 3.5.0
   */
  public QueueManager getLocalQueueManager() {
    return this.getRegistry().lookupObject(LOCAL_QUEUE_MANAGER_KEY);
  }

  @Override
  public void setQueueManager(QueueManager queueManager) {
    try {
      getRegistry().registerObject(OBJECT_QUEUE_MANAGER, queueManager);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
    this.queueManager = queueManager;
  }

  /**
   * @return the MuleConfiguration for this MuleManager. This object is immutable once the manager has initialised.
   */
  @Override
  public MuleConfiguration getConfiguration() {
    return config;
  }

  @Override
  public ServerNotificationManager getNotificationManager() {
    return notificationManager;
  }

  /**
   * Sets the JTA Transaction Manager to use with this Mule server instance
   *
   * @param manager the manager to use
   * @throws RegistrationException if a transaction manager has already been set
   */
  @Override
  public void setTransactionManager(TransactionManager manager) throws RegistrationException {
    // checkLifecycleForPropertySet(MuleProperties.OBJECT_TRANSACTION_MANAGER, Initialisable.PHASE_NAME);
    registryBroker.registerObject(OBJECT_TRANSACTION_MANAGER, manager);
  }

  /**
   * Returns the Jta transaction manager used by this Mule server instance. or null if a transaction manager has not been set
   *
   * @return the Jta transaction manager used by this Mule server instance. or null if a transaction manager has not been set
   */
  @Override
  public TransactionManager getTransactionManager() {
    return getRegistry().lookupObject(OBJECT_TRANSACTION_MANAGER);
  }

  protected void checkLifecycleForPropertySet(String propertyName, String phase) throws IllegalStateException {
    if (lifecycleManager.isPhaseComplete(phase)) {
      throw new IllegalStateException("Cannot set property: '" + propertyName + "' once the server has already been through the "
          + phase + " phase.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleRegistry getRegistry() {
    return muleRegistryHelper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Injector getInjector() {
    return injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamCloserService getStreamCloserService() {
    if (this.streamCloserService == null) {
      synchronized (streamCloserServiceLock) {
        if (this.streamCloserService == null) {
          this.streamCloserService = this.getRegistry().lookupObject(OBJECT_MULE_STREAM_CLOSER_SERVICE);
        }
      }
    }

    return this.streamCloserService;
  }

  /**
   * Returns the long date when the server was started
   *
   * @return the long date when the server was started
   */
  @Override
  public long getStartDate() {
    return startDate;
  }

  @Override
  public void setExecutionClassLoader(ClassLoader cl) {
    this.executionClassLoader = cl;
  }

  @Override
  public ClassLoader getExecutionClassLoader() {
    return executionClassLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public void addRegistry(Registry registry) {
    registryBroker.addRegistry(registry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public void removeRegistry(Registry registry) {
    registryBroker.removeRegistry(registry);
  }

  private SplashScreen buildStartupSplash() {
    SplashScreen startupScreen =
        config.isContainerMode() ? new ApplicationStartupSplashScreen() : new ServerStartupSplashScreen();
    startupScreen.setHeader(this);
    startupScreen.setFooter(this);
    return startupScreen;
  }

  private SplashScreen buildShutdownSplash() {
    SplashScreen shutdownScreen =
        config.isContainerMode() ? new ApplicationShutdownSplashScreen() : new ServerShutdownSplashScreen();
    shutdownScreen.setHeader(this);
    return shutdownScreen;
  }

  @Override
  public void handleException(Exception e, RollbackSourceCallback rollbackMethod) {
    getExceptionListener().handleException(e, rollbackMethod);
  }

  @Override
  public void handleException(Exception e) {
    handleException(e, null);
  }

  @Override
  public SystemExceptionHandler getExceptionListener() {
    return exceptionListener;
  }

  @Override
  public void setExceptionListener(SystemExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  @Override
  public String getClusterId() {
    return clusterConfiguration.getClusterId();
  }

  @Override
  public int getClusterNodeId() {
    return clusterConfiguration.getClusterNodeId();
  }

  public void setSchedulerController(SchedulerController schedulerController) {
    this.schedulerController = schedulerController;
  }

  @Override
  public boolean isPrimaryPollingInstance() {
    return schedulerController.isPrimarySchedulingInstance();
  }

  @Override
  public String getUniqueIdString() {
    return clusterConfiguration.getClusterNodeId() + "-" + UUID.getUUID();
  }

  @Override
  public FlowExceptionHandler getDefaultErrorHandler(Optional<String> rootContainerName) {
    FlowExceptionHandler defaultErrorHandler;
    if (config.getDefaultErrorHandlerName() != null) {
      defaultErrorHandler = getRegistry().lookupObject(config.getDefaultErrorHandlerName());
      if (defaultErrorHandler == null) {
        throw new MuleRuntimeException(createStaticMessage(format("No global error handler named %s",
                                                                  config.getDefaultErrorHandlerName())));
      }
    } else {
      try {
        defaultErrorHandler = new ErrorHandlerFactory().createDefault(getRegistry().lookupObject(NotificationDispatcher.class));
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }
    if (rootContainerName.isPresent() && defaultErrorHandler instanceof ErrorHandler) {
      ((ErrorHandler) defaultErrorHandler).setRootContainerName(rootContainerName.get());
    }
    return defaultErrorHandler;
  }

  @Override
  public SingleResourceTransactionFactoryManager getTransactionFactoryManager() {
    return this.singleResourceTransactionFactoryManager;
  }

  @Override
  public DataTypeConversionResolver getDataTypeConverterResolver() {
    if (dataTypeConversionResolver == null) {
      synchronized (dataTypeConversionResolverLock) {
        if (dataTypeConversionResolver == null) {
          dataTypeConversionResolver = getRegistry().lookupObject(OBJECT_CONVERTER_RESOLVER);

          if (dataTypeConversionResolver == null) {
            dataTypeConversionResolver = new DynamicDataTypeConversionResolver(this);

            try {
              getRegistry().registerObject(OBJECT_CONVERTER_RESOLVER, dataTypeConversionResolver);
            } catch (RegistrationException e) {
              // Should not occur
              throw new IllegalStateException(e);
            }
          }
        }
      }
    }

    return dataTypeConversionResolver;
  }

  @Override
  public ExtendedExpressionManager getExpressionManager() {
    if (expressionManager == null) {
      expressionManager = registryBroker.lookupObject(OBJECT_EXPRESSION_MANAGER);
    }
    return expressionManager;
  }

  @Override
  public LockFactory getLockFactory() {
    if (this.lockFactory == null) {
      this.lockFactory = registryBroker.get(OBJECT_LOCK_FACTORY);
    }
    return this.lockFactory;
  }

  @Override
  public ProcessingTimeWatcher getProcessorTimeWatcher() {
    if (this.processingTimeWatcher == null) {
      this.processingTimeWatcher = registryBroker.get(OBJECT_PROCESSING_TIME_WATCHER);
    }

    return this.processingTimeWatcher;
  }

  @Override
  public boolean waitUntilStarted(int timeout) throws InterruptedException {
    return startLatch.await(timeout, TimeUnit.MILLISECONDS);
  }

  private void overrideClusterConfiguration() {
    ClusterConfiguration overriddenClusterConfiguration = getRegistry().get(OBJECT_CLUSTER_CONFIGURATION);
    if (overriddenClusterConfiguration != null) {
      this.clusterConfiguration = overriddenClusterConfiguration;
    }
  }

  private void overridePollingController() {
    SchedulerController overriddenSchedulerController = getRegistry().get(OBJECT_POLLING_CONTROLLER);
    if (overriddenSchedulerController != null) {
      this.schedulerController = overriddenSchedulerController;
    }
  }

  public void setMuleConfiguration(MuleConfiguration muleConfiguration) {
    this.config = muleConfiguration;
  }

  public void setNotificationManager(ServerNotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }

  public void setLifecycleManager(LifecycleManager lifecycleManager) {
    // TODO(pablo.kraan): MULE-12609 - using LifecycleManager to avoid exposing MuleContextLifecycleManager
    if (!(lifecycleManager instanceof MuleContextLifecycleManager)) {
      I18nMessage msg = createStaticMessage("lifecycle manager for MuleContext must be a MuleContextLifecycleManager");
      throw new MuleRuntimeException(msg);
    }
    this.lifecycleManager = (MuleContextLifecycleManager) lifecycleManager;
  }

  public void setRegistryBroker(RegistryBroker registryBroker) {
    this.registryBroker = registryBroker;
  }

  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  public void setMuleRegistry(MuleRegistry muleRegistry) {
    this.muleRegistryHelper = muleRegistry;
  }

  @Override
  public void setExtensionManager(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  public void setObjectSerializer(ObjectSerializer objectSerializer) {
    this.objectSerializer = objectSerializer;
  }

  @Override
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  @Override
  public FlowTraceManager getFlowTraceManager() {
    if (flowTraceManager == null) {
      synchronized (flowTraceManagerLock) {
        if (flowTraceManager == null) {
          try {
            flowTraceManager = getRegistry().lookupObject(FlowTraceManager.class);
          } catch (RegistrationException e) {
            // Should not occur
            throw new IllegalStateException(e);
          }
        }
      }
    }
    return flowTraceManager;
  }

  @Override
  public Collection<ExceptionContextProvider> getExceptionContextProviders() {
    if (exceptionContextProviders == null) {
      synchronized (exceptionContextProvidersLock) {
        if (exceptionContextProviders == null) {
          exceptionContextProviders = this.muleRegistryHelper.lookupByType(ExceptionContextProvider.class).values();
        }
      }
    }
    return exceptionContextProviders;
  }

  @Override
  public TransformationService getTransformationService() {
    if (transformationService == null) {
      transformationService = getRegistry().get(OBJECT_TRANSFORMATION_SERVICE);
    }
    return transformationService;
  }

  @Override
  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  @Override
  public BootstrapServiceDiscoverer getRegistryBootstrapServiceDiscoverer() {
    return bootstrapServiceDiscoverer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CustomizationService getCustomizationService() {
    return customizationService;
  }

  @Override
  public void setBootstrapServiceDiscoverer(BootstrapServiceDiscoverer bootstrapServiceDiscoverer) {
    this.bootstrapServiceDiscoverer = bootstrapServiceDiscoverer;
  }

  @Override
  public ConfigurationComponentLocator getConfigurationComponentLocator() {
    if (componentLocator == null) {
      componentLocator = getRegistry().lookupObject(ConfigurationComponentLocator.REGISTRY_KEY);
    }
    return componentLocator;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setErrorTypeLocator(ErrorTypeLocator errorTypeLocator) {
    this.errorTypeLocator = errorTypeLocator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorTypeLocator getErrorTypeLocator() {
    return errorTypeLocator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorTypeRepository getErrorTypeRepository() {
    return errorTypeRepository;
  }

  public void setErrorTypeRepository(ErrorTypeRepository errorTypeRepository) {
    this.errorTypeRepository = errorTypeRepository;
  }

  @Override
  public Properties getDeploymentProperties() {
    return deploymentProperties;
  }

  /**
   * Sets the deployment properties so that beans as well as application properties are overridden.
   */
  public void setDeploymentProperties(Properties deploymentProperties) {
    this.deploymentProperties = deploymentProperties;
  }
}
