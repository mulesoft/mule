/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.RegistryBroker;
import org.mule.api.security.SecurityManager;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.config.MuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.management.stats.AllStatistics;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;
import org.mule.util.queue.QueueManager;

import java.util.Collection;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleContext implements MuleContext
{
    /** logger used by this class */
    private static transient Log logger = LogFactory.getLog(DefaultMuleContext.class);

    /** Internal registry facade which delegates to other registries. */
    private RegistryBroker registryBroker;

    /** Simplified Mule configuration interface */
    private MuleRegistry muleRegistryHelper;
    
    /** stats used for management */
    private AllStatistics stats = new AllStatistics();

    private WorkManager workManager;

    /**
     * LifecycleManager for the MuleContext.  Note: this is NOT the same lifecycle manager
     * as the one in the Registry.
     */
    protected LifecycleManager lifecycleManager;

    protected ServerNotificationManager notificationManager;

    private MuleConfiguration config = new MuleConfiguration();
    
    public DefaultMuleContext(LifecycleManager lifecycleManager)
    {
        if (lifecycleManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("lifecycleManager").getMessage());
        }
        this.lifecycleManager = lifecycleManager;
    }

    protected RegistryBroker createRegistryBroker()
    {
        return new DefaultRegistryBroker();
    }
    
    protected MuleRegistry createRegistryHelper(Registry registry)
    {
        return new MuleRegistryHelper(registry);
    }
    
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

        if (getNotificationManager() == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull(
                MuleProperties.OBJECT_NOTIFICATION_MANAGER).getMessage());
        }
        if (workManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("workManager").getMessage());
        }

        try
        {
            registryBroker = createRegistryBroker();
            muleRegistryHelper = createRegistryHelper(registryBroker);
            
            // Initialize internal registries
            registryBroker.initialise();

            //We need to start the work manager straight away since we need it to fire notifications
            workManager.start();
            getNotificationManager().start(workManager);

            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISING));

            lifecycleManager.firePhase(this, Initialisable.PHASE_NAME);

            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_INITIALISED));
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        return LifecycleTransitionResult.OK;
    }


    public synchronized LifecycleTransitionResult start() throws MuleException
    {
        lifecycleManager.checkPhase(Startable.PHASE_NAME);
        if (!isStarted())
        {
            if (getSecurityManager() == null)
            {
                throw new NullPointerException(CoreMessages.objectIsNull("securityManager").getMessage());
            }
            if (getQueueManager() == null)
            {
                throw new NullPointerException(CoreMessages.objectIsNull("queueManager").getMessage());
            }

            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTING));

            lifecycleManager.firePhase(this, Startable.PHASE_NAME);

            if (logger.isInfoEnabled())
            {
                logger.info(getConfiguration().getStartSplash());
            }
            fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STARTED));
        }
        return LifecycleTransitionResult.OK;
    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and
     * connectors
     *
     * @throws MuleException if either any of the sessions or connectors fail to
     *                      stop
     */
    public synchronized LifecycleTransitionResult stop() throws MuleException
    {
        lifecycleManager.checkPhase(Stoppable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPING));
        lifecycleManager.firePhase(this, Stoppable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_STOPPED));
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {        
        if (isDisposing())
        {
            return;
        }
             
        ServerNotificationManager notificationManager = getNotificationManager();
        lifecycleManager.checkPhase(Disposable.PHASE_NAME);
        fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSING));

        try
        {
            if (isStarted())
            {
                stop();
            }
        }
        catch (MuleException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }

        try
        {
            lifecycleManager.firePhase(this, Disposable.PHASE_NAME);
            // Dispose internal registries
            registryBroker.dispose();
        }
        catch (Exception e)
        {
            logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
        }

        notificationManager.fireNotification(new MuleContextNotification(this, MuleContextNotification.CONTEXT_DISPOSED));

        if ((getConfiguration().getStartDate() > 0) && logger.isInfoEnabled())
        {
            logger.info(getConfiguration().getEndSplash());
        }
        //lifecycleManager.reset();
    }


    /**
     * Determines if the server has been initialised
     *
     * @return true if the server has been initialised
     */
    public boolean isInitialised()
    {
        return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
    }

    /**
     * Determines if the server is being initialised
     *
     * @return true if the server is beening initialised
     */
    public boolean isInitialising()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    protected boolean isStopped()
    {
        return lifecycleManager.isPhaseComplete(Stoppable.PHASE_NAME);
    }

    protected boolean isStopping()
    {
        return Stoppable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    /**
     * Determines if the server has been started
     *
     * @return true if the server has been started
     */
    public boolean isStarted()
    {
        return lifecycleManager.isPhaseComplete(Startable.PHASE_NAME);
    }

    protected boolean isStarting()
    {
        return Startable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isDisposed()
    {
        return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
    }

    public boolean isDisposing()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    /**
     * Gets all statisitcs for this instance
     *
     * @return all statisitcs for this instance
     */
    public AllStatistics getStatistics()
    {
        return stats;
    }

    /** Sets statistics on this instance */
    public void setStatistics(AllStatistics stat)
    {
        this.stats = stat;
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
     * {@link org.mule.api.context.notification.listener.CustomNotificationListener} notificationManager.
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
     * thread pool. This will be used primarially by UMOAgents wanting to
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
     * @see MuleConfiguration
     */
    public WorkManager getWorkManager()
    {
        return workManager;
    }

    /**
     * Obtains a workManager instance that can be used to schedule work in a
     * thread pool. This will be used primarially by UMOAgents wanting to
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
     * @param workManager the workManager instance used by the current
     *                    MuleManager
     * @throws IllegalStateException if the workManager has already been set.
     * @see org.mule.api.config.ThreadingProfile
     * @see MuleConfiguration
     * @see org.mule.work.MuleWorkManager
     */
    public void setWorkManager(WorkManager workManager)
    {
        checkLifecycleForPropertySet("workManager", Initialisable.PHASE_NAME);
        this.workManager = workManager;
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

    public void setNotificationManager(ServerNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws Exception
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
                    transactionManager = (((TransactionManagerFactory) temp.iterator().next()).create());
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Unable to create transaction manager"));
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



    public void register() throws RegistrationException
    {
        throw new UnsupportedOperationException("register");
    }

    public void deregister() throws RegistrationException
    {
        throw new UnsupportedOperationException("deregister");
    }

    public String getRegistryId()
    {
        throw new UnsupportedOperationException("registryId");
    }


    protected void checkLifecycleForPropertySet(String propertyName, String phase) throws IllegalStateException
    {
        if (lifecycleManager.isPhaseComplete(phase))
        {
            throw new IllegalStateException("Cannot set property: '" + propertyName + "' once the server has been gone through the " + phase + " phase.");
        }
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }

    public MuleRegistry getRegistry()
    {
        return muleRegistryHelper;
    }

    /**
     * Apply current phase of the LifecycleManager.  Note: this is NOT the same lifecycle manager
     * as the one in the Registry.
     */
    public void applyLifecycle(Object object) throws MuleException
    {
        lifecycleManager.applyLifecycle(this, object);
    }

    public ThreadingProfile getDefaultMessageDispatcherThreadingProfile()
    {
        return getThreadingProfile(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultMessageRequesterThreadingProfile()
    {
        return getThreadingProfile(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultMessageReceiverThreadingProfile()
    {
        return getThreadingProfile(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultComponentThreadingProfile()
    {
        return getThreadingProfile(MuleProperties.OBJECT_DEFAULT_COMPONENT_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return getThreadingProfile(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE);
    }

    private ThreadingProfile getThreadingProfile(String name)
    {
        ThreadingProfile tp = (ThreadingProfile) registryBroker.lookupObject(name);
        if (tp != null)
        {
            return tp;
        }
        else
        {
            // only used in tests, where no registry is present
            return ThreadingProfile.DEFAULT_THREADING_PROFILE;
        }
    }

    // TODO This should ideally only be available via an Admin interface
    public void addRegistry(Registry registry)
    {
        registryBroker.addRegistry(registry);
    }
}
