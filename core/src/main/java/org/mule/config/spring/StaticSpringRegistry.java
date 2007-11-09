/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.MuleException;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.BeanUtils;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * This class is not yet in use.  It is still a work-in-progress.
 */
public class StaticSpringRegistry extends SpringRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.StaticSpring";

    public static final Integer OBJECT_SCOPE_SINGLETON = new Integer(1);
    public static final Integer OBJECT_SCOPE_PROTOTYPE = new Integer(2);
    public static final Integer OBJECT_SCOPE_POOLED = new Integer(3);
    
    protected StaticApplicationContext registryContext;

    public StaticSpringRegistry()
    {
        super(REGISTRY_ID, null);
    }

    public StaticSpringRegistry(ApplicationContext applicationContext)
    {
        this(REGISTRY_ID, applicationContext);
    }

    public StaticSpringRegistry(String id)
    {
        this(id, null);
    }

    public StaticSpringRegistry(String id, ApplicationContext applicationContext)
    {
        super(id);

        if(applicationContext==null)
        {
            registryContext = new StaticApplicationContext();
        }
        else
        {
            registryContext = new StaticApplicationContext(applicationContext);

        }

        registryContext.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor()
                {

            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
                    {
                return bean;
            }

            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
                    {
                if (isInitialised() || isInitialising())
                {
                    if(bean instanceof Initialisable)
                    {
                        try
                        {
                            ((Initialisable)bean).initialise();
                        }
                        catch (InitialisationException e)
                        {
                            throw new BeanCreationException("Failed to initialise Bean: " + e.getMessage(), e);
                        }

                    }
                }
               
                return bean;
            }
        });
        setApplicationContext(registryContext);
    }

    // Does this actually create an instance of the object?
    // The object already exists (it's passed in as a parameter), so this seems to generate a bean definition 
    // ("recipe" for creating the bean) based on the already existing object - a bit counterintuitive.
    protected void registerSingleton(Object o) throws UMOException
    {
        try
        {
            Map m = BeanUtils.describe(o);
            MutablePropertyValues mpvs = new MutablePropertyValues(m);
            registryContext.registerSingleton((String)m.get("name"), o.getClass(), mpvs);
        }
        catch (Exception e)
        {
            throw new MuleException(e);
        }
    }

    protected void registerPrototype(Object o) throws UMOException
    {
        try
        {
            Map m = BeanUtils.describe(o);
            MutablePropertyValues mpvs = new MutablePropertyValues(m);
            registryContext.registerPrototype((String)m.get("name"), o.getClass(), mpvs);
        }
        catch (Exception e)
        {
            throw new MuleException(e);
        }
    }

    protected void doRegisterObject(String key, Object value, Object metadata, UMOManagementContext managementContext) throws RegistrationException
    {
        if (metadata instanceof Integer)
        {
            try 
            {
                if (metadata.equals(OBJECT_SCOPE_SINGLETON))
                {
                    registerSingleton(value);
                }
                else if (metadata.equals(OBJECT_SCOPE_PROTOTYPE))
                {
                    registerPrototype(value);
                }
                else
                {
                    throw new RegistrationException("Object scope not recognized: " + metadata);
                }
            } 
            catch (UMOException e)
            {
                throw new RegistrationException(e);
            }
        }
        else
        {
            throw new RegistrationException("Object scope not recognized");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void registerConnector(UMOConnector connector, UMOManagementContext managementContext) throws UMOException
    {
        registerObject(connector.getName(), connector, OBJECT_SCOPE_SINGLETON, managementContext);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterConnector(String connectorName) throws UMOException
    {
        UMOConnector c = lookupConnector(connectorName);
        if (c != null)
        {
            c.dispose();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOImmutableEndpoint endpoint, UMOManagementContext managementContext) throws UMOException
    {
        registerObject(endpoint.getName(), endpoint, OBJECT_SCOPE_PROTOTYPE, managementContext);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpoint(String endpointName)
    {
        UMOImmutableEndpoint ep = lookupEndpoint(endpointName);
        if (ep != null)
        {
            //TODO Kill it
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext) throws UMOException
    {
        registerObject(transformer.getName(), transformer, OBJECT_SCOPE_PROTOTYPE, managementContext);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterTransformer(String transformerName)
    {
        UMOTransformer t = lookupTransformer(transformerName);
        if(t!=null && t instanceof Disposable) {
            ((Disposable)t).dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerModel(UMOModel model, UMOManagementContext managementContext) throws UMOException
    {
        registerObject(model.getName(), model, OBJECT_SCOPE_SINGLETON, managementContext);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterModel(String name)
    {
        UMOModel model = lookupModel(name);
        if (model != null)
        {
            model.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerAgent(UMOAgent agent, UMOManagementContext managementContext) throws UMOException
    {
        registerObject(agent.getName(), agent, OBJECT_SCOPE_SINGLETON, managementContext);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterAgent(String name) throws UMOException
    {
        UMOAgent agent = (UMOAgent) lookupObject(name);
        if (agent != null)
        {
            //TODO AP Is this the wrong way round?
            agent.dispose();
            agent.unregistered();
        }
    }

    //@java.lang.Override
    public boolean isReadOnly()
    {
        return false;
    }
}
