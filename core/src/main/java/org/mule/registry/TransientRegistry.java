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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.Model;
import org.mule.api.registry.InjectProcessor;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.PreInitProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.functors.InstanceofPredicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Use synchronized(registry) when reading/writing/iterating over the contents of the registry hashmap.
 */
//@ThreadSafe
public class TransientRegistry extends AbstractRegistry
{
    /** logger used by this class */
    protected transient final Log logger = LogFactory.getLog(TransientRegistry.class);
    public static final String REGISTRY_ID = "org.mule.Registry.Transient";

    //@ThreadSafe synchronized(registry)
    private final Map<String, Object> registry = new HashMap<String, Object>();


    public TransientRegistry(MuleContext muleContext)
    {
        this(REGISTRY_ID, muleContext);
    }

    public TransientRegistry(String id, MuleContext muleContext)
    {
        super(id, muleContext);
        synchronized(registry)
        {
            registry.put("_muleContextProcessor", new MuleContextProcessor(muleContext));
            //registry.put("_muleNotificationProcessor", new NotificationListenersProcessor(muleContext));
            registry.put("_muleExpressionEvaluatorProcessor", new ExpressionEvaluatorProcessor(muleContext));
            registry.put("_muleLifecycleStateInjectorProcessor", new LifecycleStateInjectorProcessor(getLifecycleManager().getState()));
            registry.put("_muleLifecycleManager", getLifecycleManager());
        }
    }

    @java.lang.Override
    protected void doInitialise() throws InitialisationException
    {
        applyProcessors(lookupObjects(Connector.class), null);
        applyProcessors(lookupObjects(Transformer.class), null);
        applyProcessors(lookupObjects(ImmutableEndpoint.class), null);
        applyProcessors(lookupObjects(Agent.class), null);
        applyProcessors(lookupObjects(Model.class), null);
        applyProcessors(lookupObjects(Service.class), null);
        applyProcessors(lookupObjects(Object.class), null);

        try
        {
            getLifecycleManager().fireLifecycle(Initialisable.PHASE_NAME);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    protected void doDispose()
    {
        try
        {
            getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);
        }
        catch (MuleException e)
        {
            logger.warn("Failed to dipose the registry cleanly", e);
        }
    }

    protected Map applyProcessors(Map<String, Object> objects)
    {
        if (objects == null)
        {
            return null;
        }
        Map<String, Object> results = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : objects.entrySet())
        {
            //We do this in the loop in case the map contains ObjectProcessors
            Collection<ObjectProcessor> processors = lookupObjects(ObjectProcessor.class);
            for (ObjectProcessor processor : processors)
            {
                Object result = processor.process(entry.getValue());
                //If result is null do not add the object
                if(result != null)
                {
                    results.put(entry.getKey(), result);
                }
            }
        }
        return results;
    }


    public void registerObjects(Map objects) throws RegistrationException
    {
        if (objects == null)
        {
            return;
        }

        for (Iterator iterator = objects.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            registerObject(entry.getKey().toString(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        synchronized(registry)
        {
            final Map<String, T> results = new HashMap<String, T>();
            for (Map.Entry<String, Object> entry : registry.entrySet())
            {
                final Class clazz = entry.getValue().getClass();
                if (type.isAssignableFrom(clazz))
                {
                    results.put(entry.getKey(), (T) entry.getValue());
                }
            }

            return results;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T  lookupObject(String key)
    {
        synchronized(registry)
        {
            return (T) registry.get(key);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> lookupObjects(Class<T> returntype)
    {
        synchronized(registry)
        {
            return CollectionUtils.select(registry.values(), new InstanceofPredicate(returntype));
        }
    }

    /**
     * Will fire any lifecycle methods according to the current lifecycle without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.
     *
     * @param object the object to process
     * @return the same object with lifecycle methods called (if it has any)
     * @throws org.mule.api.MuleException if the registry fails to perform the lifecycle change for the object.
     */
    Object applyLifecycle(Object object) throws MuleException
    {
        getLifecycleManager().applyCompletedPhases(object);
        return object;
    }



    Object applyProcessors(Object object, Object metadata)
    {
        Object theObject = object;

        if(!hasFlag(metadata, MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG))
        {
            //Process injectors first
            Collection<InjectProcessor> injectProcessors = lookupObjects(InjectProcessor.class);
            for (InjectProcessor processor : injectProcessors)
            {
                theObject = processor.process(theObject);
            }
        }

        if(!hasFlag(metadata, MuleRegistry.PRE_INIT_PROCESSORS_BYPASS_FLAG))
        {
            //Then any other processors
            Collection<PreInitProcessor> processors = lookupObjects(PreInitProcessor.class);
            for (PreInitProcessor processor : processors)
            {
                theObject = processor.process(theObject);
                if(theObject==null)
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
    public void registerObject(String key, Object value) throws RegistrationException
    {
        registerObject(key, value, Object.class);
    }

    /**
     * Allows for arbitrary registration of transient objects
     *
     * @param key
     */
    public void registerObject(String key, Object object, Object metadata) throws RegistrationException
    {
        checkDisposed();
        if (StringUtils.isBlank(key))
        {
            throw new RegistrationException("Attempt to register object with no key");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("registering key/object %s/%s", key, object));
        }
        
        logger.debug("applying processors");
        object = applyProcessors(object, metadata);
        //Don't add the object if the processor returns null
        if (object==null)
        {
            return;
        }

        synchronized(registry)
        {
            if (registry.containsKey(key))
            {
                // registry.put(key, value) would overwrite a previous entity with the same name.  Is this really what we want?
                // Not sure whether to throw an exception or log a warning here.
                //throw new RegistrationException("TransientRegistry already contains an object named '" + key + "'.  The previous object would be overwritten.");
                logger.warn("TransientRegistry already contains an object named '" + key + "'.  The previous object will be overwritten.");
            }
            registry.put(key, object);
        }

        try
        {
            if (!hasFlag(metadata, MuleRegistry.LIFECYCLE_BYPASS_FLAG))
            {
                if(logger.isDebugEnabled())
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


    protected void checkDisposed() throws RegistrationException
    {
        if(getLifecycleManager().isPhaseComplete(Disposable.PHASE_NAME))
        {
            throw new RegistrationException("Cannot register objects on the registry as the context is disposed");
        }
    }

    protected boolean hasFlag(Object metaData, int flag)
    {
        return !(metaData == null || !(metaData instanceof Integer)) && ((Integer) metaData & flag) != 0;
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        Object obj;
        synchronized (registry)
        {
            obj = registry.remove(key);
        }

        try
        {
            getLifecycleManager().applyPhase(obj, Disposable.PHASE_NAME);
        }
        catch (MuleException e)
        {
            throw new RegistrationException(e);
        }

    }

    public void unregisterObject(String key) throws RegistrationException
    {
        unregisterObject(key, Object.class);
    }

    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean isRemote()
    {
        return false;
    }

}
