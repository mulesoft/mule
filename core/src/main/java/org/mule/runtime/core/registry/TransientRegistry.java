/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.InjectProcessor;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.ObjectProcessor;
import org.mule.runtime.core.api.registry.PreInitProcessor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.InstanceofPredicate;
import org.slf4j.Logger;

/**
 * Use the registryLock when reading/writing/iterating over the contents of the registry hashmap.
 * @deprecated as of 3.7.0. Use {@link SimpleRegistry instead}.
 */
@Deprecated
public class TransientRegistry extends AbstractRegistry
{

    public static final String REGISTRY_ID = "org.mule.runtime.core.Registry.Transient";

    private final RegistryMap registryMap = new RegistryMap(logger);

    public TransientRegistry(MuleContext muleContext)
    {
        this(REGISTRY_ID, muleContext);
    }

    public TransientRegistry(String id, MuleContext muleContext)
    {
        super(id, muleContext);
        putDefaultEntriesIntoRegistry();
    }

    private void putDefaultEntriesIntoRegistry()
    {
        Map<String, Object> processors = new HashMap<>();
        processors.put("_muleContextProcessor", new MuleContextProcessor(muleContext));
        //processors("_muleNotificationProcessor", new NotificationListenersProcessor(muleContext));
        processors.put("_muleLifecycleStateInjectorProcessor", new LifecycleStateInjectorProcessor(getLifecycleManager().getState()));
        processors.put("_muleLifecycleManager", getLifecycleManager());
        registryMap.putAll(processors);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        applyProcessors(lookupObjects(Connector.class), null);
        applyProcessors(lookupObjects(Transformer.class), null);
        applyProcessors(lookupObjects(ImmutableEndpoint.class), null);
        applyProcessors(lookupObjects(Agent.class), null);
        applyProcessors(lookupObjects(Object.class), null);
    }

    @Override
    protected void doDispose()
    {
        disposeLostObjects();
        registryMap.clear();
    }

    private void disposeLostObjects()
    {
        for (Object obj : registryMap.getLostObjects())
        {
            try
            {
                ((Disposable) obj).dispose();
            }
            catch (Exception e)
            {
                logger.warn("Can not dispose object. " + ExceptionUtils.getMessage(e));
                if (logger.isDebugEnabled())
                {
                    logger.debug("Can not dispose object. " + ExceptionUtils.getFullStackTrace(e));
                }
            }
        }
    }

    protected Map<String, Object> applyProcessors(Map<String, Object> objects)
    {
        if (objects == null || !isInitialised())
        {
            return null;
        }

        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, Object> entry : objects.entrySet())
        {
            // We do this inside the loop in case the map contains ObjectProcessors
            Collection<ObjectProcessor> processors = lookupObjects(ObjectProcessor.class);
            for (ObjectProcessor processor : processors)
            {
                Object result = processor.process(entry.getValue());
                if (result != null)
                {
                    results.put(entry.getKey(), result);
                }
            }
        }
        return results;
    }


    @Override
    public void registerObjects(Map<String, Object> objects) throws RegistrationException
    {
        if (objects == null)
        {
            return;
        }

        for (Map.Entry<String, Object> entry : objects.entrySet())
        {
            registerObject(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        final Map<String, T> results = new HashMap<String, T>();
        try
        {
            registryMap.lockForReading();

            for (Map.Entry<String, Object> entry : registryMap.entrySet())
            {
                final Class<?> clazz = entry.getValue().getClass();
                if (type.isAssignableFrom(clazz))
                {
                    results.put(entry.getKey(), (T) entry.getValue());
                }
            }
        }
        finally
        {
            registryMap.unlockForReading();
        }

        return results;
    }

    @Override
    public <T> T lookupObject(String key)
    {
        return doGet(key);
    }

    @Override
    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        return super.lookupObject(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> lookupObjects(Class<T> returntype)
    {
        return (Collection<T>) registryMap.select(new InstanceofPredicate(returntype));
    }

    @Override
    public <T> Collection<T> lookupLocalObjects(Class<T> type)
    {
        //just delegate to lookupObjects since there's no parent ever
        return lookupObjects(type);
    }

    /**
     * Will fire any lifecycle methods according to the current lifecycle without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.
     *
     * @param object the object to process
     * @return the same object with lifecycle methods called (if it has any)
     * @throws org.mule.runtime.core.api.MuleException if the registry fails to perform the lifecycle change for the object.
     */
    Object applyLifecycle(Object object) throws MuleException
    {
        getLifecycleManager().applyCompletedPhases(object);
        return object;
    }

    Object applyLifecycle(Object object, String phase) throws MuleException
    {
        getLifecycleManager().applyPhase(object, NotInLifecyclePhase.PHASE_NAME, phase);
        return object;
    }

    protected Object applyProcessors(Object object, Object metadata)
    {
        if (!isInitialised())
        {
            return object;
        }

        Object theObject = object;

        if (!hasFlag(metadata, MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG))
        {
            //Process injectors first
            Collection<InjectProcessor> injectProcessors = lookupObjects(InjectProcessor.class);
            for (InjectProcessor processor : injectProcessors)
            {
                theObject = processor.process(theObject);
            }
        }

        if (!hasFlag(metadata, MuleRegistry.PRE_INIT_PROCESSORS_BYPASS_FLAG))
        {
            //Then any other processors
            Collection<PreInitProcessor> processors = lookupObjects(PreInitProcessor.class);
            for (PreInitProcessor processor : processors)
            {
                theObject = processor.process(theObject);
                if (theObject == null)
                {
                    return null;
                }
            }
        }
        return theObject;
    }

    /**
     * Allows for arbitary registration of transient objects
     *
     * @param key
     * @param value
     */
    @Override
    public void registerObject(String key, Object value) throws RegistrationException
    {
        registerObject(key, value, Object.class);
    }

    /**
     * Allows for arbitrary registration of transient objects
     */
    @Override
    public void registerObject(String key, Object object, Object metadata) throws RegistrationException
    {
        checkDisposed();
        if (StringUtils.isBlank(key))
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("Attempt to register object with no key"));
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("registering key/object %s/%s", key, object));
        }

        logger.debug("applying processors");
        object = applyProcessors(object, metadata);
        if (object == null)
        {
            return;
        }

        doRegisterObject(key, object, metadata);
    }

    protected <T> T doGet(String key)
    {
        return registryMap.get(key);
    }

    protected void doRegisterObject(String key, Object object, Object metadata) throws RegistrationException
    {
        doPut(key, object);

        try
        {
            if (!hasFlag(metadata, MuleRegistry.LIFECYCLE_BYPASS_FLAG))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("applying lifecycle to object: " + object);
                }
                getLifecycleManager().applyCompletedPhases(object);
            }
        }
        catch (MuleException e)
        {
            throw new RegistrationException(e);
        }
    }

    protected void doPut(String key, Object object)
    {
        registryMap.putAndLogWarningIfDuplicate(key, object);
    }

    protected void checkDisposed() throws RegistrationException
    {
        if (getLifecycleManager().isPhaseComplete(Disposable.PHASE_NAME))
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("Cannot register objects on the registry as the context is disposed"));
        }
    }

    protected boolean hasFlag(Object metaData, int flag)
    {
        return !(metaData == null || !(metaData instanceof Integer)) && ((Integer) metaData & flag) != 0;
    }

    @Override
    protected Object doUnregisterObject(String key) throws RegistrationException
    {
        return registryMap.remove(key);
    }

    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    public boolean isRemote()
    {
        return false;
    }

    /**
     * This class encapsulates the {@link HashMap} that's used for storing the objects in the
     * transient registry and also shields client code from having to deal with locking the
     * {@link ReadWriteLock} for the exposed Map operations.
     */
    protected static class RegistryMap
    {

        private final Map<String, Object> registry = new HashMap<String, Object>();
        private final ReadWriteLock registryLock = new ReentrantReadWriteLock();
        private final Set<Object> lostObjects = new TreeSet<Object>(new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                return o1 == o2 ? 0 : nvl(o1) - nvl(o2);
            }

            private int nvl(Object o)
            {
                return o != null ? o.hashCode() : 0;
            }
        });

        private Logger logger;

        public RegistryMap(Logger log)
        {
            super();
            logger = log;
        }

        public Collection<?> select(Predicate predicate)
        {
            Lock readLock = registryLock.readLock();
            try
            {
                readLock.lock();
                return CollectionUtils.select(registry.values(), predicate);
            }
            finally
            {
                readLock.unlock();
            }
        }

        public void clear()
        {
            Lock writeLock = registryLock.writeLock();
            try
            {
                writeLock.lock();
                registry.clear();
                lostObjects.clear();
            }
            finally
            {
                writeLock.unlock();
            }
        }

        public void putAndLogWarningIfDuplicate(String key, Object object)
        {
            Lock writeLock = registryLock.writeLock();
            try
            {
                writeLock.lock();

                final Object previousObject = registry.put(key, object);
                if (previousObject != null && previousObject != object)
                {
                    if (previousObject instanceof Disposable)
                    {
                        lostObjects.add(previousObject);
                    }
                    // registry.put(key, value) would overwrite a previous entity with the same name.  Is this really what we want?
                    // Not sure whether to throw an exception or log a warning here.
                    //throw new RegistrationException("TransientRegistry already contains an object named '" + key + "'.  The previous object would be overwritten.");
                    logger.warn("TransientRegistry already contains an object named '" + key + "'.  The previous object will be overwritten.");
                }
            }
            finally
            {
                writeLock.unlock();
            }
        }

        public void putAll(Map<String, Object> map)
        {
            Lock writeLock = registryLock.writeLock();
            try
            {
                writeLock.lock();
                registry.putAll(map);
            }
            finally
            {
                writeLock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key)
        {
            Lock readLock = registryLock.readLock();
            try
            {
                readLock.lock();
                return (T) registry.get(key);
            }
            finally
            {
                readLock.unlock();
            }
        }

        public Object remove(String key)
        {
            Lock writeLock = registryLock.writeLock();
            try
            {
                writeLock.lock();
                return registry.remove(key);
            }
            finally
            {
                writeLock.unlock();
            }
        }

        public Set<Entry<String, Object>> entrySet()
        {
            return registry.entrySet();
        }

        public Set<Object> getLostObjects()
        {
            return lostObjects;
        }

        public void lockForReading()
        {
            registryLock.readLock().lock();
        }

        public void unlockForReading()
        {
            registryLock.readLock().unlock();
        }
    }
}
