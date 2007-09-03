/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.registry;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ManagementContextAware;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportFactory;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.CollectionUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public abstract class AbstractRegistry implements Registry

{
    private Registry parent;
    /**
     * the unique id for this Registry
     */
    private String id;

    private int defaultScope = DEFAULT_SCOPE;

    protected transient Log logger = LogFactory.getLog(getClass());

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

    protected AbstractRegistry(String id, Registry parent)
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
            lifecycleManager.firePhase(MuleServer.getManagementContext(), Disposable.PHASE_NAME);
            if (getParent() != null)
            {
                parent.dispose();
            }
            else
            {
                //remove this reference once there is no one else left to dispose
                RegistryContext.setRegistry(null);
            }
        }
        catch (UMOException e)
        {
            //TODO
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

        // I don't think it makes sense for the Registry to know about the ManagementContext at this point.
//        UMOManagementContext mc = MuleServer.getManagementContext();
//        if (mc != null)
//        {
//            mc.fireNotification(new RegistryNotification(this, RegistryNotification.REGISTRY_INITIALISING));
//        }
        
        if (id == null)
        {
            logger.warn("No unique id has been set on this registry");
            id = UUID.getUUID();
        }
        try
        {
            doInitialise();
            lifecycleManager.firePhase(MuleServer.getManagementContext(), Initialisable.PHASE_NAME);
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


    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) lookupObject(name);
    }

    public UMOEndpoint lookupEndpoint(String name)
    {
        //This will grab a new prototype from the context
        UMOEndpoint ep = (UMOEndpoint) lookupObject(name);
        //If endpoint type is not explicitly set, set it here once the object has been requested
        if(ep!=null && ep.getType().equals(UMOEndpoint.ENDPOINT_TYPE_GLOBAL))
        {
            ep.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        }

        return ep;
    }

    public UMOTransformer lookupTransformer(String name)
    {
        return (UMOTransformer) lookupObject(name);
    }

    public UMOModel lookupModel(String name)
    {
        return (UMOModel) lookupObject(name);
    }

    public UMOModel lookupSystemModel()
    {
        return lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
    }
    
    public Collection getModels()
    {
        return lookupObjects(UMOModel.class);
    }

    public Collection getConnectors()
    {
        return lookupObjects(UMOConnector.class);
    }

    public Collection getAgents()
    {
        return lookupObjects(UMOAgent.class);
    }

    public Collection getEndpoints()
    {
        return lookupObjects(UMOImmutableEndpoint.class);
    }

    public Collection getServices()
    {
        return lookupObjects(UMODescriptor.class);
    }

    public Collection getTransformers()
    {
        return lookupObjects(UMOTransformer.class);
    }

    public UMOAgent lookupAgent(String name)
    {
        return (UMOAgent) lookupObject(name);
    }

    public UMODescriptor lookupService(String name)
    {
        return (UMODescriptor) lookupObject(name);
    }

    public final Object lookupObject(String key, int scope)
    {
        Object o = doLookupObject(key);
        
        if (o == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to find object in Registry ID: " + getRegistryId());
            }
            if (getParent() != null && scope > SCOPE_IMMEDIATE)
            {
                if(getParent().isRemote() && scope == SCOPE_REMOTE)
                {
                    o = getParent().lookupObject(key);
                }
                else if(!getParent().isRemote() && scope >= SCOPE_LOCAL)
                {
                    o = getParent().lookupObject(key);
                }
            }
        }
        return o;
    }

    public Collection lookupObjects(Class type)
    {
        return lookupObjects(type, getDefaultScope());
    }

    public final Collection lookupObjects(Class type, int scope)
    {
        Collection collection = doLookupObjects(type);
        if (collection == null)
        {
            collection = new ArrayList();
        }

        if (getParent() != null && scope > SCOPE_IMMEDIATE)
        {
            if(getParent().isRemote() && scope == SCOPE_REMOTE)
            {
                Collection collection2 = getParent().lookupObjects(type);
                if (collection2 != null)
                {
                    collection.addAll(collection2);
                }
            }
            else if(!getParent().isRemote() && scope >= SCOPE_LOCAL)
            {
                Collection collection2 = getParent().lookupObjects(type);
                if (collection2 != null)
                {
                    collection = CollectionUtils.union(collection, collection2);
                }
            }
        }

        return collection;
    }

    protected abstract Collection doLookupObjects(Class type);

    public Object lookupObject(String key)
    {
        return lookupObject(key, getDefaultScope());
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
    
    /** @return null if object not found */
    protected abstract Object doLookupObject(String key);

    public Registry getParent()
    {
        return parent;
    }

    public void setParent(Registry registry)
    {
        this.parent = registry;
    }

    /** {@inheritDoc} */
    public UMOEndpoint createEndpointFromUri(String uri, String type) throws UMOException
    {
        return createEndpointFromUri(uri, type, MuleServer.getManagementContext());
    }

    public UMOEndpoint createEndpointFromUri(String uri, String type, UMOManagementContext managementContext) throws UMOException
    {
        return createEndpointFromUri(new MuleEndpointURI(uri), type, managementContext);
    }

    /** {@inheritDoc} */
    public UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException
    {
        return createEndpointFromUri(uri, type, MuleServer.getManagementContext());
    }

    public UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws UMOException
    {
        uri.initialise();
        UMOEndpoint endpoint = TransportFactory.createEndpoint(uri, type, managementContext);
        registerEndpoint(endpoint, managementContext);
        return endpoint;
    }

    /**
     * Return an endpoint, given that endpoint's <em>name</em>.
     *
     * <p>This only returns an endpoint if the argument is a name (at least, when working
     * with Spring).  If the logic is changed to be similar to that of
     * {@link #getEndpointFromUri(org.mule.umo.endpoint.UMOEndpointURI)} below (ie
     * looping through addresses too) then endpoints are also returned if the address
     * matches the argument.  HOWEVER, this breaks other tests (XFire i nparticular).
     * As far as I can tell, this is because the "additional prefix" is lost, so
     * xfire:http://... and http://... are identical.  But I do not have a more
     * complete explanation.
     *
     * @param name The endpoint name
     * @return An endpoint, or null
     * @throws ObjectNotFoundException
     */
    public UMOEndpoint getEndpointFromName(String name) throws ObjectNotFoundException
    {
        if (null != name)
        {
            return lookupEndpoint(name);
        }
        else
        {
            return null;
        }
    }

    public UMOEndpoint getEndpointFromUri(String name) throws ObjectNotFoundException
    {
        return getEndpointFromName(name);
    }

    /**
     * Return an endpoint whose name or address matches the given URI.
     *
     * <p>This is more forgiving than {@link #getEndpointFromUri(String)} above, in that
     * it recognises endpoints via both names and addresses.
     *
     * @param uri
     * @return
     * @throws UMOException
     */
    public UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException
    {
        String name = uri.getEndpointName();
        UMOEndpoint endpoint = getEndpointFromName(name);
        if (null == endpoint)
        {
            String address = uri.getAddress();
            if (null != address)
            {
                Collection endpoints = getEndpoints();
                if (null != name && endpoints.contains(name))
                {
                    throw new IllegalStateException("Endpoint present, but direct lookup failed");
                }

                Iterator it = endpoints.iterator();
                while (it.hasNext())
                {
                    Object value = it.next();
                    if (value instanceof UMOEndpoint)
                    {
                        UMOEndpoint candidate = (UMOEndpoint) value;
                        String candidateAddress = candidate.getEndpointURI().getAddress();
                        if (null != candidateAddress && address.equals(candidateAddress))
                        {
                            if (null == endpoint)
                            {
                                endpoint = candidate;
                            }
                            else
                            {
                                throw new IllegalStateException("Duplicate endpoint URI");
                            }
                        }
                    }
                }
            }
        }
        return endpoint;
    }

    /** {@inheritDoc} */
    public UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException
    {
        return getOrCreateEndpointForUri(uriIdentifier, type, MuleServer.getManagementContext());
    }

    public UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type, UMOManagementContext managementContext) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromName(uriIdentifier);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(new MuleEndpointURI(uriIdentifier), type, managementContext);

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

    /** {@inheritDoc} */
    public UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException
    {
        return getOrCreateEndpointForUri(uri, type, MuleServer.getManagementContext());
    }

    public UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uri);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(uri, type, managementContext);
        }
        return endpoint;
    }

    protected void unsupportedOperation(String operation, Object o) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Registry: " + getRegistryId() + " is read-only so objects cannot be registered or unregistered. Failed to execute operation " + operation + " on object: " + o);
    }

    /** {@inheritDoc} */
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        registerConnector(connector, MuleServer.getManagementContext());
    }

    public void registerConnector(UMOConnector connector, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerConnector", connector);
    }

    public UMOConnector unregisterConnector(String connectorName) throws UMOException
    {
        unsupportedOperation("unregisterConnector", connectorName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerEndpoint(UMOEndpoint endpoint) throws UMOException
    {
        registerEndpoint(endpoint, MuleServer.getManagementContext());
    }

    public void registerEndpoint(UMOEndpoint endpoint, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerEndpoint", endpoint);
    }

    public UMOImmutableEndpoint unregisterEndpoint(String endpointName)
    {
        unsupportedOperation("unregisterEndpoint", endpointName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        registerTransformer(transformer, MuleServer.getManagementContext());
    }

    public void registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerTransformer", transformer);
    }

    public UMOTransformer unregisterTransformer(String transformerName)
    {
        unsupportedOperation("unregistertransformer", transformerName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerService(UMODescriptor service) throws UMOException
    {
        registerService(service, MuleServer.getManagementContext());
    }

    public void registerService(UMODescriptor service, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerService", service);
    }

    public UMODescriptor unregisterService(String serviceName)
    {
        unsupportedOperation("unregisterService", serviceName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerModel(UMOModel model) throws UMOException
    {
        registerModel(model, MuleServer.getManagementContext());
    }

    public void registerModel(UMOModel model, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerModel", model);
    }

    public UMOModel unregisterModel(String modelName)
    {
        unsupportedOperation("unregisterModel", modelName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        registerAgent(agent, MuleServer.getManagementContext());
    }

    public void registerAgent(UMOAgent agent, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerAgent", agent);
    }

    public UMOAgent unregisterAgent(String agentName) throws UMOException
    {
        unsupportedOperation("unregisterAgent", agentName);
        return null;
    }

    public final void registerObject(String key, Object value) throws RegistrationException
    {
        registerObject(key, value, null, null);
    }

    public final void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        registerObject(key, value, metadata, null);
    }

    public final void registerObject(String key, Object value, UMOManagementContext managementContext) throws RegistrationException
    {
        registerObject(key, value, null, managementContext);
    }
    
    public final void registerObject(String key, Object value, Object metadata, UMOManagementContext managementContext) throws RegistrationException
    {
        if (value instanceof ManagementContextAware)
        {
            if (managementContext == null)
            {
                throw new RegistrationException("Attempting to register a ManagementContextAware object without providing a ManagementContext.");
            }
            ((ManagementContextAware) value).setManagementContext(managementContext);
        }
        doRegisterObject(key, value, metadata, managementContext);
    }

    protected void doRegisterObject(String key, Object value, Object metadata, UMOManagementContext managementContext) throws RegistrationException
    {
        unsupportedOperation("doRegisterObject", key);
    }

    public void unregisterObject(String key)
    {
        unsupportedOperation("unregisterObject", key);
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

    public void setConfiguration(MuleConfiguration config)
    {
        unsupportedOperation("setConfiguration", config);
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
