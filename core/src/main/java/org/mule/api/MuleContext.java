/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.DataTypeConversionResolver;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.util.StreamCloserService;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.extensions.ExtensionsManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.ProcessingTimeWatcher;
import org.mule.util.lock.LockFactory;
import org.mule.util.queue.QueueManager;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

public interface MuleContext extends Lifecycle
{
    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws Exception
     */
    void setTransactionManager(TransactionManager manager) throws Exception;

    /**
     * Returns the Jta transaction manager used by this Mule server instance. or
     * null if a transaction manager has not been set
     *
     * @return the Jta transaction manager used by this Mule server instance. or
     *         null if a transaction manager has not been set
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
     * Registers an intenal server event listener. The listener will be notified
     * when a particular event happens within the server. Typically this is not
     * an event in the same sense as an MuleEvent (although there is nothing
     * stopping the implementation of this class triggering listeners when a
     * MuleEvent is received).
     * <p/>
     * The types of notifications fired is entirely defined by the implementation of
     * this class
     *
     * @param l the listener to register
     */
    void registerListener(ServerNotificationListener l) throws NotificationException;

    /**
     * Registers an intenal server event listener. The listener will be notified
     * when a particular event happens within the server. Typically this is not
     * an event in the same sense as an MuleEvent (although there is nothing
     * stopping the implementation of this class triggering listeners when a
     * MuleEvent is received).
     * <p/>
     * The types of notifications fired is entirely defined by the implementation of
     * this class
     *
     * @param l                  the listener to register
     * @param resourceIdentifier a particular resource name for the given type
     *                           of listener For example, the resourceName could be the name of
     *                           a service if the listener was a ServiceNotificationListener
     */
    void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException;

    /**
     * Unregisters a previously registered listener. If the listener has not
     * already been registered, this method should return without exception
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
     * Sets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @param securityManager the security manager used by this Mule instance to
     *                        authenticate and authorise incoming and outgoing event traffic
     *                        and service invocations
     * @throws RegistrationException
     */
    void setSecurityManager(SecurityManager securityManager) throws InitialisationException, RegistrationException;

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @return he security manager used by this Mule instance to authenticate
     *         and authorise incoming and outgoing event traffic and service
     *         invocations
     */
    SecurityManager getSecurityManager();

    /**
     * Obtains a workManager instance that can be used to schedule work in a
     * thread pool. This will be used primarially by Agents wanting to
     * schedule work. This work Manager must <b>never</b> be used by provider
     * implementations as they have their own workManager accible on the
     * connector.
     *
     * @return a workManager instance used by the current MuleManager
     */
    WorkManager getWorkManager();

    WorkListener getWorkListener();

    /**
     * Sets the queue manager used by mule for queuing events. This is used for
     * service queues
     *
     * @param queueManager
     * @throws RegistrationException
     *
     */
    void setQueueManager(QueueManager queueManager) throws RegistrationException;

    /**
     * Gets the queue manager used by mule for queuing events. This is used for
     * service queues.
     */
    QueueManager getQueueManager();
    
    ObjectStoreManager getObjectStoreManager();

    ExtensionsManager getExtensionsManager();

    AllStatistics getStatistics();

    LifecycleManager getLifecycleManager();

    MuleRegistry getRegistry();

    MuleConfiguration getConfiguration();

    ThreadingProfile getDefaultMessageDispatcherThreadingProfile();

    ThreadingProfile getDefaultMessageRequesterThreadingProfile();

    ThreadingProfile getDefaultMessageReceiverThreadingProfile();

    ThreadingProfile getDefaultServiceThreadingProfile();

    ThreadingProfile getDefaultThreadingProfile();

    /**
     * Returns the configured {@link org.mule.api.util.StreamCloserService}
     *
     * @return a {@link org.mule.api.util.StreamCloserService}
     * @since 3.5.0
     */
    public StreamCloserService getStreamCloserService();

    // TODO This should ideally only be available via an Admin interface
    void addRegistry(Registry registry);

    // TODO This should ideally only be available via an Admin interface
    void removeRegistry(Registry registry);

    /**
     * Returns the date when the server was started.
     * @return the date when the server was started.
     */
    long getStartDate();

    /**
     * Returns the Expression Manager configured for this instance of Mule
     * @return the Expression Manager configured for this instance of Mule
     * @see org.mule.api.expression.ExpressionManager
     */
    ExpressionManager getExpressionManager();

    /**
     * Returns the EndpointFactory configured for this instance of Mule
     * @return the EndpointFactory configured for this instance of Mule
     * @see EndpointFactory
     */
    EndpointFactory getEndpointFactory();

    void setExecutionClassLoader(ClassLoader cl);

    ClassLoader getExecutionClassLoader();

    boolean isStopped();

    boolean isStopping();

    boolean isStarting();

    LocalMuleClient getClient();

    SystemExceptionHandler getExceptionListener();

    void setExceptionListener(SystemExceptionHandler exceptionListener);

    void setObjectStore(String name, ListableObjectStore<Serializable> store) throws RegistrationException;
    
    void handleException(Exception e, RollbackSourceCallback rollbackMethod);

    void handleException(Exception e);

    /**
     * @return the ID of the cluster the current instance belongs to.  Returns the empty string if this instance
     * isn't part of a cluster.
     */
    String getClusterId();

    /**
     * @return the cluster node ID for the current instance belongs to.  Returns 0 if this instance
     * isn't part of a cluster.
     */
    int getClusterNodeId();

    /**
     * @return true if this instance in the designated poller.  This will always be true unless the instance is part of
     * a cluster.
     */
    boolean isPrimaryPollingInstance();

    /**
     * Generate a unique ID string; this will begin with the cluster node ID followed by a
     * dash, e.g. "3-XXXYYY"
     */
    String getUniqueIdString();

    /**
     * Return all annotations seen in the configuration
     */
    Map<QName, Set<Object>> getConfigurationAnnotations();

    /**
     * @return default exception strategy. If no default exception strategy was configured it returns {@link org.mule.exception.DefaultMessagingExceptionStrategy}
     */
    MessagingExceptionHandler getDefaultExceptionStrategy();

    /**
     * @return single resource transaction factory manager. Used to retrieve a transaction factory for each transactional resource (i.e jdbc DataSource, jms Connection)
     */
    SingleResourceTransactionFactoryManager getTransactionFactoryManager();

    /**
     * @return a non null {@link org.mule.DataTypeConversionResolver} instance to resolve implicit data type conversions
     */
    DataTypeConversionResolver getDataTypeConverterResolver();
    
    /**
     * Expression Language for evaluating expressions using Mule as the context
     * @return 
     */
    ExpressionLanguage getExpressionLanguage();

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

}

