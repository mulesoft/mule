/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context;

import static org.mule.runtime.api.config.MuleRuntimeFeature.BATCH_FIXED_AGGREGATOR_TRANSACTION_RECORD_BUFFER;
import static org.mule.runtime.api.config.MuleRuntimeFeature.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_BYTE_BUDDY_OBJECT_CREATION;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_ERROR_MAPPINGS_WHEN_POLICY_APPLIED_ON_OPERATION;
import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_RESERVED_PROPERTIES;
import static org.mule.runtime.api.config.MuleRuntimeFeature.PARALLEL_FOREACH_FLATTEN_MESSAGE;
import static org.mule.runtime.api.config.MuleRuntimeFeature.SET_VARIABLE_WITH_NULL_VALUE;
import static org.mule.runtime.api.config.MuleRuntimeFeature.START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER;
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
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
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
import static org.mule.runtime.core.api.management.stats.AllStatistics.configureComputeConnectionErrorsInStats;
import static org.mule.runtime.core.api.util.UUID.getClusterUUID;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.getSupportedJdks;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.apache.commons.lang3.SystemUtils.JAVA_VERSION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.MuleRuntimeFeature;
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
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.EventContextService;
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
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.config.ClusterConfiguration;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.runtime.core.internal.config.NullClusterConfiguration;
import org.mule.runtime.core.internal.connector.DefaultSchedulerController;
import org.mule.runtime.core.internal.connector.SchedulerController;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.ErrorHandlerFactory;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.LifecycleStrategy;
import org.mule.runtime.core.internal.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.runtime.core.internal.util.splash.ArtifactShutdownSplashScreen;
import org.mule.runtime.core.internal.util.splash.ArtifactStartupSplashScreen;
import org.mule.runtime.core.internal.util.splash.ServerShutdownSplashScreen;
import org.mule.runtime.core.internal.util.splash.ServerStartupSplashScreen;
import org.mule.runtime.core.internal.util.splash.SplashScreen;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import reactor.core.publisher.Hooks;

public class DefaultMuleContext implements MuleContextWithRegistry, PrivilegedMuleContext {

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
  private static Logger LOGGER = getLogger(DefaultMuleContext.class);

  private final CustomizationService customizationService = new DefaultCustomizationService();

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
  private final AllStatistics stats = new AllStatistics();

  private volatile SchedulerService schedulerService;

  /**
   * LifecycleManager for the MuleContext. Note: this is NOT the same lifecycle manager as the one in the Registry.
   */
  private MuleContextLifecycleManager lifecycleManager;
  private final Object lifecycleStateLock = new Object();

  private ServerNotificationManager notificationManager;

  private MuleConfiguration config;
  private String id;

  /**
   * the date in milliseconds from when the server was started
   */
  private long startDate;

  private volatile StreamCloserService streamCloserService;
  private final Object streamCloserServiceLock = new Object();

  private ClassLoader executionClassLoader;

  /**
   * Global exception handler which handles "system" exceptions (i.e., when no message is involved).
   */
  protected SystemExceptionHandler exceptionListener;

  private SchedulerController schedulerController = new DefaultSchedulerController();

  private ClusterConfiguration clusterConfiguration = new NullClusterConfiguration();
  private String clusterNodeIdPrefix = "";

  private final SingleResourceTransactionFactoryManager singleResourceTransactionFactoryManager =
      new SingleResourceTransactionFactoryManager();

  private LockFactory lockFactory;

  private ExtendedExpressionManager expressionManager;

  private ProcessingTimeWatcher processingTimeWatcher;

  private final Latch startLatch = new Latch();

  private QueueManager queueManager;

  private ExtensionManager extensionManager;
  private SecurityManager securityManager;

  private ObjectSerializer objectSerializer;
  private volatile DataTypeConversionResolver dataTypeConversionResolver;
  private final Object dataTypeConversionResolverLock = new Object();

  private volatile FlowTraceManager flowTraceManager;
  private final Object flowTraceManagerLock = new Object();

  private volatile EventContextService eventContextService;
  private final Object eventContextServiceLock = new Object();

  private volatile Collection<ExceptionContextProvider> exceptionContextProviders;
  private final Object exceptionContextProvidersLock = new Object();

  private TransformationService transformationService;

  private BootstrapServiceDiscoverer bootstrapServiceDiscoverer;

  private Properties deploymentProperties;

  private List<MuleContextListener> listeners = new CopyOnWriteArrayList<>();

  private final LifecycleInterceptor lifecycleInterceptor = new MuleLifecycleInterceptor();

  @Inject
  private ComponentInitialStateManager componentInitialStateManager;

  /**
   * The {@link ArtifactType} indicating if this configuration object is for an application or a domain.
   */
  private ArtifactType artifactType;

  private ErrorTypeLocator errorTypeLocator;
  private ErrorTypeRepository errorTypeRepository;

  private ConfigurationComponentLocator componentLocator;

  private LifecycleStrategy lifecycleStrategy = new DefaultLifecycleStrategy();

  private static final AtomicBoolean areFeatureFlagsConfigured = new AtomicBoolean();


  static {
    // Log dropped events/errors
    Hooks.onErrorDropped(error -> LOGGER.debug("ERROR DROPPED", error));
    Hooks.onNextDropped(event -> LOGGER.debug("EVENT DROPPED {}", event));
    // Feature flags (see FeatureFlaggingService)
    if (!areFeatureFlagsConfigured.getAndSet(true)) {
      configurePropertiesResolverFeatureFlag();
      configureBatchFixedAggregatorTransactionRecordBuffer();
      configureComputeConnectionErrorsInStats();
      configureEnablePolicyIsolation();
      configureSetVariableWithNullVale();
      configureStartExtensionComponentsWithArtifactClassloader();
      configureDefaultErrorHandlerNotRollbackingEveryTx();
      configureParallelForeachFlattenMessage();
      configureEnableByteBuddyObjectCreation();
      configureHonourErrorMappingsWhenPolicyAppliedOnOperation();
    }
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
        final org.mule.runtime.api.artifact.Registry apiRegistry = getApiRegistry();
        listeners.forEach(l -> {
          l.onInitialization(this, apiRegistry);
        });

        lifecycleStrategy.initialise(this);

      } catch (InitialisationException e) {
        dispose();
        throw e;
      } catch (Exception e) {
        dispose();
        throw new InitialisationException(e, this);
      }
    }
  }

  private class DefaultLifecycleStrategy implements LifecycleStrategy {

    @Override
    public void initialise(Initialisable initialisable) throws InitialisationException {
      initialiseIfNeeded(getExceptionListener(), true, DefaultMuleContext.this);

      getNotificationManager().initialise();

      // refresh object serializer reference in case a default one was redefined in the config.
      objectSerializer = muleRegistryHelper.get(DEFAULT_OBJECT_SERIALIZER_NAME);
    }

    @Override
    public void start(Startable startable) throws MuleException {
      if (getQueueManager() == null) {
        throw new MuleRuntimeException(objectIsNull("queueManager"));
      }

      componentInitialStateManager = muleRegistryHelper.get(OBJECT_COMPONENT_INITIAL_STATE_MANAGER);

      overridePollingController();
      overrideClusterConfiguration();
      startMessageSources();
    }
  }

  @Override
  public void start() throws MuleException {
    synchronized (lifecycleStateLock) {
      getLifecycleManager().checkPhase(Startable.PHASE_NAME);

      startDate = System.currentTimeMillis();

      fireNotification(new MuleContextNotification(this, CONTEXT_STARTING));
      getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);

      lifecycleStrategy.start(this);

      fireNotification(new MuleContextNotification(this, CONTEXT_STARTED));
      final org.mule.runtime.api.artifact.Registry apiRegistry = getApiRegistry();
      listeners.forEach(l -> l.onStart(this, apiRegistry));

      startLatch.release();

      if (LOGGER.isInfoEnabled()) {
        SplashScreen startupScreen = buildStartupSplash();
        log(startupScreen.toString());
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

      getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);
      fireNotification(new MuleContextNotification(this, CONTEXT_STOPPING));
      getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);

      lifecycleStrategy.stop(this);

      fireNotification(new MuleContextNotification(this, CONTEXT_STOPPED));

      final org.mule.runtime.api.artifact.Registry apiRegistry = getApiRegistry();
      listeners.forEach(l -> l.onStop(this, apiRegistry));
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
          LOGGER.error("Failed to stop Mule context", e);
        }
      }

      getLifecycleManager().checkPhase(Disposable.PHASE_NAME);

      fireNotification(new MuleContextNotification(this, CONTEXT_DISPOSING));

      lifecycleStrategy.dispose(this);

      disposeIfNeeded(getExceptionListener(), LOGGER);

      try {
        getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);

        // THis is a little odd. I find the relationship between the MuleRegistry Helper and the registry broker, too much
        // abstraction?
        if (muleRegistryHelper != null) {
          safely(() -> muleRegistryHelper.dispose());
        }
      } catch (Exception e) {
        LOGGER.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
      }

      notificationManager.fireNotification(new MuleContextNotification(this, CONTEXT_DISPOSED));

      disposeManagers();

      if ((getStartDate() > 0) && LOGGER.isInfoEnabled()) {
        SplashScreen shutdownScreen = buildShutdownSplash();
        log(shutdownScreen.toString());
      }

      setExecutionClassLoader(null);
    }
  }

  private void disposeManagers() {
    safely(() -> {
      disposeIfNeeded(getFlowTraceManager(), LOGGER);
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
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("MuleEvent Manager is not enabled, ignoring notification: " + notification);
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
      muleRegistryHelper.registerObject(OBJECT_SECURITY_MANAGER, securityManager);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
    this.securityManager = securityManager;
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
    if (config.isLazyInit()) {
      return fetchSecurityManager();
    }

    if (securityManager == null) {
      this.securityManager = fetchSecurityManager();
    }

    return securityManager;
  }

  private SecurityManager fetchSecurityManager() {
    SecurityManager securityManager = muleRegistryHelper.lookupObject(OBJECT_SECURITY_MANAGER);
    if (securityManager == null) {
      Collection<SecurityManager> temp = muleRegistryHelper.lookupObjects(SecurityManager.class);
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
      queueManager = muleRegistryHelper.lookupObject(OBJECT_QUEUE_MANAGER);
      if (queueManager == null) {
        Collection<QueueManager> temp = muleRegistryHelper.lookupObjects(QueueManager.class);
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
    muleRegistryHelper.registerObject(OBJECT_TRANSACTION_MANAGER, manager);
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

  @Override
  public void setRegistry(Registry registry) {
    if (!(registry instanceof MuleRegistryHelper)) {
      registry = new MuleRegistryHelper(registry, this);
    }
    muleRegistryHelper = (MuleRegistryHelper) registry;
  }

  public void setLifecycleStrategy(LifecycleStrategy lifecycleStrategy) {
    this.lifecycleStrategy = lifecycleStrategy;
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

  private SplashScreen buildStartupSplash() {
    SplashScreen startupScreen =
        config.isContainerMode() ? new ArtifactStartupSplashScreen() : new ServerStartupSplashScreen();
    startupScreen.setHeader(this);
    startupScreen.setFooter(this);
    return startupScreen;
  }

  private SplashScreen buildShutdownSplash() {
    SplashScreen shutdownScreen =
        config.isContainerMode() ? new ArtifactShutdownSplashScreen() : new ServerShutdownSplashScreen();
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
    return getClusterUUID(clusterNodeIdPrefix);
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

      if (rootContainerName.isPresent()) {
        defaultErrorHandler = ((GlobalErrorHandler) defaultErrorHandler)
            .createLocalErrorHandler(Location.builder().globalName(rootContainerName.get()).build());
      } else {
        try {
          defaultErrorHandler = new ErrorHandlerFactory().createDefault(getRegistry().lookupObject(NotificationDispatcher.class));
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
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
      expressionManager = muleRegistryHelper.lookupObject(OBJECT_EXPRESSION_MANAGER);
    }
    return expressionManager;
  }

  @Override
  public LockFactory getLockFactory() {
    if (this.lockFactory == null) {
      this.lockFactory = muleRegistryHelper.get(OBJECT_LOCK_FACTORY);
    }
    return this.lockFactory;
  }

  @Override
  public ProcessingTimeWatcher getProcessorTimeWatcher() {
    if (this.processingTimeWatcher == null) {
      this.processingTimeWatcher = muleRegistryHelper.get(OBJECT_PROCESSING_TIME_WATCHER);
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
      this.clusterNodeIdPrefix = overriddenClusterConfiguration.getClusterNodeId() + "-";
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

  public void setInjector(Injector injector) {
    this.injector = injector;
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
  public EventContextService getEventContextService() {
    if (eventContextService == null) {
      synchronized (eventContextServiceLock) {
        if (eventContextService == null) {
          try {
            eventContextService = getRegistry().lookupObject(EventContextService.class);
          } catch (RegistrationException e) {
            // Should not occur
            throw new IllegalStateException("Could not get 'EventContextService' instance from registry.", e);
          }
        }
      }
    }
    return eventContextService;
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

  public void setListeners(List<MuleContextListener> listeners) {
    this.listeners = new CopyOnWriteArrayList<>(listeners);
  }

  /**
   * Registers the given {@code listener}
   *
   * @param listener a {@link MuleContextListener}
   * @since 4.3.0
   */
  public void addListener(MuleContextListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the given {@code listener}
   *
   * @param listener a {@link MuleContextListener}
   * @since 4.3.0
   */
  public void removeListener(MuleContextListener listener) {
    listeners.remove(listener);
  }

  private org.mule.runtime.api.artifact.Registry getApiRegistry() {
    return getRegistry().lookupObject(OBJECT_REGISTRY);
  }

  private static void configureBatchFixedAggregatorTransactionRecordBuffer() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(BATCH_FIXED_AGGREGATOR_TRANSACTION_RECORD_BUFFER, featureContext -> false);
  }

  /**
   * Configures {@link org.mule.runtime.core.api.config.FeatureFlaggingService} to revert MULE-17659 for applications with <code>minMuleVersion</code> lesser than
   * or equal to 4.2.2, or if system property {@link MuleRuntimeFeature#HONOUR_RESERVED_PROPERTIES} is set. See MULE-17659 and
   * MULE-19038.
   *
   * @since 4.4.0 4.3.0
   */
  public static void configurePropertiesResolverFeatureFlag() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(HONOUR_RESERVED_PROPERTIES, featureContext -> featureContext
        .getArtifactMinMuleVersion().filter(muleVersion -> muleVersion.newerThan("4.2.2")).isPresent());
  }

  private static void configureEnablePolicyIsolation() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(ENABLE_POLICY_ISOLATION, featureContext -> featureContext
        .getArtifactMinMuleVersion().filter(muleVersion -> muleVersion.atLeast("4.4.0")).isPresent());
  }

  /**
   * Configures {@link FeatureFlaggingService} to revert MULE-19443 for applications with <code>minMuleVersion</code> lesser than
   * 4.4.0.
   *
   * @since 4.4.0
   */
  private static void configureSetVariableWithNullVale() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(SET_VARIABLE_WITH_NULL_VALUE, featureContext -> featureContext
        .getArtifactMinMuleVersion().filter(muleVersion -> muleVersion.atLeast("4.4.0")).isPresent());
  }

  /**
   * Configures the {@link MuleRuntimeFeature#START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER} feature flag.
   *
   * @since 4.4.0
   */
  private static void configureStartExtensionComponentsWithArtifactClassloader() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER,
                                                featureContext -> featureContext
                                                    .getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion.atLeast("4.4.0")).isPresent());
  }

  /**
   * Configures the {@link MuleRuntimeFeature#DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING} feature flag.
   *
   * @since 4.5.0, 4.4.1, 4.3.1
   */
  private static void configureDefaultErrorHandlerNotRollbackingEveryTx() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING,
                                                featureContext -> featureContext.getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion.atLeast("4.3.1"))
                                                    .isPresent());
  }

  /**
   * Configures the {@link MuleRuntimeFeature#PARALLEL_FOREACH_FLATTEN_MESSAGE} feature flag.
   *
   * @since 4.3.0-202203
   */
  private static void configureParallelForeachFlattenMessage() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(PARALLEL_FOREACH_FLATTEN_MESSAGE,
                                                featureContext -> featureContext.getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion.atLeast("4.5.0")).isPresent());
  }

  /**
   * Configures the {@link MuleRuntimeFeature#ENABLE_BYTE_BUDDY_OBJECT_CREATION} feature flag.
   *
   * @since 4.3.0-202203
   */
  private static void configureEnableByteBuddyObjectCreation() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(ENABLE_BYTE_BUDDY_OBJECT_CREATION,
                                                featureContext -> featureContext.getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion.atLeast("4.5.0")).isPresent());
  }

  /**
   * Configures the {@link MuleRuntimeFeature#HONOUR_ERROR_MAPPINGS_WHEN_POLICY_APPLIED_ON_OPERATION} feature flag.
   *
   * @since 4.5.0, 4.4.0-202207, 4.3.0-202207
   */
  private static void configureHonourErrorMappingsWhenPolicyAppliedOnOperation() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(HONOUR_ERROR_MAPPINGS_WHEN_POLICY_APPLIED_ON_OPERATION,
                                                featureContext -> featureContext.getArtifactMinMuleVersion()
                                                    .filter(muleVersion -> muleVersion.atLeast("4.5.0")).isPresent());
  }

}
