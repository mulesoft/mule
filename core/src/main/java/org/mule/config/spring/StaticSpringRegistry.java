/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.MuleException;
import org.mule.umo.UMOException;
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
 * TODO
 */
public class StaticSpringRegistry extends SpringRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.StaticSpring";

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

    /**
     * {@inheritDoc}
     */
    public UMOConnector unregisterConnector(String connectorName) throws UMOException
    {
        UMOConnector c = lookupConnector(connectorName);
        if (c != null)
        {
            c.dispose();
        }
        return c;
    }


    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOImmutableEndpoint endpoint) throws UMOException
    {
        registerPrototype(endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public UMOImmutableEndpoint unregisterEndpoint(String endpointName)
    {
        UMOImmutableEndpoint ep = lookupEndpoint(endpointName);
        if (ep != null)
        {
            //TODO Kill it
        }
        return ep;
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        registerPrototype(transformer);
    }

    /**
     * {@inheritDoc}
     */
    public UMOTransformer unregisterTransformer(String transformerName)
    {
        UMOTransformer t = lookupTransformer(transformerName);
        if(t!=null && t instanceof Disposable) {
            ((Disposable)t).dispose();
        }
        return t;
    }



    public void registerModel(UMOModel model) throws UMOException
    {
        registerSingleton(model);
    }

    public UMOModel unregisterModel(String name)
    {
        UMOModel model = lookupModel(name);
        if (model != null)
        {
            model.dispose();
        }
        return model;
    }

    /**
     * {@inheritDoc}
     */
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        registerSingleton(agent);
    }



    /**
     * {@inheritDoc}
     */
    public UMOAgent unregisterAgent(String name) throws UMOException
    {

        UMOAgent agent = (UMOAgent) lookupObject(name, UMOAgent.class);
        if (agent != null)
        {
            //TODO AP Is this the wrong way round?
            agent.dispose();
            agent.unregistered();
        }
        return agent;
    }

    //@java.lang.Override
    public boolean isReadOnly()
    {
        return false;
    }
}
