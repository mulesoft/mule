/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.management.stats.AllStatistics;
import org.mule.util.queue.QueueManager;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;

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
     *
     * @return
     *
     */
    QueueManager getQueueManager();

    AllStatistics getStatistics();

    LifecycleManager getLifecycleManager();

    MuleRegistry getRegistry();
    
    MuleConfiguration getConfiguration();

    ThreadingProfile getDefaultMessageDispatcherThreadingProfile();

    ThreadingProfile getDefaultMessageRequesterThreadingProfile();

    ThreadingProfile getDefaultMessageReceiverThreadingProfile();

    ThreadingProfile getDefaultServiceThreadingProfile();

    ThreadingProfile getDefaultThreadingProfile();

    // TODO This should ideally only be available via an Admin interface
    void addRegistry(long id, Registry registry);

    // TODO This should ideally only be available via an Admin interface
    void removeRegistry(long id);

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
}
