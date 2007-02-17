/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.manager;

import org.mule.impl.internal.notifications.NotificationException;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.util.queue.QueueManager;

import java.util.Map;

import javax.transaction.TransactionManager;

/**
 * <code>UMOManager</code> maintains and provides services for a UMO server
 * instance.
 */
public interface UMOManager extends Lifecycle, Registerable
{
    /**
     * Getter for the envionment parameters declared in the mule-config.xml
     * 
     * @param key the propery name
     * @return the property value
     */
    Object getProperty(Object key);

    /**
     * Sets an Mule environment parameter in the <code>MuleManager</code>.
     * 
     * @param key the parameter name
     * @param value the parameter value
     */
    void setProperty(Object key, Object value);

    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     * 
     * @param manager the manager to use
     * @throws Exception
     */
    void setTransactionManager(TransactionManager manager) throws Exception;

    /**
     * Returns the Jta transaction manager used by this Mule server instance. or null
     * if a transaction manager has not been set
     * 
     * @return the Jta transaction manager used by this Mule server instance. or null
     *         if a transaction manager has not been set
     */
    TransactionManager getTransactionManager();

    /**
     * Gets all properties associated with the UMOManager
     * 
     * @return a map of properties on the Manager
     * 
     * @deprecated Returning a Map is bad for encapsulation.
     */
    Map getProperties();

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
     * Returns the long date when the server was started
     * 
     * @return the long date when the server was started
     */
    long getStartDate();

    /**
     * Registers an intenal server event listener. The listener will be notified when
     * a particular event happens within the server. Typically this is not an event
     * in the same sense as an UMOEvent (although there is nothing stopping the
     * implementation of this class triggering listeners when a UMOEvent is
     * received). The types of notifications fired is entirely defined by the
     * implementation of this class
     * 
     * @param l the listener to register
     */
    void registerListener(UMOServerNotificationListener l) throws NotificationException;

    /**
     * Registers an intenal server event listener. The listener will be notified when
     * a particular event happens within the server. Typically this is not an event
     * in the same sense as an UMOEvent (although there is nothing stopping the
     * implementation of this class triggering listeners when a UMOEvent is
     * received). The types of notifications fired is entirely defined by the
     * implementation of this class
     * 
     * @param l the listener to register
     * @param resourceIdentifier a particular resource name for the given type of
     *            listener For example, the resourceName could be the name of a
     *            component if the listener was a ComponentNotificationListener
     */
    void registerListener(UMOServerNotificationListener l, String resourceIdentifier)
        throws NotificationException;

    /**
     * Unregisters a previously registered listener. If the listener has not already
     * been registered, this method should return without exception
     * 
     * @param l the listener to unregister
     */
    void unregisterListener(UMOServerNotificationListener l);

    /**
     * Fires a server notification to all regiistered listeners
     * 
     * @param notification the notification to fire
     */
    void fireNotification(UMOServerNotification notification);

    /**
     * associates a Dependency Injector container with Mule. This can be used to
     * integrate container managed resources with Mule resources
     * 
     * @param context a Container context to use.
     */
    void setContainerContext(UMOContainerContext context) throws UMOException;

    /**
     * associates a Dependency Injector container with Mule. This can be used to
     * integrate container managed resources with Mule resources
     * 
     * @return the container associated with the Manager
     */
    UMOContainerContext getContainerContext();

    /**
     * Sets the unique Id for this Manager instance. this id can be used to assign an
     * identy to the manager so it can be identified in a network of Mule nodes
     * 
     * @param id the unique Id for this manager in the network
     */
    void setId(String id);

    /**
     * Gets the unique Id for this Manager instance. this id can be used to assign an
     * identy to the manager so it can be identified in a network of Mule nodes
     * 
     * @return the unique Id for this manager in the network
     */
    String getId();

    /**
     * Sets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     * 
     * @param securityManager the security manager used by this Mule instance to
     *            authenticate and authorise incoming and outgoing event traffic and
     *            service invocations
     */
    void setSecurityManager(UMOSecurityManager securityManager) throws InitialisationException;

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     * 
     * @return he security manager used by this Mule instance to authenticate and
     *         authorise incoming and outgoing event traffic and service invocations
     */
    UMOSecurityManager getSecurityManager();

    /**
     * Obtains a workManager instance that can be used to schedule work in a thread
     * pool. This will be used primarially by UMOAgents wanting to schedule work.
     * This work Manager must <b>never</b> be used by provider implementations as
     * they have their own workManager accible on the connector.
     * 
     * @return a workManager instance used by the current MuleManager
     */
    UMOWorkManager getWorkManager();

    /**
     * Sets a workManager instance that can be used to schedule work in a thread
     * pool. This will be used primarially by UMOAgents wanting to schedule work.
     * This work Manager must <b>never</b> be used by provider implementations as
     * they have their own workManager accible on the connector.
     * 
     * @param workManager the workManager instance used by the current MuleManager
     */
    void setWorkManager(UMOWorkManager workManager);

    /**
     * Sets the queue manager used by mule for queuing events. This is used by both
     * components and vm provider.
     * 
     * @param queueManager
     */
    void setQueueManager(QueueManager queueManager);

    /**
     * Gets the queue manager used by mule for queuing events. This is used by both
     * components and vm provider.
     * 
     * @return
     */
    QueueManager getQueueManager();
}
