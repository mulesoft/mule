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
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ObjectProcessor;
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
    private Map registry = new HashMap();

    private MuleContext context;

    public TransientRegistry(MuleContext context)
    {
        super(REGISTRY_ID);
        this.context = context;
        synchronized(registry)
        {
            registry.put("_muleContextProcessor", new MuleContextProcessor(context));
            registry.put("_mulePropertyExtractorProcessor", new ExpressionEvaluatorProcessor(context));
        }
    }

    @java.lang.Override
    protected void doInitialise() throws InitialisationException
    {
        applyProcessors(lookupObjects(Connector.class));
        applyProcessors(lookupObjects(Transformer.class));
        applyProcessors(lookupObjects(ImmutableEndpoint.class));
        applyProcessors(lookupObjects(Agent.class));
        applyProcessors(lookupObjects(Model.class));
        applyProcessors(lookupObjects(Service.class));
        applyProcessors(lookupObjects(Object.class));

        synchronized(registry)
        {
            Collection allObjects = lookupObjects(Object.class);
            Object obj;
            for (Iterator iterator = allObjects.iterator(); iterator.hasNext();)
            {
                obj = iterator.next();
                if (obj instanceof Initialisable)
                {
                    ((Initialisable) obj).initialise();
                }        
            }
        }
    }

    @Override
    protected void doDispose()
    {
        synchronized(registry)
        {
            Collection allObjects = lookupObjects(Object.class);
            Object obj;
            for (Iterator iterator = allObjects.iterator(); iterator.hasNext();)
            {
                obj = iterator.next();
                if (obj instanceof Disposable)
                {
                    ((Disposable) obj).dispose();
                }
            }
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

    public Object lookupObject(String key)
    {
        synchronized(registry)
        {
            return registry.get(key);
        }
    }

    public <T>Collection lookupObjects(Class<T> returntype)
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
        context.getLifecycleManager().applyCompletedPhases(object);
        return object;
    }



    Object applyProcessors(Object object)
    {
        Object theObject = object;
        // this may be an incorrect hack.  the problem is that if we try to lookup objects in spring before
        // it is initialised, we end up triggering object creation.  that causes circular dependencies because
        // this may have originally been called while creating objects in spring...  so we prevent that by
        // only doing the full lookup once everything is stable.  ac.
        Collection<ObjectProcessor> processors = lookupObjects(ObjectProcessor.class);
        for (ObjectProcessor processor : processors)
        {
            theObject = processor.process(theObject);
            if(theObject==null)
            {
                return null;
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
     * Allows for arbitary registration of transient objects
     *
     * @param key
     */
    public void registerObject(String key, Object object, Object metadata) throws RegistrationException
    {
        if (StringUtils.isBlank(key))
        {
            throw new RegistrationException("Attempt to register object with no key");
        }
        
        logger.debug("registering object");
        if (!MuleRegistry.LIFECYCLE_BYPASS_FLAG.equals(metadata))
        {
            if (context.isInitialised() || context.isInitialising())
            {
                logger.debug("applying processors");
                object = applyProcessors(object);
                //Don't add the object if the processor returns null
                if(object==null)
                {
                    return;
                }
            }
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
            if (!MuleRegistry.LIFECYCLE_BYPASS_FLAG.equals(metadata))
            {
                if(logger.isDebugEnabled())
                {
                    logger.debug("applying lifecycle to object: " + object);
                }
                context.getLifecycleManager().applyCompletedPhases(object);
            }
        }
        catch (MuleException e)
        {
            throw new RegistrationException(e);
        }
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        Object obj;
        synchronized(registry)
        {
            obj = registry.remove(key);
        }

            try
            {
                context.getLifecycleManager().applyPhases(obj, Disposable.PHASE_NAME);

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
