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

import org.mule.api.MuleException;
import org.mule.api.NamedObject;
import org.mule.api.agent.Agent;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.Model;
import org.mule.api.registry.AbstractServiceDescriptor;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.api.service.Service;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerCollection;
import org.mule.transformer.TransformerWeighting;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.ObjectToString;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Adds lookup/register/unregister methods for Mule-specific entities to the standard
 * Registry interface.
 * 
 * TODO MULE-2228 "Java-based configuration mechanism for Mule 2.0" will extend/build 
 * upon this interface.
 */
public class MuleRegistryHelper implements MuleRegistry, Initialisable, Disposable
{
    private static final ObjectToString objectToString = new ObjectToString();
    private static final ObjectToByteArray objectToByteArray = new ObjectToByteArray();

    protected Map transformerListCache = new ConcurrentHashMap(8);
    protected Map exactTransformerCache = new ConcurrentHashMap(8);
    
    /** A reference to Mule's internal registry */
    private Registry registry;
    
    protected transient Log logger = LogFactory.getLog(MuleRegistryHelper.class);
    
    public MuleRegistryHelper(Registry registry)
    {
        this.registry = registry;
    }
    
    public void initialise() throws InitialisationException
    {
        // no-op
    }

    public void dispose()
    {
        exactTransformerCache.clear();
        transformerListCache.clear();
    }

    public Connector lookupConnector(String name)
    {
        return (Connector) registry.lookupObject(name);
    }

    /**
     * Removed this method from {@link Registry} API as it should only be used
     * internally and may confuse users. The {@link EndpointFactory} should be used
     * for creating endpoints.<br/><br/> Looks up an returns endpoints registered in the
     * registry by their idendifier (currently endpoint name)<br/><br/ <b>NOTE:
     * This method does not create new endpoint instances, but rather returns
     * existing endpoint instances that have been registered. This lookup method
     * should be avoided and the intelligent, role specific endpoint lookup methods
     * should be used instead.<br/><br/>
     * 
     * @param name the idendtifer/name used to register endpoint in registry
     * @see #lookupInboundEndpoint(String, org.mule.api.MuleContext)
     * @see #lookupResponseEndpoint(String, org.mule.api.MuleContext)
     */
    public ImmutableEndpoint lookupEndpoint(String name)
    {
        Object obj = registry.lookupObject(name);
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
        Object o = registry.lookupObject(name);
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
        return (EndpointFactory) registry.lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }

    public Transformer lookupTransformer(String name)
    {
        return (Transformer) registry.lookupObject(name);
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
        return (Model) registry.lookupObject(name);
    }

    public Model lookupSystemModel()
    {
        return lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
    }

    public Collection getModels()
    {
        return registry.lookupObjects(Model.class);
    }

    public Collection getConnectors()
    {
        return registry.lookupObjects(Connector.class);
    }

    public Collection getAgents()
    {
        return registry.lookupObjects(Agent.class);
    }

    public Collection getEndpoints()
    {
        return registry.lookupObjects(ImmutableEndpoint.class);
    }

    public Collection getTransformers()
    {
        return registry.lookupObjects(Transformer.class);
    }

    public Agent lookupAgent(String name)
    {
        return (Agent) registry.lookupObject(name);
    }

    public Service lookupService(String name)
    {
        return (Service) registry.lookupObject(name);
    }

    public Collection/*<Service>*/ lookupServices()
    {
        return lookupObjects(Service.class);
    }

    public Collection/*<Service>*/ lookupServices(String model)
    {
        Collection/*<Service>*/ services = lookupServices();
        List modelServices = new ArrayList();
        Iterator it = services.iterator();
        Service service;
        while (it.hasNext())
        {
            service = (Service) it.next();
            // TODO Make this comparison more robust.
            if (model.equals(service.getModel().getName()))
            {
                modelServices.add(service);
            }
        }
        return modelServices;
    }

    public final void registerTransformer(Transformer transformer) throws MuleException
    {
        if (transformer instanceof DiscoverableTransformer)
        {
            exactTransformerCache.clear();
            transformerListCache.clear();
        }
        
        //TODO should we always throw an exception if an object already exists
        if (lookupTransformer(transformer.getName()) != null)
        {
            throw new RegistrationException(CoreMessages.objectAlreadyRegistered("transformer: " +
                    transformer.getName(), lookupTransformer(transformer.getName()), transformer).getMessage());
        }
        registry.registerObject(getName(transformer), transformer, Transformer.class);
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

    /** Looks up the service descriptor from a singleton cache and creates a new one if not found. */
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        String key = new AbstractServiceDescriptor.Key(name, overrides).getKey();
        //TODO If we want these descriptors loaded form Spring we need to checnge the key mechanism
        //and the scope, and then deal with circular reference issues.
        ServiceDescriptor sd = (ServiceDescriptor) registry.lookupObject(key);

        synchronized (this)
        {
            if (sd == null)
            {
                sd = createServiceDescriptor(type, name, overrides);
                try
                {
                    registry.registerObject(key, sd, ServiceDescriptor.class);
                }
                catch (RegistrationException e)
                {
                    throw new ServiceException(e.getI18nMessage(), e);
                }
            }
        }
        return sd;
    }

    /** @deprecated ServiceDescriptors will be created upon bundle startup for OSGi. */
    protected ServiceDescriptor createServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if (props == null)
        {
            throw new ServiceException(CoreMessages.failedToLoad(type + " " + name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides, registry, null);
    }

    //@java.lang.Override
    public void registerAgent(Agent agent) throws MuleException
    {
        registry.registerObject(getName(agent), agent, Agent.class);
    }

    //@java.lang.Override
    public void registerConnector(Connector connector) throws MuleException
    {
        registry.registerObject(getName(connector), connector, Connector.class);
    }

    //@java.lang.Override
    public void registerEndpoint(ImmutableEndpoint endpoint) throws MuleException
    {
        registry.registerObject(getName(endpoint), endpoint, ImmutableEndpoint.class);
    }

    public void registerEndpointBuilder(String name, EndpointBuilder builder) throws MuleException
    {
        registry.registerObject(name, builder, EndpointBuilder.class);
    }

    //@java.lang.Override
    public void registerModel(Model model) throws MuleException
    {
        registry.registerObject(getName(model), model, Model.class);
    }

    //@java.lang.Override
    public void registerService(Service service) throws MuleException
    {
        registry.registerObject(getName(service), service, Service.class);
    }

    //@java.lang.Override
    public void unregisterService(String serviceName) throws MuleException
    {
        registry.unregisterObject(serviceName, Service.class);
    }


    //@java.lang.Override
    public void unregisterAgent(String agentName) throws MuleException
    {
        registry.unregisterObject(agentName, Agent.class);
    }

    //@java.lang.Override
    public void unregisterConnector(String connectorName) throws MuleException
    {
        registry.unregisterObject(connectorName, Connector.class);
    }

    //@java.lang.Override
    public void unregisterEndpoint(String endpointName) throws MuleException
    {
        registry.unregisterObject(endpointName, ImmutableEndpoint.class);
    }

    //@java.lang.Override
    public void unregisterModel(String modelName) throws MuleException
    {
        registry.unregisterObject(modelName, Model.class);
    }

    //@java.lang.Override
    public void unregisterTransformer(String transformerName) throws MuleException
    {
        Transformer transformer = lookupTransformer(transformerName);
        if (transformer instanceof DiscoverableTransformer)
        {
            exactTransformerCache.clear();
            transformerListCache.clear();
        }
        registry.unregisterObject(transformerName, Transformer.class);
    }

    //@java.lang.Override
//    public Transformer lookupTransformer(String name)
//    {
//        Transformer transformer = super.lookupTransformer(name);
//        if (transformer != null)
//        {
//            try
//            {
//                if (transformer.getEndpoint() != null)
//                {
//                    throw new IllegalStateException("Endpoint cannot be set");
//                }
////                Map props = BeanUtils.describe(transformer);
////                props.remove("endpoint");
////                props.remove("strategy");
////                transformer = (Transformer)ClassUtils.instanciateClass(transformer.getClass(), ClassUtils.NO_ARGS);
//                //TODO: friggin' cloning
//                transformer = (Transformer) BeanUtils.cloneBean(transformer);
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
//        return transformer;
//    }

    ////////////////////////////////////////////////////////////////////////////
    // Delegate to internal registry
    ////////////////////////////////////////////////////////////////////////////
    
    public Object lookupObject(Class type) throws RegistrationException
    {
        return registry.lookupObject(type);
    }

    public Object lookupObject(String key)
    {
        return registry.lookupObject(key);
    }

    public Collection lookupObjects(Class type)
    {
        return registry.lookupObjects(type);
    }

    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        registry.registerObject(key, value, metadata);
    }

    public void registerObject(String key, Object value) throws RegistrationException
    {
        registry.registerObject(key, value);
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        registry.registerObjects(objects);
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        registry.unregisterObject(key, metadata);
    }

    public void unregisterObject(String key) throws RegistrationException
    {
        registry.unregisterObject(key);
    }
    
    protected String getName(Object obj)
    {
        String name = null;
        if (obj instanceof NamedObject)
        {
            name = ((NamedObject) obj).getName();
        }
        if (StringUtils.isBlank(name))
        {
            name = obj.getClass().getName() + ":" + UUID.getUUID();
        }
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    ////////////////////////////////////////////////////////////////////////////
    
    public String getRegistryId()
    {
        return this.toString();
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean isRemote()
    {
        return false;
    }
}


