/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.api.config.MuleProperties.OBJECT_POLLING_CONTROLLER;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.SingleResourceTransactionFactoryManager;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.api.util.StreamCloserService;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.config.ClusterConfiguration;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.NullClusterConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.expression.DefaultExpressionManager;
import org.mule.extensions.ExtensionsManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.ProcessingTimeWatcher;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.transport.DefaultPollingController;
import org.mule.transport.PollingController;
import org.mule.util.ApplicationShutdownSplashScreen;
import org.mule.util.ApplicationStartupSplashScreen;
import org.mule.util.JdkVersionUtils;
import org.mule.util.ServerShutdownSplashScreen;
import org.mule.util.ServerStartupSplashScreen;
import org.mule.util.SplashScreen;
import org.mule.util.SystemUtils;
import org.mule.util.UUID;
import org.mule.util.concurrent.Latch;
import org.mule.util.lock.LockFactory;
import org.mule.util.queue.QueueManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleContext implements MuleContext
{

    /**
     * TODO: Remove these constants.
     * These constants only make sense until we have a reliable solution for durable persistence in Clustering.
     * These are not part of Mule's API and you should not use them in applications or extensions
     */
    public static final String LOCAL_TRANSIENT_OBJECT_STORE_KEY = "_localInMemoryObjectStore";
    public static final String LOCAL_PERSISTENT_OBJECT_STORE_KEY = "_localPersistentObjectStore";
    public static final String LOCAL_OBJECT_STORE_MANAGER_KEY = "_localObjectStoreManager";
    public static final String LOCAL_QUEUE_MANAGER_KEY = "_localQueueManager";

    /**
     * logger used by this class
     */
    private transient Log logger = LogFactory.getLog(DefaultMuleContext.class);

    /**
     * Internal registry facade which delegates to other registries.
     */
    private DefaultRegistryBroker registryBroker;

    /**
     * Simplified Mule configuration interface
     */
    private MuleRegistry muleRegistryHelper;

    /**
     * stats used for management
     */
    private AllStatistics stats = new AllStatistics();

    private WorkManager workManager;

    private WorkListener workListener;

    /**
     * LifecycleManager for the MuleContext.  Note: this is NOT the same lifecycle manager
     * as the one in the Registry.
     */
    protected MuleContextLifecycleManager lifecycleManager;

    protected ServerNotificationManager notificationManager;

    private MuleConfiguration config;

    /**
     * the date in milliseconds from when the server was started
     */
    private long startDate;

    private ExpressionManager expressionManager;

    private StreamCloserService streamCloserService;

    private ClassLoader executionClassLoader;

    protected LocalMuleClient localMuleClient;

    /**
     * Global exception handler which handles "system" exceptions (i.e., when no message is involved).
     */
    protected SystemExceptionHandler exceptionListener;

    private PollingController pollingController = new DefaultPollingController();

    private ClusterConfiguration clusterConfiguration = new NullClusterConfiguration();

    private Map<QName, Set<Object>> configurationAnnotations = new HashMap<QName, Set<Object>>();

    private SingleResourceTransactionFactoryManager singleResourceTransactionFactoryManager = new SingleResourceTransactionFactoryManager();

    private TransactionManager transactionManager;

    private LockFactory lockFactory;

    private ExpressionLanguage expressionLanguage;

    private ProcessingTimeWatcher processingTimeWatcher;

    private final Latch startLatch = new Latch();

    private QueueManager queueManager;

    private ExtensionsManager extensionsManager;

    /**
     * @deprecated Use empty constructor instead and use setter for dependencies.
     */
    @Deprecated
    public DefaultMuleContext(MuleConfiguration config,
                              WorkManager workManager,
                              WorkListener workListener,
                              MuleContextLifecycleManager lifecycleManager,
                              ServerNotificationManager notificationManager)
    {
        this.config = config;
        ((MuleContextAware) config).setMuleContext(this);
        this.workManager = workManager;
        this.workListener = workListener;
        this.lifecycleManager = lifecycleManager;
        this.notificationManager = notificationManager;
        this.notificationManager.setMuleContext(this);
        //there is no point having this object configurable
        this.expressionManager = new DefaultExpressionManager();
        ((MuleContextAware) this.expressionManager).setMuleContext(this);
        registryBroker = createRegistryBroker();
        muleRegistryHelper = createRegistryHelper(registryBroker);
        localMuleClient = new DefaultLocalMuleClient(this);
        exceptionListener = new DefaultSystemExceptionStrategy(this);
    }

    public DefaultMuleContext()
    {
    }

    protected DefaultRegistryBroker createRegistryBroker()
    {
        return new DefaultRegistryBroker(this);
    }

    protected MuleRegistry createRegistryHelper(DefaultRegistryBroker registry)
    {
        return new MuleRegistryHelper(registry, this);
    }

    public synchronized void initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

        if (getNotificationManager() == null)
        {
            throw new MuleRuntimeException(
                    CoreMessages.objectIsNull(MuleProperties.OBJECT_NOTIFICATION_MANAGER));
        }
        if (workManager == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("workManager"));
        }

        try
        {
            JdkVersionUtils.validateJdk();
        }
        catch (RuntimeException e)
        {
            throw new InitialisationException(CoreMessages.invalidJdk(SystemUtils.JAVA_VERSION,
                                                                      JdkVersionUtils.getSupportedJdks()), this);
        }

        try
        {
            // Initialize the helper, this only initialises the helper class and does not call the registry lifecycle manager
            //The registry lifecycle is called below using 'getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);'
            muleRegistryHelper.initialise();

            //We need to start the work manager straight away since we need it to fire notifications
            if (workManager instanceof MuleContextAware)
            {
                MuleContextAware contextAware = (MuleContextAware) workManager;
                contextAware.setMuleContext(this);
            }

            workManager.start();
            getNotificationManager().start(workManager, workListener);
            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISING));
            getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);

            if (expressionManager instanceof Initialisable)
            {
                ((Initialisable) expressionManager).initialise();
            }

            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISED));
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public synchronized void start() throws MuleException
    {
        getLifecycleManager().checkPhase(Startable.PHASE_NAME);

        if (getQueueManager() == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("queueManager"));
        }

        startDate = System.currentTimeMillis();

        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTING));
        getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);
        overridePollingController();
        overrideClusterConfiguration();

        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTED));

        startLatch.release();

        if (logger.isInfoEnabled())
        {
            SplashScreen startupScreen = buildStartupSplash();
            logger.info(startupScreen.toString());
        }
    }

    /**
     * Stops the <code>MuleContext</code> which stops all sessions and
     * connectors
     *
     * @throws MuleException if either any of the sessions or connectors fail to stop
     */
    public synchronized void stop() throws MuleException
    {
        startLatch.release();

        getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPING));
        getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPED));
    }

    public synchronized void dispose()
    {
        if (isStarted())
        {
            try
            {
                stop();
            }
            catch (MuleException e)
            {
                logger.error("Failed to stop Mule context", e);
            }
        }

        getLifecycleManager().checkPhase(Disposable.PHASE_NAME);

        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSING));

        try
        {
            getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);

            // THis is a little odd. I find the relationship between the MuleRegistry Helper and the registry broker, too much abstraction?
            muleRegistryHelper.dispose();
        }
        catch (Exception e)
        {
            logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
        }

        notificationManager.fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSED));

        notificationManager.dispose();
        workManager.dispose();

        if (expressionManager != null && expressionManager instanceof Disposable)
        {
            ((Disposable) expressionManager).dispose();
        }

        if ((getStartDate() > 0) && logger.isInfoEnabled())
        {
            SplashScreen shutdownScreen = buildShutdownSplash();
            logger.info(shutdownScreen.toString());
        }

        //registryBroker.dispose();

        setExecutionClassLoader(null);
    }

    /**
     * Determines if the server has been initialised
     *
     * @return true if the server has been initialised
     */
    public boolean isInitialised()
    {
        return getLifecycleManager().getState().isInitialised();
    }

    /**
     * Determines if the server is being initialised
     *
     * @return true if the server is beening initialised
     */
    public boolean isInitialising()
    {
        return getLifecycleManager().getState().isInitialising();
    }

    public boolean isStopped()
    {
        return getLifecycleManager().getState().isStopped();
    }

    public boolean isStopping()
    {
        return getLifecycleManager().getState().isStopping();
    }

    /**
     * Determines if the server has been started
     *
     * @return true if the server has been started
     */
    public boolean isStarted()
    {
        return getLifecycleManager().isPhaseComplete(Startable.PHASE_NAME);
    }

    public boolean isStarting()
    {
        return getLifecycleManager().getState().isStarting();
    }

    public boolean isDisposed()
    {
        return getLifecycleManager().getState().isDisposed();
    }

    public boolean isDisposing()
    {
        return getLifecycleManager().getState().isDisposing();
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    /**
     * Gets all statistics for this instance
     *
     * @return all statistics for this instance
     */
    public AllStatistics getStatistics()
    {
        return stats;
    }

    public void registerListener(ServerNotificationListener l) throws NotificationException
    {
        registerListener(l, null);
    }

    public void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager == null)
        {
            throw new MuleRuntimeException(CoreMessages.serverNotificationManagerNotEnabled());
        }
        notificationManager.addListenerSubscription(l, resourceIdentifier);
    }

    public void unregisterListener(ServerNotificationListener l)
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager != null)
        {
            notificationManager.removeListener(l);
        }
    }

    /**
     * Fires a server notification to all registered
     * {@link org.mule.api.context.notification.CustomNotificationListener} notificationManager.
     *
     * @param notification the notification to fire. This must be of type
     *                     {@link org.mule.context.notification.CustomNotification} otherwise an
     *                     exception will be thrown.
     * @throws UnsupportedOperationException if the notification fired is not a
     *                                       {@link org.mule.context.notification.CustomNotification}
     */
    public void fireNotification(ServerNotification notification)
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager != null)
        {
            notificationManager.fireNotification(notification);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("MuleEvent Manager is not enabled, ignoring notification: " + notification);
        }
    }

    /**
     * Sets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @param securityManager the security manager used by this Mule instance to
     *                        authenticate and authorise incoming and outgoing event traffic
     *                        and service invocations
     */
    public void setSecurityManager(SecurityManager securityManager) throws RegistrationException
    {
        checkLifecycleForPropertySet(MuleProperties.OBJECT_SECURITY_MANAGER, Initialisable.PHASE_NAME);
        registryBroker.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, securityManager);
    }

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @return he security manager used by this Mule instance to authenticate
     *         and authorise incoming and outgoing event traffic and service
     *         invocations
     */
    public SecurityManager getSecurityManager()
    {
        SecurityManager securityManager = registryBroker.lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
        if (securityManager == null)
        {
            Collection temp = registryBroker.lookupObjects(SecurityManager.class);
            if (temp.size() > 0)
            {
                securityManager = ((SecurityManager) temp.iterator().next());
            }
        }
        if (securityManager == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("securityManager"));
        }
        return securityManager;
    }

    /**
     * Obtains a workManager instance that can be used to schedule work in a
     * thread pool. This will be used primarially by Agents wanting to
     * schedule work. This work Manager must <b>never</b> be used by provider
     * implementations as they have their own workManager accible on the
     * connector.
     * <p/>
     * If a workManager has not been set by the time the
     * <code>initialise()</code> method has been called a default
     * <code>MuleWorkManager</code> will be created using the
     * <i>DefaultThreadingProfile</i> on the <code>MuleConfiguration</code>
     * object.
     *
     * @return a workManager instance used by the current MuleManager
     * @see org.mule.api.config.ThreadingProfile
     * @see DefaultMuleConfiguration
     */
    public WorkManager getWorkManager()
    {
        return workManager;
    }

    public WorkListener getWorkListener()
    {
        return workListener;
    }

    public QueueManager getQueueManager()
    {
        if (queueManager == null)
        {
            queueManager = registryBroker.lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER);
            if (queueManager == null)
            {
                Collection<QueueManager> temp = registryBroker.lookupObjects(QueueManager.class);
                if (temp.size() > 0)
                {
                    queueManager = temp.iterator().next();
                }
            }
        }
        return queueManager;
    }

    @Override
    public ExtensionsManager getExtensionsManager()
    {
        return extensionsManager;
    }

    @Override
    public ObjectStoreManager getObjectStoreManager()
    {
        return this.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_MANAGER);
    }

    /**
     * When running in clustered mode, it returns a {@link org.mule.api.store.ObjectStoreManager} that
     * creates {@link org.mule.api.store.ObjectStore} instances which are only local to the current node.
     * This is just a workaround until we introduce a solution for durable persistent stores in HA. This is not part of
     * Mule's API and you should not use this in your apps or extensions
     *
     * @return a {@link org.mule.api.store.ObjectStoreManager}
     * @since 3.5.0
     */
    public ObjectStoreManager getLocalObjectStoreManager()
    {
        return this.getRegistry().lookupObject(LOCAL_OBJECT_STORE_MANAGER_KEY);
    }

    /**
     * When running in clustered mode, it returns a {@link org.mule.util.queue.QueueManager} that
     * creates {@link org.mule.util.queue.Queue} instances which are only local to the current node.
     * This is just a workaround until we introduce a solution for durable persistent queues in HA. This is not part of
     * Mule's API and you should not use this in your apps or extensions
     *
     * @return a {@link org.mule.util.queue.QueueManager}
     * @since 3.5.0
     */
    public QueueManager getLocalQueueManager()
    {
        return this.getRegistry().lookupObject(LOCAL_QUEUE_MANAGER_KEY);
    }

    public void setQueueManager(QueueManager queueManager) throws RegistrationException
    {
        registryBroker.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
        this.queueManager = queueManager;
    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    public MuleConfiguration getConfiguration()
    {

        return config;
    }

    public ServerNotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    /**
     * Sets the JTA Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws RegistrationException if a transaction manager has already been set
     */
    public void setTransactionManager(TransactionManager manager) throws RegistrationException
    {
        //checkLifecycleForPropertySet(MuleProperties.OBJECT_TRANSACTION_MANAGER, Initialisable.PHASE_NAME);
        registryBroker.registerObject(MuleProperties.OBJECT_TRANSACTION_MANAGER, manager);
    }

    /**
     * Returns the Jta transaction manager used by this Mule server instance. or
     * null if a transaction manager has not been set
     *
     * @return the Jta transaction manager used by this Mule server instance. or
     *         null if a transaction manager has not been set
     */
    public TransactionManager getTransactionManager()
    {
        if (transactionManager == null)
        {
            transactionManager = registryBroker.lookupObject(MuleProperties.OBJECT_TRANSACTION_MANAGER);
            if (transactionManager == null)
            {
                Collection temp = registryBroker.lookupObjects(TransactionManagerFactory.class);
                if (temp.size() > 1)
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("More than one TX manager has been configured - Only one TX manager can be defined per application. " +
                                                                                    "Validate your app configuration or if your app belongs to a domain and the domains defines a TX manager then you should use that one."));
                }
                if (temp.size() > 0)
                {
                    try
                    {
                        transactionManager = (((TransactionManagerFactory) temp.iterator().next()).create(config));
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(CoreMessages.failedToCreate("transaction manager"), e);
                    }
                }
                else
                {
                    temp = registryBroker.lookupObjects(TransactionManager.class);
                    if (temp.size() > 0)
                    {
                        transactionManager = (((TransactionManager) temp.iterator().next()));
                    }
                }
            }
        }
        return transactionManager;
    }

    protected void checkLifecycleForPropertySet(String propertyName, String phase) throws IllegalStateException
    {
        if (lifecycleManager.isPhaseComplete(phase))
        {
            throw new IllegalStateException("Cannot set property: '" + propertyName + "' once the server has already been through the " + phase + " phase.");
        }
    }

    public MuleRegistry getRegistry()
    {
        return muleRegistryHelper;
    }

    public ThreadingProfile getDefaultMessageDispatcherThreadingProfile()
    {
        return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultMessageRequesterThreadingProfile()
    {
        return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultMessageReceiverThreadingProfile()
    {
        return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultServiceThreadingProfile()
    {
        return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamCloserService getStreamCloserService()
    {
        if (this.streamCloserService == null)
        {
            this.streamCloserService = this.getRegistry().lookupObject(
                    MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
        }

        return this.streamCloserService;
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return (ThreadingProfile) getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE);
    }

    /**
     * Returns the long date when the server was started
     *
     * @return the long date when the server was started
     */
    public long getStartDate()
    {
        return startDate;
    }

    /**
     * Returns the Expression Manager configured for this instance of Mule
     *
     * @return the Expression Manager configured for this instance of Mule
     * @see org.mule.api.expression.ExpressionManager
     */
    public ExpressionManager getExpressionManager()
    {
        return expressionManager;
    }

    public void setExecutionClassLoader(ClassLoader cl)
    {
        this.executionClassLoader = cl;
    }

    public ClassLoader getExecutionClassLoader()
    {
        return executionClassLoader;
    }

    public void addRegistry(Registry registry)
    {
        registryBroker.addRegistry(registry);
    }

    public void removeRegistry(Registry registry)
    {
        registryBroker.removeRegistry(registry);
    }

    protected SplashScreen buildStartupSplash()
    {
        SplashScreen startupScreen = config.isContainerMode()
                                         ? new ApplicationStartupSplashScreen()
                                         : new ServerStartupSplashScreen();
        startupScreen.setHeader(this);
        startupScreen.setFooter(this);
        return startupScreen;
    }

    protected SplashScreen buildShutdownSplash()
    {
        SplashScreen shutdownScreen = config.isContainerMode()
                                         ? new ApplicationShutdownSplashScreen()
                                         : new ServerShutdownSplashScreen();
        shutdownScreen.setHeader(this);
        return shutdownScreen;
    }

    public LocalMuleClient getClient()
    {
        return localMuleClient;
    }

    public void handleException(Exception e, RollbackSourceCallback rollbackMethod)
    {
        getExceptionListener().handleException(e, rollbackMethod);
    }

    public void handleException(Exception e)
    {
        handleException(e, null);
    }

    public SystemExceptionHandler getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(SystemExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public EndpointFactory getEndpointFactory()
    {
        return (EndpointFactory) registryBroker.lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }

    public void setObjectStore(String name, ListableObjectStore<Serializable> store) throws RegistrationException
    {
        checkLifecycleForPropertySet(name, Initialisable.PHASE_NAME);
        registryBroker.registerObject(name, store);
    }

    public String getClusterId()
    {
        return clusterConfiguration.getClusterId();
    }

    public int getClusterNodeId()
    {
        return clusterConfiguration.getClusterNodeId();
    }

    public void setPollingController(PollingController pollingController)
    {
        this.pollingController = pollingController;
    }

    @Override
    public boolean isPrimaryPollingInstance()
    {
        return pollingController.isPrimaryPollingInstance();
    }

    @Override
    public String getUniqueIdString()
    {
        return clusterConfiguration.getClusterNodeId() + "-" + UUID.getUUID();
    }

    @Override
    public Map<QName, Set<Object>> getConfigurationAnnotations()
    {
        return configurationAnnotations;
    }

    @Override
    public MessagingExceptionHandler getDefaultExceptionStrategy()
    {
        MessagingExceptionHandler defaultExceptionStrategy;
        if (config.getDefaultExceptionStrategyName() != null)
        {
            defaultExceptionStrategy = getRegistry().lookupObject(config.getDefaultExceptionStrategyName());
            if (defaultExceptionStrategy == null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("No global exception strategy named %s",config.getDefaultExceptionStrategyName())));
            }
        }
        else
        {
            defaultExceptionStrategy = new DefaultMessagingExceptionStrategy(this);
        }
        return defaultExceptionStrategy;
    }

    @Override
    public SingleResourceTransactionFactoryManager getTransactionFactoryManager()
    {
        return this.singleResourceTransactionFactoryManager;
    }

    @Override
    public DataTypeConversionResolver getDataTypeConverterResolver()
    {
        DataTypeConversionResolver dataTypeConversionResolver = getRegistry().lookupObject(MuleProperties.OBJECT_CONVERTER_RESOLVER);
        if (dataTypeConversionResolver == null)
        {
            dataTypeConversionResolver = new DynamicDataTypeConversionResolver(this);

            try
            {
                getRegistry().registerObject(MuleProperties.OBJECT_CONVERTER_RESOLVER, dataTypeConversionResolver);
            }
            catch (RegistrationException e)
            {
                // Should not occur
                throw new IllegalStateException(e);
            }
        }

        return dataTypeConversionResolver;
    }

    @Override
    public ExpressionLanguage getExpressionLanguage()
    {
        if (this.expressionLanguage == null)
        {
            this.expressionLanguage = this.registryBroker.lookupObject(MuleProperties.OBJECT_EXPRESSION_LANGUAGE);
        }

        return this.expressionLanguage;
    }

    @Override
    public LockFactory getLockFactory()
    {
        if (this.lockFactory == null)
        {
            this.lockFactory = registryBroker.get(MuleProperties.OBJECT_LOCK_FACTORY);
        }
        return this.lockFactory;
    }

    @Override
    public ProcessingTimeWatcher getProcessorTimeWatcher()
    {
        if (this.processingTimeWatcher == null)
        {
            this.processingTimeWatcher = registryBroker.get(MuleProperties.OBJECT_PROCESSING_TIME_WATCHER);
        }

        return this.processingTimeWatcher;
    }

    @Override
    public boolean waitUntilStarted(int timeout) throws InterruptedException
    {
        return startLatch.await(timeout, TimeUnit.MILLISECONDS);
    }

    private void overrideClusterConfiguration()
    {
        ClusterConfiguration overriddenClusterConfiguration = getRegistry().get(MuleProperties.OBJECT_CLUSTER_CONFIGURATION);
        if (overriddenClusterConfiguration != null)
        {
            this.clusterConfiguration = overriddenClusterConfiguration;
        }
    }

    private void overridePollingController()
    {
        PollingController overriddenPollingController = getRegistry().get(OBJECT_POLLING_CONTROLLER);
        if (overriddenPollingController != null)
        {
            this.pollingController = overriddenPollingController;
        }
    }

    public void setMuleConfiguration(MuleConfiguration muleConfiguration)
    {
        this.config = muleConfiguration;
    }

    public void setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    public void setworkListener(WorkListener workListener)
    {
        this.workListener = workListener;
    }

    public void setNotificationManager(ServerNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    public void setLifecycleManager(MuleContextLifecycleManager lifecyleManager)
    {
        this.lifecycleManager = lifecyleManager;
    }

    public void setExpressionManager(DefaultExpressionManager expressionManager)
    {
        this.expressionManager = expressionManager;
    }

    public void setRegistryBroker(DefaultRegistryBroker registryBroker)
    {
        this.registryBroker = registryBroker;
    }

    public void setMuleRegistry(MuleRegistryHelper muleRegistry)
    {
        this.muleRegistryHelper = muleRegistry;
    }

    public void setLocalMuleClient(DefaultLocalMuleClient localMuleContext)
    {
        this.localMuleClient = localMuleContext;
    }

    public void setExtensionsManager(ExtensionsManager extensionsManager)
    {
        this.extensionsManager = extensionsManager;
    }
}
