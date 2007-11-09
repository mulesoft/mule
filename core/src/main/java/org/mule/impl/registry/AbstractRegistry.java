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
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOHeaderOnlyTransformer;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.CollectionUtils;
import org.mule.util.UUID;
import org.mule.util.properties.PropertyExtractorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** TODO */
public abstract class AbstractRegistry implements Registry

{

    private Registry parent;
    /** the unique id for this Registry */
    private String id;

    private int defaultScope = DEFAULT_SCOPE;

    protected transient Log logger = LogFactory.getLog(getClass());

    protected UMOLifecycleManager lifecycleManager;
    protected Map transformerCache = new ConcurrentHashMap(8);

    /** Default Constructor */
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
        // TODO lifecycleManager.checkPhase(Disposable.PHASE_NAME);

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
                // remove this reference once there is no one else left to dispose
                RegistryContext.setRegistry(null);
                PropertyExtractorManager.clear();
            }
        }
        catch (UMOException e)
        {
            // TODO
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

//        if (getParent() != null)
//        {
//            parent.initialise();
//        }

        // I don't think it makes sense for the Registry to know about the ManagementContext at this point.
        // UMOManagementContext mc = MuleServer.getManagementContext();
        // if (mc != null)
        // {
        // mc.fireNotification(new RegistryNotification(this, RegistryNotification.REGISTRY_INITIALISING));
        // }

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


    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) lookupObject(name);
    }

    public UMOImmutableEndpoint lookupEndpoint(String name, UMOManagementContext managementContext)
    {
        Object obj = lookupObject(name);
        if (obj instanceof UMOImmutableEndpoint)
        {
            return (UMOImmutableEndpoint) obj;
        }
        else
        {
            logger.debug("No endpoint with the name: "
                         + name
                         + "found.  If "
                         + name
                         + " is a global endpoint you should use the EndpointFactory to create endpoint instances from global endpoints.");
            return null;
        }
    }

    public UMOImmutableEndpoint lookupEndpoint(String name)
    {
        return (UMOImmutableEndpoint) lookupEndpoint(name, MuleServer.getManagementContext());
    }
    
    public UMOEndpointBuilder lookupEndpointBuilder(String name)
    {
        // TODO DF: This is spring specific because if uses "&" to lookup FactoryBean and thus builder.
        Object o = lookupObject(name);
        if (o instanceof UMOEndpointBuilder)
        {
            logger.debug("Global endpoint EndpointBuilder for name: " + name + "found");
            return (UMOEndpointBuilder) o;
        }
        else
        {
            logger.debug("Global endpoint EndpointBuilder not found, attempting to lookup named concrete endpoint builder with name: "
                         + name);
            UMOEndpointBuilder endpointBuilder = (UMOEndpointBuilder) lookupObject("&" + name);
            if (endpointBuilder != null)
            {
                logger.debug("Endpoint builder for concrete endpoint with name: " + name + " found.");
            }
            else
            {
                logger.debug("Endpoint builder for concrete endpoint with name: " + name + " NOT found.");
            }
            return endpointBuilder;
        }
    }

    public UMOEndpointFactory lookupEndpointFactory()
    {
        return (UMOEndpointFactory) lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }

    public UMOTransformer lookupTransformer(String name)
    {
        return (UMOTransformer) lookupObject(name);
    }

    public List lookupTransformers(Class input, Class output)
    {
        // TODO: Optimize & Cache

        List results = (List)transformerCache.get(input.getName() + output.getName());
        if(results!=null)
        {
            return results;
        }

        results = new ArrayList(2);
        Collection transformers = getTransformers();
        for (Iterator itr = transformers.iterator(); itr.hasNext();)
        {
            UMOTransformer t = (UMOTransformer) itr.next();
            if (t instanceof UMOHeaderOnlyTransformer)
            {
                continue;
            }
            Class c = t.getReturnClass();
            //TODO RM* this sohuld be an exception
            if (c == null)
            {
                c = Object.class;
            }
            if (output.isAssignableFrom(c)
                    && t.isSourceTypeSupported(input))
            {
                results.add(t);
            }
        }

        transformerCache.put(input.getName() + output.getName(), results);
        return results;
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

    public Collection getTransformers()
    {
        return lookupObjects(UMOTransformer.class);
    }

    public UMOAgent lookupAgent(String name)
    {
        return (UMOAgent) lookupObject(name);
    }

    public UMOComponent lookupComponent(String name)
    {
        return (UMOComponent) lookupObject(name);
    }

    public Collection/*<UMOComponent>*/ lookupComponents()
    {
        return lookupObjects(UMOComponent.class);
    }

    public Collection/*<UMOComponent>*/ lookupComponents(String model)
    {
        Collection/*<UMOComponent>*/ components = lookupComponents();
        List modelComponents = new ArrayList();
        Iterator it = components.iterator();
        UMOComponent component;
        while (it.hasNext())
        {
            component = (UMOComponent) it.next();
            // TODO Make this comparison more robust.
            if (model.equals(component.getModel().getName()))
            {
                modelComponents.add(component);
            }
        }
        return modelComponents;
    }

    public final Object lookupObject(String key, int scope)
    {
        logger.debug("lookupObject: key=" + key + " scope=" + scope);
        Object o = doLookupObject(key);

        if (o == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to find object in Registry ID: " + getRegistryId());
            }
            if (getParent() != null && scope > SCOPE_IMMEDIATE)
            {
                if (getParent().isRemote() && scope == SCOPE_REMOTE)
                {
                    o = getParent().lookupObject(key);
                }
                else if (!getParent().isRemote() && scope >= SCOPE_LOCAL)
                {
                    o = getParent().lookupObject(key);
                }
            }
        }
        return o;
    }

    public final Collection lookupObjects(Class type)
    {
        return lookupObjects(type, getDefaultScope());
    }

    public final Collection lookupObjects(Class type, int scope)
    {
        logger.debug("lookupObjects: type=" + type + " scope=" + scope);
        Collection collection = doLookupObjects(type);
        if (collection == null)
        {
            collection = new ArrayList();
        }

        if (getParent() != null && scope > SCOPE_IMMEDIATE)
        {
            if (getParent().isRemote() && scope == SCOPE_REMOTE)
            {
                Collection collection2 = getParent().lookupObjects(type);
                if (collection2 != null)
                {
                    collection.addAll(collection2);
                }
            }
            else if (!getParent().isRemote() && scope >= SCOPE_LOCAL)
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
     */
    // TODO: Spring is now taking care of the initialisation lifecycle, need to check that we still get this
    // problem
    // protected void initialiseAgents() throws InitialisationException
    // {
    // logger.info("Initialising agents...");
    //
    // // Do not iterate over the map directly, as 'complex' agents
    // // may spawn extra agents during initialisation. This will
    // // cause a ConcurrentModificationException.
    // // Use a cursorable iteration, which supports on-the-fly underlying
    // // data structure changes.
    // Collection agentsSnapshot = lookupCollection(UMOAgent.class).values();
    // CursorableLinkedList agentRegistrationQueue = new CursorableLinkedList(agentsSnapshot);
    // CursorableLinkedList.Cursor cursor = agentRegistrationQueue.cursor();
    //
    // // the actual agent object refs are the same, so we are just
    // // providing different views of the same underlying data
    //
    // try
    // {
    // while (cursor.hasNext())
    // {
    // UMOAgent umoAgent = (UMOAgent) cursor.next();
    //
    // int originalSize = agentsSnapshot.size();
    // logger.debug("Initialising agent: " + umoAgent.getName());
    // umoAgent.initialise();
    // // thank you, we are done with you
    // cursor.remove();
    //
    // // Direct calls to MuleManager.registerAgent() modify the original
    // // agents map, re-check if the above agent registered any
    // // 'child' agents.
    // int newSize = agentsSnapshot.size();
    // int delta = newSize - originalSize;
    // if (delta > 0)
    // {
    // // TODO there's some mess going on in
    // // http://issues.apache.org/jira/browse/COLLECTIONS-219
    // // watch out when upgrading the commons-collections.
    // Collection tail = CollectionUtils.retainAll(agentsSnapshot, agentRegistrationQueue);
    // Collection head = CollectionUtils.subtract(agentsSnapshot, tail);
    //
    // // again, above are only refs, all going back to the original agents map
    //
    // // re-order the queue
    // agentRegistrationQueue.clear();
    // // 'spawned' agents first
    // agentRegistrationQueue.addAll(head);
    // // and the rest
    // agentRegistrationQueue.addAll(tail);
    //
    // // update agents map with a new order in case we want to re-initialise
    // // MuleManager on the fly
    // for (Iterator it = agentRegistrationQueue.iterator(); it.hasNext();)
    // {
    // UMOAgent theAgent = (UMOAgent) it.next();
    // theAgent.initialise();
    // }
    // }
    // }
    // }
    // finally
    // {
    // // close the cursor as per JavaDoc
    // cursor.close();
    // }
    // logger.info("Agents Successfully Initialised");
    // }
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

    protected void unsupportedOperation(String operation, Object o) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException(
                "Registry: "
                        + getRegistryId()
                        + " is read-only so objects cannot be registered or unregistered. Failed to execute operation "
                        + operation + " on object: " + o);
    }

    /** @deprecated  */
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        registerConnector(connector, MuleServer.getManagementContext());
    }

    public void registerConnector(UMOConnector connector, UMOManagementContext managementContext)
            throws UMOException
    {
        unsupportedOperation("registerConnector", connector);
    }

    public UMOConnector unregisterConnector(String connectorName) throws UMOException
    {
        unsupportedOperation("unregisterConnector", connectorName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerEndpoint(UMOImmutableEndpoint endpoint) throws UMOException
    {
        registerEndpoint(endpoint, MuleServer.getManagementContext());
    }

    public void registerEndpoint(UMOImmutableEndpoint endpoint, UMOManagementContext managementContext)
            throws UMOException
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

    public void registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext)
            throws UMOException
    {
        unsupportedOperation("registerTransformer", transformer);
    }

    public UMOTransformer unregisterTransformer(String transformerName)
    {
        unsupportedOperation("unregistertransformer", transformerName);
        return null;
    }

    /** {@inheritDoc} */
    public void registerComponent(UMOComponent component, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerComponent", component);
    }

    public UMOComponent unregisterComponent(String componentName)
    {
        unsupportedOperation("unregisterComponent", componentName);
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

    public final void registerObject(String key, Object value, UMOManagementContext managementContext)
            throws RegistrationException
    {
        registerObject(key, value, null, managementContext);
    }

    public final void registerObject(String key,
                                     Object value,
                                     Object metadata,
                                     UMOManagementContext managementContext) throws RegistrationException
    {
        logger.debug("registerObject: key=" + key + " value=" + value + " metadata=" + metadata
                + " managementContext=" + managementContext);
        if (value instanceof ManagementContextAware)
        {
            if (managementContext == null)
            {
                throw new RegistrationException(
                        "Attempting to register a ManagementContextAware object without providing a ManagementContext.");
            }
            ((ManagementContextAware) value).setManagementContext(managementContext);
        }
        doRegisterObject(key, value, metadata, managementContext);
    }

    protected void doRegisterObject(String key,
                                    Object value,
                                    Object metadata,
                                    UMOManagementContext managementContext) throws RegistrationException
    {
        unsupportedOperation("doRegisterObject", key);
    }

    public void unregisterObject(String key)
    {
        unsupportedOperation("unregisterObject", key);
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        unsupportedOperation("registryObjects", objects);
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
        if (scope < SCOPE_IMMEDIATE || scope > SCOPE_REMOTE)
        {
            throw new IllegalArgumentException("Invalid value for scope: " + scope);
        }
        defaultScope = scope;
    }

    /**
     * TODO MULE-2162
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    protected MuleConfiguration getLocalConfiguration()
    {
        Collection collection = lookupObjects(MuleConfiguration.class);
        if (collection == null)
        {
            logger.warn("No MuleConfiguration was found in registry");
            return null;
        }

        if (collection.size() > 1)
        {
            logger.warn("More than one MuleConfiguration was found in registry");
        }
        return (MuleConfiguration) collection.iterator().next();
    }

    /** {@inheritDoc} */
//    public TransactionManager getTransactionManager()
//    {
//        Map m = applicationContext.getBeansOfType(TransactionManager.class);
//        if (m.size() > 0)
//        {
//            return (TransactionManager) m.values().iterator().next();
//        }
//        return null;
//    }


    public void registerEndpointBuilder(String name, UMOEndpointBuilder builder, UMOManagementContext managementContext) throws UMOException
    {
        unsupportedOperation("registerEndpointBuilder", builder);
    }

}
