/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.model.Model;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.service.Service;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.config.MuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerCollection;
import org.mule.transformer.TransformerWeighting;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.ObjectToString;
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


public abstract class AbstractRegistry implements Registry
{
    private static final ObjectToString objectToString = new ObjectToString();
    private static final ObjectToByteArray objectToByteArray = new ObjectToByteArray();

    private Registry parent;
    /** the unique id for this Registry */
    private String id;

    private int defaultScope = DEFAULT_SCOPE;

    protected transient Log logger = LogFactory.getLog(getClass());

    protected LifecycleManager lifecycleManager;
    protected Map transformerListCache = new ConcurrentHashMap(8);
    protected Map exactTransformerCache = new ConcurrentHashMap(8);

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

    protected abstract LifecycleManager createLifecycleManager();

    protected LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
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
            exactTransformerCache.clear();
            transformerListCache.clear();

            doDispose();
            lifecycleManager.firePhase(MuleServer.getMuleContext(), Disposable.PHASE_NAME);
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
        catch (MuleException e)
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

    public final LifecycleTransitionResult initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

//        if (getParent() != null)
//        {
//            parent.initialise();
//        }

        // I don't think it makes sense for the Registry to know about the MuleContext at this point.
        // MuleContext mc = MuleServer.getMuleContext();
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
            lifecycleManager.firePhase(MuleServer.getMuleContext(), Initialisable.PHASE_NAME);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        return LifecycleTransitionResult.OK;
    }

    protected void doInitialise() throws InitialisationException
    {

    }


    public Connector lookupConnector(String name)
    {
        return (Connector) lookupObject(name);
    }

    public ImmutableEndpoint lookupEndpoint(String name)
    {
        Object obj = lookupObject(name);
        if (obj instanceof ImmutableEndpoint)
        {
            return (ImmutableEndpoint) obj;
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

    public EndpointBuilder lookupEndpointBuilder(String name)
    {
        Object o = lookupObject(name);
        if (o instanceof EndpointBuilder)
        {
            logger.debug("Global endpoint EndpointBuilder for name: " + name + " found");
            return (EndpointBuilder) o;
        }
        else
        {
            logger.debug("No endpoint builder with the name: " + name + " found.");
            return null;
        }
    }

    public EndpointFactory lookupEndpointFactory()
    {
        return (EndpointFactory) lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }

    public Transformer lookupTransformer(String name)
    {
        return (Transformer) lookupObject(name);
    }

    /** {@inheritDoc} */
    public Transformer lookupTransformer(Class inputType, Class outputType) throws TransformerException
    {
        Transformer result = (Transformer) exactTransformerCache.get(inputType.getName() + outputType.getName());
        if (result != null)
        {
            return result;
        }
        List trans = lookupTransformers(inputType, outputType);

        result = getNearestTransformerMatch(trans, inputType, outputType);
        //If an exact mach is not found, we have a 'second pass' transformer that can be used to converting to String or
        //byte[]
        Transformer secondPass = null;

        if (result == null)
        {
            //If no transformers were found but the outputType type is String or byte[] we can perform a more general search
            // using Object.class and then convert to String or byte[] using the second pass transformer
            if (outputType.equals(String.class))
            {
                secondPass = objectToString;
            }
            else if (outputType.equals(byte[].class))
            {
                secondPass = objectToByteArray;
            }
            else
            {
                throw new TransformerException(CoreMessages.noTransformerFoundForMessage(inputType, outputType));
            }
            //Perform a more general search
            trans = lookupTransformers(inputType, Object.class);

            result = getNearestTransformerMatch(trans, inputType, outputType);
            if (result != null)
            {
                result = new TransformerCollection(new Transformer[]{result, secondPass});
            }
        }

        if (result != null)
        {
            exactTransformerCache.put(inputType.getName() + outputType.getName(), result);
        }
        return result;
    }

    protected Transformer getNearestTransformerMatch(List trans, Class input, Class output) throws TransformerException
    {
        if (trans.size() > 1)
        {
            TransformerWeighting weighting = null;
            for (Iterator iterator = trans.iterator(); iterator.hasNext();)
            {
                Transformer transformer = (Transformer) iterator.next();
                TransformerWeighting current = new TransformerWeighting(input, output, transformer);
                if (weighting == null)
                {
                    weighting = current;
                }
                else
                {
                    int compare = current.compareTo(weighting);
                    if (compare == 1)
                    {
                        weighting = current;
                    }
                    else if (compare == 0)
                    {
                        //We may have two transformers that are exactly the same, in which case we can use either i.e. use the current
                        if (!weighting.getTransformer().getClass().equals(current.getTransformer().getClass()))
                        {
                            throw new TransformerException(CoreMessages.transformHasMultipleMatches(input, output,
                                    current.getTransformer(), weighting.getTransformer()));
                        }
                    }
                }
            }
            return weighting.getTransformer();
        }
        else if (trans.size() == 0)
        {
            return null;
        }
        else
        {
            return (Transformer) trans.get(0);
        }
    }

    /** {@inheritDoc} */
    public List lookupTransformers(Class input, Class output)
    {
        List results = (List) transformerListCache.get(input.getName() + output.getName());
        if (results != null)
        {
            return results;
        }

        results = new ArrayList(2);
        Collection transformers = getTransformers();
        for (Iterator itr = transformers.iterator(); itr.hasNext();)
        {
            Transformer t = (Transformer) itr.next();
            //The transformer must have the DiscoveryTransformer interface if we are going to
            //find it here
            if (!(t instanceof DiscoverableTransformer))
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

        transformerListCache.put(input.getName() + output.getName(), results);
        return results;
    }

    public Model lookupModel(String name)
    {
        return (Model) lookupObject(name);
    }

    public Model lookupSystemModel()
    {
        return lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
    }

    public Collection getModels()
    {
        return lookupObjects(Model.class);
    }

    public Collection getConnectors()
    {
        return lookupObjects(Connector.class);
    }

    public Collection getAgents()
    {
        return lookupObjects(Agent.class);
    }

    public Collection getEndpoints()
    {
        return lookupObjects(ImmutableEndpoint.class);
    }

    public Collection getTransformers()
    {
        return lookupObjects(Transformer.class);
    }

    public Agent lookupAgent(String name)
    {
        return (Agent) lookupObject(name);
    }

    public Service lookupService(String name)
    {
        return (Service) lookupObject(name);
    }

    public Collection/*<Service>*/ lookupComponents()
    {
        return lookupObjects(Service.class);
    }

    public Collection/*<Service>*/ lookupComponents(String model)
    {
        Collection/*<Service>*/ components = lookupComponents();
        List modelComponents = new ArrayList();
        Iterator it = components.iterator();
        Service service;
        while (it.hasNext())
        {
            service = (Service) it.next();
            // TODO Make this comparison more robust.
            if (model.equals(service.getModel().getName()))
            {
                modelComponents.add(service);
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

    public final Object lookupObject(Class type) throws RegistrationException
    {
        return lookupObject(type, getDefaultScope());
    }

    /** 
     * Look up a single object by type.  
     * @return null if no object is found
     * @throws RegistrationException if more than one object is found
     */
    public final Object lookupObject(Class type, int scope) throws RegistrationException
    {
        Collection collection = lookupObjects(type, scope);
        if (collection == null || collection.size() < 1)
        {
            return null;
        }
        else if (collection.size() > 1)
        {
            throw new RegistrationException("More than one object of type " + type + " was found in registry, but only 1 was expected.");
        }
        else
        {
            return collection.iterator().next();
        }
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
     * @throws org.mule.api.lifecycle.InitialisationException
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
    // Collection agentsSnapshot = lookupCollection(Agent.class).values();
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
    // Agent umoAgent = (Agent) cursor.next();
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
    // Agent theAgent = (Agent) it.next();
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

    protected void unsupportedOperation(String operation, Object o) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException(
                "Registry: "
                        + getRegistryId()
                        + " is read-only so objects cannot be registered or unregistered. Failed to execute operation "
                        + operation + " on object: " + o);
    }

    public final void registerObject(String key, Object value) throws RegistrationException
    {
        registerObject(key, value, null);
    }

    public final void registerObject(String key,
                                     Object value,
                                     Object metadata) throws RegistrationException
    {
        
        logger.debug("registerObject: key=" + key + " value=" + value + " metadata=" + metadata);
        if (value instanceof MuleContextAware)
        {
            ((MuleContextAware) value).setMuleContext(MuleServer.getMuleContext());
        }
        doRegisterObject(key, value, metadata);
    }

    protected abstract void doRegisterObject(String key,
                                            Object value,
                                            Object metadata) throws RegistrationException;
    
    public final void registerTransformer(Transformer transformer) throws MuleException
    {
        if (transformer instanceof DiscoverableTransformer)
        {
            exactTransformerCache.clear();
            transformerListCache.clear();
        }
        doRegisterTransformer(transformer);

    }

    protected abstract void doRegisterTransformer(Transformer transformer) throws MuleException;

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

    /**
     * TODO MULE-2162
     *
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
    
    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    public final String getRegistryId()
    {
        return id;
    }

    public Registry getParent()
    {
        return parent;
    }

    public void setParent(Registry registry)
    {
        this.parent = registry;
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
}
