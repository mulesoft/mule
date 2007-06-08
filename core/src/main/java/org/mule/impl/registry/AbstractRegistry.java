/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.registry;

import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.SpringRegistry;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.notifications.RegistryNotification;
import org.mule.providers.service.TransportFactory;
import org.mule.registry.DeregistrationException;
import org.mule.registry.Registration;
import org.mule.registry.RegistrationException;
import org.mule.registry.RegistryStore;
import org.mule.registry.impl.MuleRegistration;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.registry.RegistryFacade;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public abstract class AbstractRegistry implements RegistryFacade

{
    private RegistryFacade parent;
    /**
     * the unique id for this Registry
     */
    private String id;

    private int defaultScope = DEFAULT_SCOPE;

    private static Log logger = LogFactory.getLog(SpringRegistry.class);

    protected UMOManagementContext managementContext;

    protected UMOLifecycleManager lifecycleManager;

    /**
     * Default Constructor
     */
    protected AbstractRegistry(String id)
    {
        if (id == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("RegistryID").getMessage());
        }
        this.id = id;
        lifecycleManager = createLifecycleManager();
    }

    protected AbstractRegistry(String id, RegistryFacade parent)
    {
        this(id);
        setParent(parent);
        lifecycleManager = createLifecycleManager();
    }

    protected abstract UMOLifecycleManager createLifecycleManager();

    protected UMOLifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    public final String getRegistryId()
    {
        return id;
    }

    public final synchronized void dispose()
    {
        //TODO lifecycleManager.checkPhase(Disposable.PHASE_NAME);

        if (isDisposed())
        {
            return;
        }

        try
        {
            doDispose();
            lifecycleManager.firePhase(getManagementContext(), Disposable.PHASE_NAME);
            if (getParent() != null)
            {
                parent.dispose();
            }
            else
            {
                //remove this referenceonce there is no one else left to dispose
                RegistryContext.setRegistry(null);
            }
        }
        catch (UMOException e)
        {
            //TO-DO
            logger.error("Failed to cleanly dispose: " + e.getMessage(), e);
        }
    }

    protected void doDispose()
    {

    }

    public boolean isDisposed()
    {
        return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
    }

    public boolean isDisposing()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isInitialised()
    {
        return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
    }

    public boolean isInitialising()
    {
        return Initialisable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public final void initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

        if (getParent() != null)
        {
            parent.initialise();
        }

        fireSystemEvent(new RegistryNotification(this, RegistryNotification.REGISTRY_INITIALISING));
        if (id == null)
        {
            logger.warn("No unique id has been set on this registry");
            id = UUID.getUUID();
        }
        try
        {
            doInitialise();
            lifecycleManager.firePhase(getManagementContext(), Initialisable.PHASE_NAME);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

    }

    protected void doInitialise() throws InitialisationException
    {

    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @throws org.mule.umo.UMOException if the the connectors or components fail to start
     */
//    public final synchronized void start() throws UMOException
//    {
//        if (getParent() != null)
//        {
//            parent.start();
//        }
//
//
//        if (!started.get())
//        {
//            starting.set(true);
//            fireSystemEvent(new RegistryNotification(getManagementContext(), RegistryNotification.MANAGER_STARTING));
//            startObjects();
//            started.set(true);
//            starting.set(false);
//
//            fireSystemEvent(new RegistryNotification(getManagementContext(), RegistryNotification.MANAGER_STARTED));
//        }
//    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and connectors
     *
     * @throws org.mule.umo.UMOException if either any of the sessions or connectors fail to stop
     */
//    public final synchronized void stop() throws UMOException
//    {
//        if (getParent() != null)
//        {
//            parent.stop();
//        }
//
//        started.set(false);
//        stopping.set(true);
//        fireSystemEvent(new RegistryNotification(getManagementContext(), RegistryNotification.MANAGER_STOPPING));
//
//        stopObjects();
//        stopping.set(false);
//        fireSystemEvent(new RegistryNotification(getManagementContext(), RegistryNotification.MANAGER_STOPPED));
//    }


    /**
     * {@inheritDoc}
     */
    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) lookupObject(name, UMOConnector.class);
    }


    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpoint(String name)
    {
        //This will grab a new prototype from the context
        UMOEndpoint ep = (UMOEndpoint) lookupObject(name, UMOImmutableEndpoint.class);
        //If endpoint type is not explicitly set, set it here once the object has been requested
        if(ep!=null && ep.getType().equals(UMOEndpoint.ENDPOINT_TYPE_GLOBAL))
        {
            ep.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        }

        return ep;
    }


    /**
     * {@inheritDoc}
     */
    public UMOTransformer lookupTransformer(String name)
    {
        return (UMOTransformer) lookupObject(name, UMOTransformer.class);
    }


    public UMOModel lookupModel(String name)
    {
        return (UMOModel) lookupObject(name, UMOModel.class);
    }

    public Map getModels()
    {
        return lookupCollection(UMOModel.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getConnectors()
    {
        return lookupCollection(UMOConnector.class);
    }

    public Map getAgents()
    {
        return lookupCollection(UMOAgent.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpoints()
    {
        return lookupCollection(UMOImmutableEndpoint.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getServices()
    {
        return lookupCollection(UMODescriptor.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getTransformers()
    {
        return lookupCollection(UMOTransformer.class);
    }

    public UMOAgent lookupAgent(String name)
    {
        return (UMOAgent) lookupObject(name, UMOAgent.class);
    }

    public UMODescriptor lookupService(String name)
    {
        return (UMODescriptor) lookupObject(name, UMODescriptor.class);
    }

    /**
     * Fires a mule 'system' event. These are notifications that are fired because
     * something within the Mule instance happened such as the Model started or the
     * server is being disposed.
     *
     * @param e the event that occurred
     */
    protected void fireSystemEvent(UMOServerNotification e)
    {
        if (getManagementContext() != null)
        {
            getManagementContext().fireNotification(e);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring event: " + e);
        }
    }


    /**
     * New registration method - just pass in the object, the registry
     * will take care of the rest
     */
    public Registration registerMuleObject(Registerable parent, Registerable object) throws RegistrationException
    {
        return new MuleRegistration();
    }

    public void deregisterComponent(String registryId) throws DeregistrationException
    {
        //TODO add method for loading a transport service descriptor. Remember to pass in the registry Context
    }

    public final Object lookupObject(Object key, int scope)
    {
        return lookupObject(key, Object.class, scope);
    }

    public final Object lookupObject(Object key, Class returntype, int scope)
    {
        Object o = null;
        try
        {
            o = doLookupObject(key, returntype);
            if (returntype.isAssignableFrom(o.getClass()))
            {
                return o;
            }
            else
            {
                throw new IllegalArgumentException("Object was found in registry with key: " + key + ". But object was of type: " + o.getClass().getName() + ", not of expected type: " + returntype);
            }
        }
        catch (ObjectNotFoundException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to find object in Registry ID: " + getRegistryId());
            }
            if (getParent() != null && scope > SCOPE_IMMEDIATE)
            {
                if(getParent().isRemote() && scope == SCOPE_REMOTE)
                {
                    o = getParent().lookupObject(key, returntype);
                }
                else if(!getParent().isRemote() && scope >= SCOPE_LOCAL)
                {
                    o = getParent().lookupObject(key, returntype);
                }
            }
            //Legacy behaviour. Try instantiating the class
            if (o == null && key.toString().indexOf(".") > 0)
            {
                try
                {
                    o = ClassUtils.instanciateClass(key.toString(), ClassUtils.NO_ARGS, getClass());
                }
                catch (Exception e1)
                {
                    //logger.error("Failed to reference: " + key, e1);
                }

            }
            return o;
        }
    }

    public final Map lookupCollection(Class returntype, int scope)
    {
        Map collection = doLookupCollection(returntype);
        if (collection == null)
        {
            collection = new HashMap(8);
        }

        if (getParent() != null && scope > SCOPE_IMMEDIATE)
        {
            if(getParent().isRemote() && scope == SCOPE_REMOTE)
            {
                Map collection2 = getParent().lookupCollection(returntype);
                if (collection2 != null)
                {
                    collection.putAll(collection2);
                }
            }
            else if(!getParent().isRemote() && scope >= SCOPE_LOCAL)
            {
                Map collection2 = getParent().lookupCollection(returntype);
                if (collection2 != null)
                {
                    collection.putAll(collection2);
                }
            }
        }

        return collection;
    }

    protected abstract Map doLookupCollection(Class returntype);


    public Object lookupProperty(Object key, int scope)
    {
        Map props = lookupProperties(scope);
        if (props != null)
        {
            return props.get(key);
        }
        return lookupObject(key, Object.class, scope);
    }

    public Map lookupProperties(int scope)
    {
        return (Map) lookupObject(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES, Map.class, scope);
    }

    public Map lookupCollection(Class returntype)
    {
        return lookupCollection(returntype, getDefaultScope());
    }

    public Object lookupObject(Object key)
    {
        return lookupObject(key, getDefaultScope());
    }

    public Object lookupObject(Object key, Class returnType)
    {
        return lookupObject(key, returnType, getDefaultScope());
    }

    public Map lookupProperties()
    {
        return lookupProperties(getDefaultScope());
    }

    public Object lookupProperty(Object key)
    {
        return lookupProperty(key, getDefaultScope());
    }

    /**
     * Initialises all registered agents
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *
     */
    //TODO: Spring is now taking care of the initialisation lifecycle, need to check that we still get this problem
//    protected void initialiseAgents() throws InitialisationException
//    {
//        logger.info("Initialising agents...");
//
//        // Do not iterate over the map directly, as 'complex' agents
//        // may spawn extra agents during initialisation. This will
//        // cause a ConcurrentModificationException.
//        // Use a cursorable iteration, which supports on-the-fly underlying
//        // data structure changes.
//        Collection agentsSnapshot = lookupCollection(UMOAgent.class).values();
//        CursorableLinkedList agentRegistrationQueue = new CursorableLinkedList(agentsSnapshot);
//        CursorableLinkedList.Cursor cursor = agentRegistrationQueue.cursor();
//
//        // the actual agent object refs are the same, so we are just
//        // providing different views of the same underlying data
//
//        try
//        {
//            while (cursor.hasNext())
//            {
//                UMOAgent umoAgent = (UMOAgent) cursor.next();
//
//                int originalSize = agentsSnapshot.size();
//                logger.debug("Initialising agent: " + umoAgent.getName());
//                umoAgent.initialise();
//                // thank you, we are done with you
//                cursor.remove();
//
//                // Direct calls to MuleManager.registerAgent() modify the original
//                // agents map, re-check if the above agent registered any
//                // 'child' agents.
//                int newSize = agentsSnapshot.size();
//                int delta = newSize - originalSize;
//                if (delta > 0)
//                {
//                    // TODO there's some mess going on in
//                    // http://issues.apache.org/jira/browse/COLLECTIONS-219
//                    // watch out when upgrading the commons-collections.
//                    Collection tail = CollectionUtils.retainAll(agentsSnapshot, agentRegistrationQueue);
//                    Collection head = CollectionUtils.subtract(agentsSnapshot, tail);
//
//                    // again, above are only refs, all going back to the original agents map
//
//                    // re-order the queue
//                    agentRegistrationQueue.clear();
//                    // 'spawned' agents first
//                    agentRegistrationQueue.addAll(head);
//                    // and the rest
//                    agentRegistrationQueue.addAll(tail);
//
//                    // update agents map with a new order in case we want to re-initialise
//                    // MuleManager on the fly
//                    for (Iterator it = agentRegistrationQueue.iterator(); it.hasNext();)
//                    {
//                        UMOAgent theAgent = (UMOAgent) it.next();
//                        theAgent.initialise();
//                    }
//                }
//            }
//        }
//        finally
//        {
//            // close the cursor as per JavaDoc
//            cursor.close();
//        }
//        logger.info("Agents Successfully Initialised");
//    }
    protected abstract Object doLookupObject(Object key, Class returntype) throws ObjectNotFoundException;


    public RegistryStore getRegistryStore()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getRegisteredComponents(String parentId, String type)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getRegisteredComponents(String parentId)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Registration getRegisteredComponent(String id)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifyStateChange(String id, int state)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifyPropertyChange(String id, String propertyName, Object propertyValue)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPersistenceMode()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UMOManagementContext getManagementContext()
    {
        if (managementContext == null)
        {
            managementContext = (UMOManagementContext) lookupObject(MuleProperties.OBJECT_MANAGMENT_CONTEXT,
                    UMOManagementContext.class, SCOPE_LOCAL);
        }
        return managementContext;
    }

    public RegistryFacade getParent()
    {
        return parent;
    }

    public void setParent(RegistryFacade registry)
    {
        this.parent = registry;
    }

    public UMOEndpoint createEndpointFromUri(String uri, String type) throws UMOException
    {
        return createEndpointFromUri(new MuleEndpointURI(uri), type);
    }

    public UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException
    {
        uri.initialise();
        UMOEndpoint endpoint = TransportFactory.createEndpoint(uri, type);
        registerEndpoint(endpoint);
        return endpoint;
    }

    public UMOEndpoint getEndpointFromUri(String uri) throws ObjectNotFoundException
    {
        UMOEndpoint endpoint = null;
        if (uri != null)
        {
            endpoint = lookupEndpoint(uri);
        }
        return endpoint;
    }

    public UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException
    {
        String endpointName = uri.getEndpointName();
        if (endpointName != null)
        {
            UMOEndpoint endpoint = lookupEndpoint(endpointName);
            if (endpoint != null)
            {
                if (StringUtils.isNotEmpty(uri.getAddress()))
                {
                    //TODO RM*    endpoint.setEndpointURI(uri);
                }
            }
            return endpoint;
        }

        return null;
    }

    public UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uriIdentifier);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(new MuleEndpointURI(uriIdentifier), type);

        }
        else
        {
            if (!endpoint.getType().equals(type) && !endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER))
            {
                throw new IllegalArgumentException("Endpoint matching: " + uriIdentifier
                        + " is not of type: " + type + ". It is of type: "
                        + endpoint.getType());

            }
        }
        return endpoint;
    }

    public UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uri);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(uri, type);
        }
        return endpoint;
    }

    protected void unsupportedOperation(String operation, Object o) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Registry: " + getRegistryId() + " is read-only so objects cannot be registered or unregistered. Failed to execute operation " + operation + " on object: " + o);
    }

    public void registerConnector(UMOConnector connector) throws UMOException
    {
        unsupportedOperation("registerConnector", connector);
    }

    public UMOConnector unregisterConnector(String connectorName) throws UMOException
    {
        unsupportedOperation("unregisterConnector", connectorName);
        return null;
    }

    public void registerEndpoint(UMOEndpoint endpoint) throws UMOException
    {
        unsupportedOperation("registerEndpoint", endpoint);
    }

    public UMOImmutableEndpoint unregisterEndpoint(String endpointName)
    {
        unsupportedOperation("unregisterEndpoint", endpointName);
        return null;
    }

    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        unsupportedOperation("registerTransformer", transformer);
    }

    public UMOTransformer unregisterTransformer(String transformerName)
    {
        unsupportedOperation("unregistertransformer", transformerName);
        return null;
    }

    public void registerService(UMODescriptor service) throws UMOException
    {
        unsupportedOperation("registerService", service);
    }

    public UMODescriptor unregisterService(String serviceName)
    {
        unsupportedOperation("unregisterService", serviceName);
        return null;
    }

    public void registerModel(UMOModel model) throws UMOException
    {
        unsupportedOperation("registerModel", model);
    }

    public UMOModel unregisterModel(String modelName)
    {
        unsupportedOperation("unregisterModel", modelName);
        return null;
    }

    public void registerAgent(UMOAgent agent) throws UMOException
    {
        unsupportedOperation("registerAgent", agent);
    }

    public UMOAgent unregisterAgent(String agentName) throws UMOException
    {
        unsupportedOperation("unregisterAgent", agentName);
        return null;
    }

    public void registerProperty(Object key, Object value)
    {
        unsupportedOperation("registerProperty", value);
    }

    public void registerProperties(Map props)
    {
        unsupportedOperation("registerProperties", props);
    }

    public void registerObject(Object key, Object value)
    {
        unsupportedOperation("registerObject", value);
    }

    public Object unregisterObject(String key)
    {
        unsupportedOperation("unregisterObject", key);
        return null;
    }

    public final MuleConfiguration getConfiguration()
    {
        MuleConfiguration config = getLocalConfiguration();
        if (config == null && getParent() != null)
        {
            config = getParent().getConfiguration();
        }
        if (config == null)
        {
            config = new MuleConfiguration();
            setConfiguration(config);
        }
        return config;
    }

    public int getDefaultScope()
    {
        return defaultScope;
    }

    public void setDefaultScope(int scope)
    {
        if(scope < SCOPE_IMMEDIATE || scope > SCOPE_REMOTE)
        {
            throw new IllegalArgumentException("Invalid value for scope: "+ scope);
        }
        defaultScope = scope;
    }

    protected abstract MuleConfiguration getLocalConfiguration();
}
