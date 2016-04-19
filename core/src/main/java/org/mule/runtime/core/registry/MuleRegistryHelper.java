/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.registry;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NameableObject;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.registry.AbstractServiceDescriptor;
import org.mule.runtime.core.api.registry.LifecycleRegistry;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.registry.RegistryProvider;
import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.api.registry.ServiceDescriptor;
import org.mule.runtime.core.api.registry.ServiceDescriptorFactory;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.api.registry.TransformerResolver;
import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.Predicate;
import org.mule.runtime.core.util.SpiUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.UUID;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Adds lookup/register/unregister methods for Mule-specific entities to the standard
 * Registry interface.
 */
public class MuleRegistryHelper implements MuleRegistry, RegistryProvider
{
    protected transient Log logger = LogFactory.getLog(MuleRegistryHelper.class);

    /**
     * A reference to Mule's internal registry
     */
    private DefaultRegistryBroker registry;

    /**
     * We cache transformer searches so that we only search once
     */
    protected ConcurrentHashMap/*<String, Transformer>*/ exactTransformerCache = new ConcurrentHashMap/*<String, Transformer>*/(8);
    protected ConcurrentHashMap/*Map<String, List<Transformer>>*/ transformerListCache = new ConcurrentHashMap/*<String, List<Transformer>>*/(8);

    private MuleContext muleContext;

    private final ReadWriteLock transformerResolversLock = new ReentrantReadWriteLock();

    /**
     * Transformer transformerResolvers are registered on context start, then they are not unregistered.
     */
    private List<TransformerResolver> transformerResolvers = new ArrayList<TransformerResolver>();

    private final ReadWriteLock transformersLock = new ReentrantReadWriteLock();

    /**
     * Transformers are registered on context start, then they are usually not unregistered
     */
    private Collection<Transformer> transformers = new CopyOnWriteArrayList<Transformer>();

    public MuleRegistryHelper(DefaultRegistryBroker registry, MuleContext muleContext)
    {
        this.registry = registry;
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() throws InitialisationException
    {
        //no-op

        //This is called when the MuleContext starts up, and should only do initialisation for any state on this class, the lifecycle
        //for the registries will be handled by the LifecycleManager on the registry that this class wraps
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
    {
        transformerListCache.clear();
        exactTransformerCache.clear();
    }

    @Override
    public void fireLifecycle(String phase) throws LifecycleException
    {
        if (Initialisable.PHASE_NAME.equals(phase))
        {
            registry.initialise();
        }
        else if (Disposable.PHASE_NAME.equals(phase))
        {
            registry.dispose();
        }
        else
        {
            registry.fireLifecycle(phase);
        }
    }

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    @Override
    public Connector lookupConnector(String name)
    {
        return (Connector) registry.lookupObject(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    @Override
    public EndpointBuilder lookupEndpointBuilder(String name)
    {
        Object o = registry.lookupObject(name);
        if (o instanceof EndpointBuilder)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Global endpoint EndpointBuilder for name: " + name + " found");
            }
            return (EndpointBuilder) o;
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No endpoint builder with the name: " + name + " found.");
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transformer lookupTransformer(String name)
    {
        return (Transformer) registry.lookupObject(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transformer lookupTransformer(DataType source, DataType result) throws TransformerException
    {
        final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);
        Transformer cachedTransformer = (Transformer) exactTransformerCache.get(dataTypePairHash);
        if (cachedTransformer != null)
        {
            return cachedTransformer;
        }

        Transformer trans = resolveTransformer(source, result);

        if (trans != null)
        {
            Transformer concurrentlyAddedTransformer = (Transformer) exactTransformerCache.putIfAbsent(
                    dataTypePairHash, trans);
            if (concurrentlyAddedTransformer != null)
            {
                return concurrentlyAddedTransformer;
            }
            else
            {
                return trans;
            }
        }
        else
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, result));
        }
    }

    protected Transformer resolveTransformer(DataType source, DataType result) throws TransformerException
    {
        Lock readLock = transformerResolversLock.readLock();
        readLock.lock();

        try
        {
            for (TransformerResolver resolver : transformerResolvers)
            {
                try
                {
                    Transformer trans = resolver.resolve(source, result);
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
        }
        finally
        {
            readLock.unlock();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Transformer> lookupTransformers(DataType source, DataType result)
    {
        final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);

        List<Transformer> results = (List<Transformer>) transformerListCache.get(dataTypePairHash);
        if (results != null)
        {
            return results;
        }

        results = new ArrayList<Transformer>(2);

        Lock readLock = transformersLock.readLock();
        readLock.lock();
        try
        {
            for (Transformer transformer : transformers)
            {
                // The transformer must have the DiscoveryTransformer interface if we are
                // going to find it here
                if (!(transformer instanceof Converter))
                {
                    continue;
                }
                DataType returnDataType = transformer.getReturnDataType();
                if (result.isCompatibleWith(returnDataType) && transformer.isSourceDataTypeSupported(source))
                {
                    results.add(transformer);
                }
            }
        }
        finally
        {
            readLock.unlock();
        }

        List<Transformer> concurrentlyAddedTransformers = (List<Transformer>) transformerListCache.putIfAbsent(
                dataTypePairHash, results);
        if (concurrentlyAddedTransformers != null)
        {
            return concurrentlyAddedTransformers;
        }
        else
        {
            return results;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Agent lookupAgent(String name)
    {
        return (Agent) registry.lookupObject(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowConstruct lookupFlowConstruct(String name)
    {
        return (FlowConstruct) registry.lookupObject(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FlowConstruct> lookupFlowConstructs()
    {
        return lookupObjects(FlowConstruct.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void registerTransformer(Transformer transformer) throws MuleException
    {
        registerObject(getName(transformer), transformer, Transformer.class);
    }

    public void notifyTransformerResolvers(Transformer t, TransformerResolver.RegistryAction action)
    {
        if (t instanceof Converter)
        {
            Lock transformerResolversReadLock = transformerResolversLock.readLock();
            transformerResolversReadLock.lock();
            try
            {

                for (TransformerResolver resolver : transformerResolvers)
                {
                    resolver.transformerChange(t, action);
                }
            }
            finally
            {
                transformerResolversReadLock.unlock();
            }

            transformerListCache.clear();
            exactTransformerCache.clear();

            Lock transformersWriteLock = transformersLock.writeLock();
            transformersWriteLock.lock();
            try
            {
                if (action == TransformerResolver.RegistryAction.ADDED)
                {
                    transformers.add(t);
                }
                else
                {
                    transformers.remove(t);
                }
            }
            finally
            {
                transformersWriteLock.unlock();
            }
        }
    }

    /**
     * Looks up the service descriptor from a singleton cache and creates a new one if not found.
     * 
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    @Override
    public ServiceDescriptor lookupServiceDescriptor(ServiceType type, String name, Properties overrides) throws ServiceException
    {
        String key = new AbstractServiceDescriptor.Key(name, overrides).getKey();
        // TODO If we want these descriptors loaded form Spring we need to change the key mechanism
        // and the scope, and then deal with circular reference issues.

        synchronized (this)
        {
            ServiceDescriptor sd = registry.lookupObject(key);
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
            return sd;
        }
    }

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    protected ServiceDescriptor createServiceDescriptor(ServiceType type, String name, Properties overrides) throws ServiceException
    {
        // Stripe off and use the meta-scheme if present
        String scheme = name;
        if (name.contains(":"))
        {
            scheme = name.substring(0, name.indexOf(":"));
        }

        Properties props = SpiUtils.findServiceDescriptor(type, scheme);
        if (props == null)
        {
            throw new ServiceException(CoreMessages.failedToLoad(type + " " + scheme));
        }

        return ServiceDescriptorFactory.create(type, name, props, overrides, muleContext,
                muleContext.getExecutionClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerAgent(Agent agent) throws MuleException
    {
        registry.registerObject(getName(agent), agent, Agent.class);
    }

    @Override
    public void registerConnector(Connector connector) throws MuleException
    {
        registry.registerObject(getName(connector), connector, Connector.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerEndpoint(ImmutableEndpoint endpoint) throws MuleException
    {
        registry.registerObject(getName(endpoint), endpoint, ImmutableEndpoint.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerEndpointBuilder(String name, EndpointBuilder builder) throws MuleException
    {
        registry.registerObject(name, builder, EndpointBuilder.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerFlowConstruct(FlowConstruct flowConstruct) throws MuleException
    {
        registry.registerObject(getName(flowConstruct), flowConstruct, FlowConstruct.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAgent(String agentName) throws MuleException
    {
        registry.unregisterObject(agentName, Agent.class);
    }

    @Override
    public void registerScheduler(Scheduler scheduler) throws MuleException
    {
        registry.registerObject(scheduler.getName(), scheduler);
    }

    @Override
    public void unregisterScheduler(Scheduler scheduler) throws MuleException
    {
        registry.unregisterObject(scheduler.getName(), scheduler);
    }

    @Override
    public Collection<Scheduler> lookupScheduler(Predicate<String> schedulerNamePredicate)
    {
        Collection<Scheduler> schedulers = new ArrayList<Scheduler>();
        Map<String, Scheduler> registeredSchedulers = lookupByType(Scheduler.class);
        for (Scheduler registeredScheduler : registeredSchedulers.values())
        {
            if (schedulerNamePredicate.evaluate(registeredScheduler.getName()))
            {
                schedulers.add(registeredScheduler);
            }
        }

        return schedulers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterTransformer(String transformerName) throws MuleException
    {
        Transformer transformer = lookupTransformer(transformerName);
        notifyTransformerResolvers(transformer, TransformerResolver.RegistryAction.REMOVED);
        registry.unregisterObject(transformerName, Transformer.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object applyProcessorsAndLifecycle(Object object) throws MuleException
    {
        object = applyProcessors(object);
        object = applyLifecycle(object);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object applyProcessors(Object object) throws MuleException
    {
        return muleContext.getInjector().inject(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object applyLifecycle(Object object) throws MuleException
    {
        return applyLifecycle(object, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object applyLifecycle(Object object, String phase) throws MuleException
    {
        LifecycleRegistry lifecycleRegistry = registry.getLifecycleRegistry();
        if (lifecycleRegistry != null)
        {
            return lifecycleRegistry.applyLifecycle(object, phase);
        }

        return object;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Delegate to internal registry
    ////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        return registry.lookupObject(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T lookupObject(String key)
    {
        return (T) registry.lookupObject(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T lookupObject(String key, boolean applyLifecycle)
    {
        return (T) registry.lookupObject(key, applyLifecycle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> lookupObjects(Class<T> type)
    {
        return registry.lookupObjects(type);
    }

    @Override
    public <T> Collection<T> lookupLocalObjects(Class<T> type)
    {
        return registry.lookupLocalObjects(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type)
    {
        return registry.lookupObjectsForLifecycle(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key)
    {
        return (T) registry.get(key);
    }

    @Override
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        return registry.lookupByType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        registry.registerObject(key, value, metadata);

        postObjectRegistrationActions(value);
    }

    public void postObjectRegistrationActions(Object value)
    {
        if (value instanceof TransformerResolver)
        {
            registerTransformerResolver((TransformerResolver) value);
        }

        if (value instanceof Converter)
        {
            notifyTransformerResolvers((Converter) value,  TransformerResolver.RegistryAction.ADDED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObject(String key, Object value) throws RegistrationException
    {
        registry.registerObject(key, value);

        postObjectRegistrationActions(value);
    }

    public void registerTransformerResolver(TransformerResolver value)
    {
        Lock lock = transformerResolversLock.writeLock();
        lock.lock();
        try
        {
            transformerResolvers.add(value);
            Collections.sort(transformerResolvers, new TransformerResolverComparator());
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObjects(Map objects) throws RegistrationException
    {
        registry.registerObjects(objects);

        for (Object value : objects.values())
        {
            postObjectRegistrationActions(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unregisterObject(String key, Object metadata) throws RegistrationException
    {
        return registry.unregisterObject(key, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unregisterObject(String key) throws RegistrationException
    {
        return registry.unregisterObject(key);
    }

    @Override
    public Collection<Registry> getRegistries()
    {
        return ImmutableList.copyOf(registry.getRegistries());
    }

    /**
     * Returns the name for the object passed in.  If the object implements {@link org.mule.runtime.core.api.NameableObject}, then
     * {@link org.mule.runtime.core.api.NameableObject#getName()} will be returned, otherwise a name is generated using the class name
     * and a generated UUID.
     *
     * @param obj the object to inspect
     * @return the name for this object
     */
    protected String getName(Object obj)
    {
        String name = null;
        if (obj instanceof NameableObject)
        {
            name = ((NameableObject) obj).getName();
        }
        else if (obj instanceof FlowConstruct)
        {
            name = ((FlowConstruct) obj).getName();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRegistryId()
    {
        return this.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote()
    {
        return false;
    }

    private String getDataTypeSourceResultPairHash(DataType<?> source, DataType<?> result)
    {
        return source.getClass().getName() + source.hashCode() + ":" + result.getClass().getName()
               + result.hashCode();
    }

    private class TransformerResolverComparator implements Comparator<TransformerResolver>
    {

        @Override
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


