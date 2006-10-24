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
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.queue.QueueManager;

import javax.transaction.TransactionManager;

import java.util.Map;

/**
 * <code>UMOManager</code> maintains and provides services for a UMO server
 * instance.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOManager extends Lifecycle
{
    /**
     * Getter for the envionment parameters declared in the mule-config.xml
     * 
     * @param key the propery name
     * @return the property value
     */
    Object getProperty(Object key);

    /**
     * @param logicalName the name of the endpoint to retrieve
     * @return the endpoint instnace if it exists
     */
    UMOConnector lookupConnector(String logicalName);

    /**
     * @param logicalName the logical mapping name for an endpointUri i.e. rather
     *            than specifing an endpointUri to be someone@my.com you can supply a
     *            more descriptive name such as <i>The System Administrator</i>
     * @param defaultName
     * @return the actual endpointUri value or null if it is not found
     */
    String lookupEndpointIdentifier(String logicalName, String defaultName);

    /**
     * Getter for a global endpoint. Any endpoints returned from this method can be
     * modified, as they are clones of the registered endpoints.
     * 
     * @param logicalName the name of the endpoint
     * @return the <code>UMOEndpoint</code> or null if it doesn't exist
     */
    UMOEndpoint lookupEndpoint(String logicalName);

    /**
     * Getter method for a Transformer.
     * 
     * @param name the name of the transformer
     * @return the Transformer instance if found, otherwise null
     */
    UMOTransformer lookupTransformer(String name);

    /**
     * Registers a <code>UMOConnector</code> with the <code>MuleManager</code>.
     * 
     * @param connector the <code>UMOConnector</code> to register
     */
    void registerConnector(UMOConnector connector) throws UMOException;

    /**
     * UnRegisters a <code>UMOConnector</code> with the <code>MuleManager</code>.
     * 
     * @param connectorName the name of the <code>UMOConnector</code> to unregister
     */
    void unregisterConnector(String connectorName) throws UMOException;

    /**
     * Registers an endpointUri with a logical name
     * 
     * @param logicalName the name of the endpointUri
     * @param endpoint the physical endpointUri value
     */
    void registerEndpointIdentifier(String logicalName, String endpoint) throws InitialisationException;

    /**
     * unregisters an endpointUri with a logical name
     * 
     * @param logicalName the name of the endpointUri
     */
    void unregisterEndpointIdentifier(String logicalName);

    /**
     * Registers a shared/global endpoint with the <code>MuleManager</code>.
     * 
     * @param endpoint the <code>UMOEndpoint</code> to register.
     */
    void registerEndpoint(UMOEndpoint endpoint) throws InitialisationException;

    /**
     * unregisters a shared/global endpoint with the <code>MuleManager</code>.
     * 
     * @param endpointName the <code>UMOEndpoint</code> name to unregister.
     */
    void unregisterEndpoint(String endpointName);

    /**
     * Registers a transformer with the <code>MuleManager</code>.
     * 
     * @param transformer the <code>UMOTransformer</code> to register.
     */
    void registerTransformer(UMOTransformer transformer) throws InitialisationException;

    /**
     * UnRegisters a transformer with the <code>MuleManager</code>.
     * 
     * @param transformerName the <code>UMOTransformer</code> name to register.
     */
    void unregisterTransformer(String transformerName);

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
     * The model used for managing components for this server
     * 
     * @return The model used for managing components for this server
     */
    UMOModel getModel();

    /**
     * The model used for managing components for this server
     * 
     * @param model The model used for managing components for this server
     */
    void setModel(UMOModel model) throws UMOException;

    /**
     * Gets all properties associated with the UMOManager
     * 
     * @return a map of properties on the Manager
     */
    Map getProperties();

    /**
     * Gets an unmodifiable collection of Connectors registered with the UMOManager
     * 
     * @return All connectors registered on the Manager
     * @see UMOConnector
     */
    Map getConnectors();

    /**
     * Gets an unmodifiable collection of endpoints registered with the UMOManager
     * 
     * @return All endpoints registered on the Manager
     */
    Map getEndpointIdentifiers();

    /**
     * Gets an unmodifiable collection of endpoints registered with the UMOManager
     * 
     * @return All endpoints registered on the Manager
     * @see org.mule.umo.endpoint.UMOEndpoint
     */
    Map getEndpoints();

    /**
     * Gets an unmodifiable collection of transformers registered with the UMOManager
     * 
     * @return All transformers registered on the Manager
     * @see UMOTransformer
     */
    Map getTransformers();

    /**
     * registers a interceptor stack list that can be referenced by other components
     * 
     * @param name the referenceable name for this stack
     * @param stack a List of interceptors
     * @see org.mule.umo.UMOInterceptor
     */
    void registerInterceptorStack(String name, UMOInterceptorStack stack);

    /**
     * Retrieves a configured interceptor stack.
     * 
     * @param name the name of the stack
     * @return the interceptor stack requested or null if there wasn't one configured
     *         for the given name
     */
    UMOInterceptorStack lookupInterceptorStack(String name);

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
     * Will register an agent object on this model. Agents can be server plugins such
     * as Jms support
     * 
     * @param agent
     */
    void registerAgent(UMOAgent agent) throws UMOException;

    /**
     * Will find a registered agent using its name, which is unique for all
     * registered agents
     * 
     * @param name the name of the Agent to find
     * @return the Agent or null if there is not agent registered with the given name
     */
    UMOAgent lookupAgent(String name);

    /**
     * Removes and destroys a registered agent
     * 
     * @param name the agent name
     * @return the destroyed agent or null if the agent doesn't exist
     */
    UMOAgent unregisterAgent(String name) throws UMOException;

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
