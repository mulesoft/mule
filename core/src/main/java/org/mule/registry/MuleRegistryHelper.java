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
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.service.Service;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 */
public class MuleRegistryHelper implements MuleRegistry, Initialisable, Disposable
{
    /**
     * A reference to Mule's internal registry
     */
    private DefaultRegistryBroker registry;

    protected Map<String, List<Transformer>> transformerListCache = new ConcurrentHashMap/*<String, List<Transformer>>*/(8);


    protected transient Log logger = LogFactory.getLog(MuleRegistryHelper.class);

    public MuleRegistryHelper(DefaultRegistryBroker registry)
    {
        this.registry = registry;
    }

    public void initialise() throws InitialisationException
    {
        //no-op
    }

    public void dispose()
    {
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


    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    public Transformer lookupTransformer(Class inputType, Class outputType) throws TransformerException
    {
        return lookupTransformer(new SimpleDataType(inputType), new SimpleDataType(outputType));
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    public List<Transformer> lookupTransformers(Class input, Class output)
    {
        return lookupTransformers(new SimpleDataType(input), new SimpleDataType(output));
    }

    /**
     * {@inheritDoc}
     */
    public Transformer lookupTransformer(DataType source, DataType result) throws TransformerException
    {

        Transformer trans;
        List<TransformerResolver> resolvers = (List<TransformerResolver>) lookupObjects(TransformerResolver.class);
        Collections.sort(resolvers, new TransformerResolverComarator());
        for (TransformerResolver resolver : resolvers)
        {
            try
            {
                trans = resolver.resolve(source, result);
                if (trans != null)
                {
                    return trans;
                }
            }
            catch (ResolverException e)
            {
                throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, result), e);
            }
        }
        throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, result));
    }

    /**
     * {@inheritDoc}
     */
    public List<Transformer> lookupTransformers(DataType source, DataType result)
    {
        List<Transformer> results = transformerListCache.get(source.toString() + result.toString());
        if (results != null)
        {
            return results;
        }

        results = new ArrayList<Transformer>(2);
        Collection<Transformer> transformers = getTransformers();
        for (Transformer t : transformers)
        {
            //The transformer must have the DiscoveryTransformer interface if we are going to
            //find it here
            if (!(t instanceof DiscoverableTransformer))
            {
                continue;
            }
            DataType dt = t.getReturnDataType();
//            Class c = t.getReturnClass();
//            //TODO RM* this sohuld be an exception
//            if (c == null)
//            {
//                c = Object.class;
//            }
            if (result.isCompatibleWith(dt) && t.isSourceDataTypeSupported(source))
            {
                results.add(t);
            }
        }

        transformerListCache.put(source.toString() + result.toString(), results);
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

    public Collection<Model> getModels()
    {
        return registry.lookupObjects(Model.class);
    }

    public Collection<Connector> getConnectors()
    {
        return registry.lookupObjects(Connector.class);
    }

    public Collection<Agent> getAgents()
    {
        return registry.lookupObjects(Agent.class);
    }

    public Collection<ImmutableEndpoint> getEndpoints()
    {
        return registry.lookupObjects(ImmutableEndpoint.class);
    }

    public Collection<Transformer> getTransformers()
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

    public Collection<Service> lookupServices()
    {
        return lookupObjects(Service.class);
    }

    public Collection<Service> lookupServices(String model)
    {
        Collection<Service> services = lookupServices();
        List<Service> modelServices = new ArrayList<Service>();
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
        registry.registerObject(getName(transformer), transformer, Transformer.class);
        notifyTransformerResolvers(transformer, TransformerResolver.RegistryAction.ADDED);
    }

    protected void notifyTransformerResolvers(Transformer t, TransformerResolver.RegistryAction action)
    {
        if (t instanceof DiscoverableTransformer)
        {
            Collection<TransformerResolver> resolvers = lookupObjects(TransformerResolver.class);
            for (TransformerResolver resolver : resolvers)
            {
                resolver.transformerChange(t, action);
            }
            transformerListCache.clear();
        }
    }

    /**
     * Looks up the service descriptor from a singleton cache and creates a new one if not found.
     */
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        String key = new AbstractServiceDescriptor.Key(name, overrides).getKey();
        //TODO If we want these descriptors loaded from Spring we need to change the key mechanism
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

    /**
     * @deprecated ServiceDescriptors will be created upon bundle startup for OSGi.
     */
    protected ServiceDescriptor createServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if (props == null)
        {
            throw new ServiceException(CoreMessages.failedToLoad(type + " " + name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides, this, null);
    }

    public void registerAgent(Agent agent) throws MuleException
    {
        registry.registerObject(getName(agent), agent, Agent.class);
    }

    public void registerConnector(Connector connector) throws MuleException
    {
        registry.registerObject(getName(connector), connector, Connector.class);
    }

    public void registerEndpoint(ImmutableEndpoint endpoint) throws MuleException
    {
        registry.registerObject(getName(endpoint), endpoint, ImmutableEndpoint.class);
    }

    public void registerEndpointBuilder(String name, EndpointBuilder builder) throws MuleException
    {
        registry.registerObject(name, builder, EndpointBuilder.class);
    }

    public void registerModel(Model model) throws MuleException
    {
        registry.registerObject(getName(model), model, Model.class);
    }

    public void registerService(Service service) throws MuleException
    {
        registry.registerObject(getName(service), service, Service.class);
    }

    public void unregisterService(String serviceName) throws MuleException
    {
        registry.unregisterObject(serviceName, Service.class);
    }

    public void unregisterAgent(String agentName) throws MuleException
    {
        registry.unregisterObject(agentName, Agent.class);
    }

    public void unregisterConnector(String connectorName) throws MuleException
    {
        registry.unregisterObject(connectorName, Connector.class);
    }

    public void unregisterEndpoint(String endpointName) throws MuleException
    {
        registry.unregisterObject(endpointName, ImmutableEndpoint.class);
    }

    public void unregisterModel(String modelName) throws MuleException
    {
        registry.unregisterObject(modelName, Model.class);
    }

    public void unregisterTransformer(String transformerName) throws MuleException
    {
        Transformer transformer = lookupTransformer(transformerName);
        notifyTransformerResolvers(transformer, TransformerResolver.RegistryAction.REMOVED);
        registry.unregisterObject(transformerName, Transformer.class);

    }

    /**
     * Will execute any processors on an object and fire any lifecycle methods according to the current lifecycle without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.
     *
     * @param object the object to process
     * @return the same object with any processors and lifecycle methods called
     * @throws org.mule.api.MuleException if the registry fails to perform the lifecycle change or process object processors for the object.
     */
    public Object applyProcessorsAndLifecycle(Object object) throws MuleException
    {
        object = applyProcessors(object);
        object = applyLifecycle(object);
        return object;
    }

    public Object applyProcessors(Object object) throws MuleException
    {
        return registry.getTransientRegistry().applyProcessors(object);
    }

    public Object applyLifecycle(Object object) throws MuleException
    {
        return registry.getTransientRegistry().applyLifecycle(object);
    }

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

    public <T> Collection<T> lookupObjects(Class<T> type)
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


    private class TransformerResolverComarator implements Comparator<TransformerResolver>
    {
        public int compare(TransformerResolver transformerResolver, TransformerResolver transformerResolver1)
        {
            if (transformerResolver.getClass().equals(TypeBasedTransformerResolver.class))
            {
                return 1;
            }

            if (transformerResolver1.getClass().equals(TypeBasedTransformerResolver.class))
            {
                return -1;
            }
            return 0;
        }
    }
}


