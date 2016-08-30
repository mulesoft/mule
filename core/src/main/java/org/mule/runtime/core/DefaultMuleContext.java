/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.lang.String.format;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLLING_CONTROLLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.config.spring.DefaultCustomizationService;
import org.mule.runtime.core.api.CustomizationService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.client.DefaultLocalMuleClient;
import org.mule.runtime.core.config.ClusterConfiguration;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.NullClusterConfiguration;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.DefaultPollingController;
import org.mule.runtime.core.connector.PollingController;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultSystemExceptionStrategy;
import org.mule.runtime.core.expression.DefaultExpressionManager;
import org.mule.runtime.core.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.management.stats.AllStatistics;
import org.mule.runtime.core.management.stats.ProcessingTimeWatcher;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.util.ApplicationShutdownSplashScreen;
import org.mule.runtime.core.util.ApplicationStartupSplashScreen;
import org.mule.runtime.core.util.JdkVersionUtils;
import org.mule.runtime.core.util.ServerShutdownSplashScreen;
import org.mule.runtime.core.util.ServerStartupSplashScreen;
import org.mule.runtime.core.util.SplashScreen;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.lock.LockFactory;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.extension.api.ExtensionManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleContext implements MuleContext {

  /**
   * TODO: Remove these constants. These constants only make sense until we have a reliable solution for durable persistence in
   * Clustering. These are not part of Mule's API and you should not use them in applications or extensions
   */
  public static final String LOCAL_TRANSIENT_OBJECT_STORE_KEY = "_localInMemoryObjectStore";
  public static final String LOCAL_PERSISTENT_OBJECT_STORE_KEY = "_localPersistentObjectStore";
  public static final String LOCAL_OBJECT_STORE_MANAGER_KEY = "_localObjectStoreManager";
  public static final String LOCAL_QUEUE_MANAGER_KEY = "_localQueueManager";

  /**
   * logger used by this class
   */
  private transient Logger logger = LoggerFactory.getLogger(DefaultMuleContext.class);

  private CustomizationService customizationService = new DefaultCustomizationService();

  /**
   * Internal registry facade which delegates to other registries.
   */
  private DefaultRegistryBroker registryBroker;

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

  private WorkManager workManager;

  private WorkListener workListener;

  /**
   * LifecycleManager for the MuleContext. Note: this is NOT the same lifecycle manager as the one in the Registry.
   */
  protected MuleContextLifecycleManager lifecycleManager;

  protected ServerNotificationManager notificationManager;

  private MuleConfiguration config;

  /**
   * the date in milliseconds from when the server was started
   */
  private long startDate;

  private ExpressionManager expressionManager;

  private volatile StreamCloserService streamCloserService;
  private Object streamCloserServiceLock = new Object();

  private ClassLoader executionClassLoader;

  protected MuleClient localMuleClient;

  /**
   * Global exception handler which handles "system" exceptions (i.e., when no message is involved).
   */
  protected SystemExceptionHandler exceptionListener;

  private PollingController pollingController = new DefaultPollingController();

  private ClusterConfiguration clusterConfiguration = new NullClusterConfiguration();

  private Map<QName, Set<Object>> configurationAnnotations = new HashMap<>();

  private SingleResourceTransactionFactoryManager singleResourceTransactionFactoryManager =
      new SingleResourceTransactionFactoryManager();

  private LockFactory lockFactory;

  private ExpressionLanguage expressionLanguage;

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

  /**
   * The {@link ArtifactType} indicating if this configuration object is for an application or a domain.
   */
  private ArtifactType artifactType;

  private ErrorTypeLocator errorTypeLocator;

  /**
   * @deprecated Use empty constructor instead and use setter for dependencies.
   */
  @Deprecated
  public DefaultMuleContext(MuleConfiguration config, WorkManager workManager, WorkListener workListener,
                            MuleContextLifecycleManager lifecycleManager, ServerNotificationManager notificationManager) {
    this.config = config;
    ((MuleContextAware) config).setMuleContext(this);
    this.workManager = workManager;
    this.workListener = workListener;
    this.lifecycleManager = lifecycleManager;
    this.notificationManager = notificationManager;
    this.notificationManager.setMuleContext(this);
    // there is no point having this object configurable
    this.expressionManager = new DefaultExpressionManager();
    ((MuleContextAware) this.expressionManager).setMuleContext(this);
    registryBroker = createRegistryBroker();
    muleRegistryHelper = createRegistryHelper(registryBroker);
    localMuleClient = new DefaultLocalMuleClient(this);
    exceptionListener = new DefaultSystemExceptionStrategy();
    transformationService = new TransformationService(this);
  }

  public DefaultMuleContext() {
    transformationService = new TransformationService(this);
  }

  protected DefaultRegistryBroker createRegistryBroker() {
    return new DefaultRegistryBroker(this);
  }

  protected MuleRegistry createRegistryHelper(DefaultRegistryBroker registry) {
    return new MuleRegistryHelper(registry, this);
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

    if (getNotificationManager() == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull(MuleProperties.OBJECT_NOTIFICATION_MANAGER));
    }
    if (workManager == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull("workManager"));
    }

    try {
      JdkVersionUtils.validateJdk();
    } catch (RuntimeException e) {
      throw new InitialisationException(CoreMessages.invalidJdk(SystemUtils.JAVA_VERSION, JdkVersionUtils.getSupportedJdks()),
                                        this);
    }

    try {
      // Initialize the helper, this only initialises the helper class and does not call the registry lifecycle manager
      // The registry lifecycle is called below using 'getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);'
      muleRegistryHelper.initialise();

      // We need to start the work manager straight away since we need it to fire notifications
      if (workManager instanceof MuleContextAware) {
        MuleContextAware contextAware = (MuleContextAware) workManager;
        contextAware.setMuleContext(this);
      }

      workManager.start();
      getNotificationManager().start(workManager, workListener);
      fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISING));
      getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);

      if (expressionManager instanceof Initialisable) {
        ((Initialisable) expressionManager).initialise();
      }

      fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISED));
    } catch (InitialisationException e) {
      throw e;
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public synchronized void start() throws MuleException {
    getLifecycleManager().checkPhase(Startable.PHASE_NAME);

    if (getQueueManager() == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull("queueManager"));
    }

    startDate = System.currentTimeMillis();

    startIfNeeded(extensionManager);
    fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTING));
    getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);
    overridePollingController();
    overrideClusterConfiguration();

    fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTED));

    startLatch.release();

    if (logger.isInfoEnabled()) {
      SplashScreen startupScreen = buildStartupSplash();
      logger.info(startupScreen.toString());
    }
  }

  /**
   * Stops the <code>MuleContext</code> which stops all sessions and connectors
   *
   * @throws MuleException if either any of the sessions or connectors fail to stop
   */
  @Override
  public synchronized void stop() throws MuleException {
    startLatch.release();

    stopIfNeeded(extensionManager);
    getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);
    fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPING));
    getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
    fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPED));
  }

  @Override
  public synchronized void dispose() {
    if (isStarted()) {
      try {
        stop();
      } catch (MuleException e) {
        logger.error("Failed to stop Mule context", e);
      }
    }

    getLifecycleManager().checkPhase(Disposable.PHASE_NAME);

    fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSING));

    try {
      getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);

      // THis is a little odd. I find the relationship between the MuleRegistry Helper and the registry broker, too much
      // abstraction?
      muleRegistryHelper.dispose();
    } catch (Exception e) {
      logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
    }

    notificationManager.fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSED));

    notificationManager.dispose();
    workManager.dispose();

    if (expressionManager != null && expressionManager instanceof Disposable) {
      ((Disposable) expressionManager).dispose();
    }

    if ((getStartDate() > 0) && logger.isInfoEnabled()) {
      SplashScreen shutdownScreen = buildShutdownSplash();
      logger.info(shutdownScreen.toString());
    }

    // registryBroker.dispose();

    setExecutionClassLoader(null);
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

  @Override
  public void registerListener(ServerNotificationListener l) throws NotificationException {
    registerListener(l, null);
  }

  @Override
  public void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException {
    ServerNotificationManager notificationManager = getNotificationManager();
    if (notificationManager == null) {
      throw new MuleRuntimeException(CoreMessages.serverNotificationManagerNotEnabled());
    }
    notificationManager.addListenerSubscription(l, resourceIdentifier);
  }

  @Override
  public void unregisterListener(ServerNotificationListener l) {
    ServerNotificationManager notificationManager = getNotificationManager();
    if (notificationManager != null) {
      notificationManager.removeListener(l);
    }
  }

  /**
   * Fires a server notification to all registered
   * {@link org.mule.runtime.core.api.context.notification.CustomNotificationListener} notificationManager.
   *
   * @param notification the notification to fire. This must be of type
   *        {@link org.mule.runtime.core.context.notification.CustomNotification} otherwise an exception will be thrown.
   * @throws UnsupportedOperationException if the notification fired is not a
   *         {@link org.mule.runtime.core.context.notification.CustomNotification}
   */
  @Override
  public void fireNotification(ServerNotification notification) {
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
  public void setSecurityManager(SecurityManager securityManager) throws RegistrationException {
    checkLifecycleForPropertySet(MuleProperties.OBJECT_SECURITY_MANAGER, Initialisable.PHASE_NAME);
    registryBroker.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, securityManager);
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
    SecurityManager securityManager = registryBroker.lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
    if (securityManager == null) {
      Collection temp = registryBroker.lookupObjects(SecurityManager.class);
      if (temp.size() > 0) {
        securityManager = ((SecurityManager) temp.iterator().next());
      }
    }
    if (securityManager == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull("securityManager"));
    }
    return securityManager;
  }

  /**
   * Obtains a workManager instance that can be used to schedule work in a thread pool. This will be used primarially by Agents
   * wanting to schedule work. This work Manager must <b>never</b> be used by provider implementations as they have their own
   * workManager accible on the connector.
   * <p/>
   * If a workManager has not been set by the time the <code>initialise()</code> method has been called a default
   * <code>MuleWorkManager</code> will be created using the <i>DefaultThreadingProfile</i> on the <code>MuleConfiguration</code>
   * object.
   *
   * @return a workManager instance used by the current MuleManager
   * @see org.mule.runtime.core.api.config.ThreadingProfile
   * @see DefaultMuleConfiguration
   */
  @Override
  public WorkManager getWorkManager() {
    return workManager;
  }

  @Override
  public WorkListener getWorkListener() {
    return workListener;
  }

  @Override
  public QueueManager getQueueManager() {
    if (queueManager == null) {
      queueManager = registryBroker.lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER);
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

  @Override
  public ObjectStoreManager getObjectStoreManager() {
    return this.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_MANAGER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectSerializer getObjectSerializer() {
    return objectSerializer;
  }

  /**
   * When running in clustered mode, it returns a {@link org.mule.runtime.core.api.store.ObjectStoreManager} that creates
   * {@link org.mule.runtime.core.api.store.ObjectStore} instances which are only local to the current node. This is just a
   * workaround until we introduce a solution for durable persistent stores in HA. This is not part of Mule's API and you should
   * not use this in your apps or extensions
   *
   * @return a {@link org.mule.runtime.core.api.store.ObjectStoreManager}
   * @since 3.5.0
   */
  public ObjectStoreManager getLocalObjectStoreManager() {
    return this.getRegistry().lookupObject(LOCAL_OBJECT_STORE_MANAGER_KEY);
  }

  /**
   * When running in clustered mode, it returns a {@link org.mule.runtime.core.util.queue.QueueManager} that creates
   * {@link org.mule.runtime.core.util.queue.Queue} instances which are only local to the current node. This is just a workaround
   * until we introduce a solution for durable persistent queues in HA. This is not part of Mule's API and you should not use this
   * in your apps or extensions
   *
   * @return a {@link org.mule.runtime.core.util.queue.QueueManager}
   * @since 3.5.0
   */
  public QueueManager getLocalQueueManager() {
    return this.getRegistry().lookupObject(LOCAL_QUEUE_MANAGER_KEY);
  }

  @Override
  public void setQueueManager(QueueManager queueManager) throws RegistrationException {
    getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
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

  @Override
  public ThreadingProfile getDefaultMessageDispatcherThreadingProfile() {
    return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE);
  }

  @Override
  public ThreadingProfile getDefaultMessageRequesterThreadingProfile() {
    return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE);
  }

  @Override
  public ThreadingProfile getDefaultMessageReceiverThreadingProfile() {
    return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE);
  }

  @Override
  public ThreadingProfile getDefaultServiceThreadingProfile() {
    return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamCloserService getStreamCloserService() {
    if (this.streamCloserService == null) {
      synchronized (streamCloserServiceLock) {
        if (this.streamCloserService == null) {
          this.streamCloserService = this.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
        }
      }
    }

    return this.streamCloserService;
  }

  @Override
  public ThreadingProfile getDefaultThreadingProfile() {
    return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE);
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

  /**
   * Returns the Expression Manager configured for this instance of Mule
   *
   * @return the Expression Manager configured for this instance of Mule
   * @see org.mule.runtime.core.api.expression.ExpressionManager
   */
  @Override
  public ExpressionManager getExpressionManager() {
    return expressionManager;
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

  protected SplashScreen buildStartupSplash() {
    SplashScreen startupScreen =
        config.isContainerMode() ? new ApplicationStartupSplashScreen() : new ServerStartupSplashScreen();
    startupScreen.setHeader(this);
    startupScreen.setFooter(this);
    return startupScreen;
  }

  protected SplashScreen buildShutdownSplash() {
    SplashScreen shutdownScreen =
        config.isContainerMode() ? new ApplicationShutdownSplashScreen() : new ServerShutdownSplashScreen();
    shutdownScreen.setHeader(this);
    return shutdownScreen;
  }

  @Override
  public MuleClient getClient() {
    return localMuleClient;
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
  public void setObjectStore(String name, ListableObjectStore<Serializable> store) throws RegistrationException {
    checkLifecycleForPropertySet(name, Initialisable.PHASE_NAME);
    registryBroker.registerObject(name, store);
  }

  @Override
  public String getClusterId() {
    return clusterConfiguration.getClusterId();
  }

  @Override
  public int getClusterNodeId() {
    return clusterConfiguration.getClusterNodeId();
  }

  public void setPollingController(PollingController pollingController) {
    this.pollingController = pollingController;
  }

  @Override
  public boolean isPrimaryPollingInstance() {
    return pollingController.isPrimaryPollingInstance();
  }

  @Override
  public String getUniqueIdString() {
    return clusterConfiguration.getClusterNodeId() + "-" + UUID.getUUID();
  }

  @Override
  public Map<QName, Set<Object>> getConfigurationAnnotations() {
    return configurationAnnotations;
  }

  @Override
  public MessagingExceptionHandler getDefaultExceptionStrategy() {
    MessagingExceptionHandler defaultExceptionStrategy;
    if (config.getDefaultExceptionStrategyName() != null) {
      defaultExceptionStrategy = getRegistry().lookupObject(config.getDefaultExceptionStrategyName());
      if (defaultExceptionStrategy == null) {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("No global exception strategy named %s",
                                                                                      config.getDefaultExceptionStrategyName())));
      }
    } else {
      defaultExceptionStrategy = new DefaultMessagingExceptionStrategy(this);
    }
    return defaultExceptionStrategy;
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
          dataTypeConversionResolver = getRegistry().lookupObject(MuleProperties.OBJECT_CONVERTER_RESOLVER);

          if (dataTypeConversionResolver == null) {
            dataTypeConversionResolver = new DynamicDataTypeConversionResolver(this);

            try {
              getRegistry().registerObject(MuleProperties.OBJECT_CONVERTER_RESOLVER, dataTypeConversionResolver);
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
  public ExpressionLanguage getExpressionLanguage() {
    if (this.expressionLanguage == null) {
      this.expressionLanguage = this.registryBroker.lookupObject(OBJECT_EXPRESSION_LANGUAGE);
    }

    return this.expressionLanguage;
  }

  @Override
  public LockFactory getLockFactory() {
    if (this.lockFactory == null) {
      this.lockFactory = registryBroker.get(MuleProperties.OBJECT_LOCK_FACTORY);
    }
    return this.lockFactory;
  }

  @Override
  public ProcessingTimeWatcher getProcessorTimeWatcher() {
    if (this.processingTimeWatcher == null) {
      this.processingTimeWatcher = registryBroker.get(MuleProperties.OBJECT_PROCESSING_TIME_WATCHER);
    }

    return this.processingTimeWatcher;
  }

  @Override
  public boolean waitUntilStarted(int timeout) throws InterruptedException {
    return startLatch.await(timeout, TimeUnit.MILLISECONDS);
  }

  private void overrideClusterConfiguration() {
    ClusterConfiguration overriddenClusterConfiguration = getRegistry().get(MuleProperties.OBJECT_CLUSTER_CONFIGURATION);
    if (overriddenClusterConfiguration != null) {
      this.clusterConfiguration = overriddenClusterConfiguration;
    }
  }

  private void overridePollingController() {
    PollingController overriddenPollingController = getRegistry().get(OBJECT_POLLING_CONTROLLER);
    if (overriddenPollingController != null) {
      this.pollingController = overriddenPollingController;
    }
  }

  public void setMuleConfiguration(MuleConfiguration muleConfiguration) {
    this.config = muleConfiguration;
  }

  public void setWorkManager(WorkManager workManager) {
    this.workManager = workManager;
  }

  public void setworkListener(WorkListener workListener) {
    this.workListener = workListener;
  }

  public void setNotificationManager(ServerNotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }

  public void setLifecycleManager(MuleContextLifecycleManager lifecyleManager) {
    this.lifecycleManager = lifecyleManager;
  }

  public void setExpressionManager(DefaultExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  public void setRegistryBroker(DefaultRegistryBroker registryBroker) {
    this.registryBroker = registryBroker;
  }

  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  public void setMuleRegistry(MuleRegistryHelper muleRegistry) {
    this.muleRegistryHelper = muleRegistry;
  }

  public void setLocalMuleClient(DefaultLocalMuleClient localMuleContext) {
    this.localMuleClient = localMuleContext;
  }

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

  public void setBootstrapServiceDiscoverer(BootstrapServiceDiscoverer bootstrapServiceDiscoverer) {
    this.bootstrapServiceDiscoverer = bootstrapServiceDiscoverer;
  }

  @Override
  public String getId() {
    MuleConfiguration conf = getConfiguration();
    return format("%s.%s.%s", conf.getDomainId(), getClusterId(), conf.getId());
  }

  public void setErrorTypeLocator(ErrorTypeLocator errorTypeLocator) {
    this.errorTypeLocator = errorTypeLocator;
  }

  public ErrorTypeLocator getErrorTypeLocator() {
    return errorTypeLocator;
  }
}
