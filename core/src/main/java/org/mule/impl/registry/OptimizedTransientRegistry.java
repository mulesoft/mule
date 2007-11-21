/*
 * $Id: TransientRegistry.java 9676 2007-11-09 17:08:30Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.registry;

import org.mule.RegistryContext;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Same as TransientRegistry, but tries to optimize performance by segmenting the HashMap by type.
 */
public class OptimizedTransientRegistry extends TransientRegistry
{
    /** Map of Maps registry */
    //@Override
    private Map registry;

    //@Override
    public static Registry getInstance()
    {
        return new OptimizedTransientRegistry();
    }
    
    private void init()
    {
        registry = new HashMap(8);

        //Register ManagementContext Injector for locally registered objects
        //TODO this has to be registered once the managementContext is created
//        getObjectTypeMap(ObjectProcessor.class).put(MuleProperties.OBJECT_MANAGMENT_CONTEXT_PROCESSOR,
//                new ManagementContextDependencyProcessor(context));

        getObjectTypeMap(ObjectProcessor.class).put("_mulePropertyExtractorProcessor",
                new PropertyExtractorProcessor());

        RegistryContext.setRegistry(this);
        try
        {
            initialise();
        }
        catch (InitialisationException e)
        {
            e.printStackTrace();
        }

    }

    protected Object doLookupObject(String key)
    {
        Object o = null;
        if (key != null)
        {
            Map map;
            for (Iterator it = registry.values().iterator(); it.hasNext();)
            {
                map = (Map) it.next();
                o = map.get(key);
                if (o != null)
                {
                    return o;
                }
            }
        }
        return o;
    }

    public Collection doLookupObjects(Class returntype)
    {
        Map map = (Map) registry.get(returntype);
        if (map != null)
        {
            return map.values();
        }
        else
        {
            return null;
        }
    }

    protected Map getObjectTypeMap(Object o)
    {
        if (o == null)
        {
            o = Object.class;
        }

        Object key;
        if (o instanceof Class)
        {
            key = (Class) o;
        }
        else if (o instanceof String)
        {
            key = o;
        }
        else
        {
            key = o.getClass();
        }
        Map objects = (Map) registry.get(key);
        if (objects == null)
        {
            objects = new HashMap(8);
            registry.put(key, objects);
        }
        return objects;
    }

    /**
     * Allows for arbitary registration of transient objects
     *
     * @param key
     * @param value
     */
    protected void doRegisterObject(String key, Object value) throws RegistrationException
    {
        doRegisterObject(key, value, Object.class);
    }

    /**
     * Allows for arbitary registration of transient objects
     * 
     * @param key
     * @param value
     */
    protected void doRegisterObject(String key, Object value, Object metadata) throws RegistrationException
    {
        if (isInitialised() || isInitialising())
        {
            value = applyProcessors(value);
        }

        Map objectMap = getObjectTypeMap(metadata);
        if (objectMap != null)
        {
            if (objectMap.containsKey(key))
            {
                // objectMap.put(key, value) would overwrite a previous entity with the same name.  Is this really what we want?
                // Not sure whether to throw an exception or log a warning here.
                //throw new RegistrationException("TransientRegistry already contains an object named '" + key + "'.  The previous object would be overwritten.");
                logger.warn("TransientRegistry already contains an object named '" + key + "'.  The previous object will be overwritten.");
            }
            objectMap.put(key, value);

            try
            {
                getLifecycleManager().applyLifecycle(value);
            }
            catch (UMOException e)
            {
                throw new RegistrationException(e);
            }
        }
        else
        {
            throw new RegistrationException("No object map exists for type " + metadata);
        }
    }

    //@java.lang.Override
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        registerObject(agent.getName(), agent, UMOAgent.class);
    }

    //@java.lang.Override
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        registerObject(connector.getName(), connector, UMOConnector.class);
    }

    //@java.lang.Override
    public void registerEndpoint(UMOImmutableEndpoint endpoint) throws UMOException
    {
        registerObject(endpoint.getName(), endpoint, UMOImmutableEndpoint.class);
    }

    public void registerEndpointBuilder(String name, UMOEndpointBuilder builder) throws UMOException
    {
        registerObject(name, builder, UMOEndpointBuilder.class);
    }

    //@java.lang.Override
    public void registerModel(UMOModel model) throws UMOException
    {
        registerObject(model.getName(), model, UMOModel.class);
    }

    //@java.lang.Override
    protected void doRegisterTransformer(UMOTransformer transformer) throws UMOException
    {
        registerObject(transformer.getName(), transformer, UMOTransformer.class);
    }

    //@java.lang.Override
    public void registerComponent(UMOComponent component) throws UMOException
    {
        registerObject(component.getName(), component, UMOComponent.class);
    }

    protected void unregisterObject(String key, Object metadata) throws UMOException
    {
        Object obj = getObjectTypeMap(metadata).remove(key);
        if (obj instanceof Stoppable)
        {
            ((Stoppable) obj).stop();
        }
    }

    public void unregisterObject(String key) throws UMOException
    {
        unregisterObject(key, Object.class);
    }

    //@java.lang.Override
    public void unregisterComponent(String componentName) throws UMOException
    {
        unregisterObject(componentName, UMOComponent.class);
    }


    //@java.lang.Override
    public void unregisterAgent(String agentName) throws UMOException
    {
        unregisterObject(agentName, UMOAgent.class);
    }

    //@java.lang.Override
    public void unregisterConnector(String connectorName) throws UMOException
    {
        unregisterObject(connectorName, UMOConnector.class);
    }

    //@java.lang.Override
    public void unregisterEndpoint(String endpointName) throws UMOException
    {
        unregisterObject(endpointName, UMOImmutableEndpoint.class);
    }

    //@java.lang.Override
    public void unregisterModel(String modelName) throws UMOException
    {
        unregisterObject(modelName, UMOModel.class);
    }

    //@java.lang.Override
    public void unregisterTransformer(String transformerName) throws UMOException
    {
        unregisterObject(transformerName, UMOTransformer.class);
    }
}
