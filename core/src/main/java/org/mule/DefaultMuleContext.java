/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.*;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.endpoint.EndpointFactory;
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
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.expression.DefaultExpressionManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.ProcessingTimeWatcher;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.util.ApplicationShutdownSplashScreen;
import org.mule.util.ApplicationStartupSplashScreen;
import org.mule.util.ServerShutdownSplashScreen;
import org.mule.util.ServerStartupSplashScreen;
import org.mule.util.SplashScreen;
import org.mule.util.concurrent.Latch;
import org.mule.util.queue.QueueManager;

import java.util.Collection;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleContext implements MuleContext
{
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

    private ClassLoader executionClassLoader;
    
    protected LocalMuleClient localMuleClient;
    
    /** Global exception handler which handles "system" exceptions (i.e., when no message is involved). */
    protected SystemExceptionHandler exceptionListener;

    private ProcessingTimeWatcher processingTimeWatcher;

    private final Latch startLatch = new Latch();

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

        if (getSecurityManager() == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("securityManager"));
        }
        if (getQueueManager() == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("queueManager"));
        }

        startDate = System.currentTimeMillis();

        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTING));
        getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);


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
        SecurityManager securityManager = (SecurityManager) registryBroker.lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
        if (securityManager == null)
        {
            Collection temp = registryBroker.lookupObjects(SecurityManager.class);
            if (temp.size() > 0)
            {
                securityManager = ((SecurityManager) temp.iterator().next());
            }
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
        QueueManager queueManager = (QueueManager) registryBroker.lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER);
        if (queueManager == null)
        {
            Collection temp = registryBroker.lookupObjects(QueueManager.class);
            if (temp.size() > 0)
            {
                queueManager = ((QueueManager) temp.iterator().next());
            }
        }
        return queueManager;
    }

    public void setQueueManager(QueueManager queueManager) throws RegistrationException
    {
        checkLifecycleForPropertySet(MuleProperties.OBJECT_QUEUE_MANAGER, Initialisable.PHASE_NAME);
        registryBroker.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    public MuleConfiguration getConfiguration()
    {

        return config;
        //return (MuleConfiguration) getRegistry().lookupObject(MuleProperties.OBJECT_MULE_CONFIGURATION);
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
        TransactionManager transactionManager = (TransactionManager) registryBroker.lookupObject(MuleProperties.OBJECT_TRANSACTION_MANAGER);
        if (transactionManager == null)
        {
            Collection temp = registryBroker.lookupObjects(TransactionManagerFactory.class);
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
        return transactionManager;
    }

    protected void checkLifecycleForPropertySet(String propertyName, String phase) throws IllegalStateException
    {
        if (lifecycleManager.isPhaseComplete(phase))
        {
            throw new IllegalStateException("Cannot set property: '" + propertyName + "' once the server has been gone through the " + phase + " phase.");
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

    public ProcessingTimeWatcher getProcessorTimeWatcher()
    {
        if (this.processingTimeWatcher == null)
        {
            this.processingTimeWatcher = registryBroker.get(MuleProperties.OBJECT_PROCESSING_TIME_WATCHER);
        }

        return this.processingTimeWatcher;
    }

    public boolean waitUtilStarted(int timeout) throws InterruptedException
    {
        return startLatch.await(timeout, MILLISECONDS);
    }

}
