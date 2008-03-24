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
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.functors.InstanceofPredicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransientRegistry extends AbstractRegistry
{
    /** logger used by this class */
    protected transient final Log logger = LogFactory.getLog(TransientRegistry.class);
    public static final String REGISTRY_ID = "org.mule.Registry.Transient";

    private Map registry = new HashMap(8);

    public TransientRegistry()
    {
        super(REGISTRY_ID);
        registry.put("_mulePropertyExtractorProcessor", new ExpressionEvaluatorProcessor());
    }

    //@java.lang.Override
    protected void doInitialise() throws InitialisationException
    {
        //int oldScope = getDefaultScope();
        //setDefaultScope(Registry.SCOPE_IMMEDIATE);
        try
        {
            applyProcessors(lookupObjects(Connector.class));
            applyProcessors(lookupObjects(Transformer.class));
            applyProcessors(lookupObjects(ImmutableEndpoint.class));
            applyProcessors(lookupObjects(Agent.class));
            applyProcessors(lookupObjects(Model.class));
            applyProcessors(lookupObjects(Service.class));
            applyProcessors(lookupObjects(Object.class));
        }
        finally
        {
            //setDefaultScope(oldScope);
        }

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

    //@Override
    protected void doDispose()
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

    protected void applyProcessors(Map objects)
    {
        if (objects == null)
        {
            return;
        }
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            Collection processors = lookupObjects(ObjectProcessor.class);
            for (Iterator iterator2 = processors.iterator(); iterator2.hasNext();)
            {
                ObjectProcessor op = (ObjectProcessor) iterator2.next();
                op.process(o);
            }
        }
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
        return registry.get(key);
    }

    public Collection lookupObjects(Class returntype)
    {
        return CollectionUtils.select(registry.values(), new InstanceofPredicate(returntype));
    }

    protected Object applyProcessors(Object object)
    {
        Object theObject = object;
        // this may be an incorrect hack.  the problem is that if we try to lookup objects in spring before
        // it is initialised, we end up triggering object creation.  that causes circular dependencies because
        // this may have originally been called while creating objects in spring...  so we prevent that by
        // only doing the full lookup once everything is stable.  ac.
        Collection processors = lookupObjects(ObjectProcessor.class);
        for (Iterator iterator = processors.iterator(); iterator.hasNext();)
        {
            ObjectProcessor o = (ObjectProcessor) iterator.next();
            theObject = o.process(theObject);
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
     * @param value
     */
    public void registerObject(String key, Object object, Object metadata) throws RegistrationException
    {
        logger.debug("registering object");
        if (MuleServer.getMuleContext().isInitialised() || MuleServer.getMuleContext().isInitialising())
        {
            logger.debug("applying processors");
            object = applyProcessors(object);
        }

        if (registry.containsKey(key))
        {
            // registry.put(key, value) would overwrite a previous entity with the same name.  Is this really what we want?
            // Not sure whether to throw an exception or log a warning here.
            //throw new RegistrationException("TransientRegistry already contains an object named '" + key + "'.  The previous object would be overwritten.");
            logger.warn("TransientRegistry already contains an object named '" + key + "'.  The previous object will be overwritten.");
        }
        registry.put(key, object);
        try
        {
            MuleContext mc = MuleServer.getMuleContext();
            logger.debug("context: " + mc);
            if (mc != null)
            {
                logger.debug("applying lifecycle");
                mc.getLifecycleManager().applyCompletedPhases(object);
            }
            else
            {
                throw new RegistrationException("Unable to register object (\""
                        + key + ":" + ClassUtils.getSimpleName(object.getClass())
                        + "\") because MuleContext has not yet been created.");
            }
        }
        catch (MuleException e)
        {
            throw new RegistrationException(e);
        }
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        Object obj = registry.remove(key);
        if (obj instanceof Stoppable)
        {
            try
            {
                ((Stoppable) obj).stop();
            }
            catch (MuleException e)
            {
                throw new RegistrationException(e);
            }
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
