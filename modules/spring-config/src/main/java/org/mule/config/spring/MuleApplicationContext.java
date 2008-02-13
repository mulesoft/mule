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

import org.mule.api.MuleContext;
import org.mule.api.registry.Registry;
import org.mule.config.ConfigResource;
import org.mule.util.ClassUtils;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <code>MuleApplicationContext</code> is a simple extension application context
 * that allows resources to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 *
 */
public class MuleApplicationContext extends AbstractXmlApplicationContext
{
    public static final String LEGACY_BEAN_READER_CLASS = "org.mule.config.spring.MuleBeanDefinitionReader";

    private MuleContext muleContext;
    private Resource[] springResources;

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     * 
     * @param registry
     * @param configResources
     * @see org.mule.config.spring.SpringRegistry
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, ConfigResource[] configResources)
    {
        this(muleContext, registry, configResources, true);
    }
    
    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     * 
     * @param registry
     * @param configLocations
     * @param parent 
     * @see org.mule.config.spring.SpringRegistry
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, ConfigResource[] configResources, ApplicationContext parent)
    {
        super(parent);
        setupParentSpringRegistry(registry);
        this.muleContext = muleContext;
        this.springResources = convert(configResources);
        refresh();
    }

    
    /**
     * @param registry
     * @param configLocations
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, Resource[] configResources)
    {
        this(muleContext, registry, configResources, true);
    }

    /**
     * @param registry
     * @param configResources
     * @param refresh
     * @throws BeansException
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, ConfigResource[] configResources, boolean refresh)
            throws BeansException
    {
        this.muleContext = muleContext;
        setupParentSpringRegistry(registry);
        this.springResources = convert(configResources);
        if (refresh)
        {
            refresh();
        }
    }

    /**
     * @param registry
     * @param configLocations
     * @param parent 
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, Resource[] springResources, ApplicationContext parent) throws IOException
    {
        super(parent);
        this.muleContext = muleContext;
        setupParentSpringRegistry(registry);
        this.springResources = springResources;
        refresh();
    }

    /**
     * @param registry
     * @param configLocations
     * @param refresh
     * @throws BeansException 
     */
    public MuleApplicationContext(MuleContext muleContext, Registry registry, Resource[] springResources, boolean refresh)
            throws BeansException
    {
        setupParentSpringRegistry(registry);
        this.muleContext = muleContext;
        this.springResources = springResources;
        if (refresh)
        {
            refresh();
        }
    }

    /**
     * Sets up TransientRegistry SpringRegistry parent relationship here. This is
     * required here before "refresh()" rather than in the configuration builder
     * after parsing the spring config because spring executes the initialize phase
     * for objects it manages during "refresh()" and during intialization of mule
     * artifacts need to be able to lookup other artifacts both in TransientRegistry
     * and in spring (using SpringRegistry facade) by using the mule Registry
     * interface.
     *
     * @param registry
     */
    protected void setupParentSpringRegistry(Registry registry)
    {
        registry.setParent(new SpringRegistry(this));
    }

    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.prepareBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(new MuleContextPostProcessor(muleContext));
    }

    private Resource[] convert(ConfigResource[] resources)
    {
        Resource[] configResources = new Resource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            ConfigResource resource = resources[i];
            if(resource.getUrl()!=null)
            {
                configResources[i] = new UrlResource(resource.getUrl());
            }
            else
            {
                try
                {
                    configResources[i] = new InputStreamResource(resource.getInputStream());
                }
                catch (IOException e)
                {
                    //ignore, should never happen
                }
            }
        }
        return configResources;
    }

    //@Override
    protected Resource[] getConfigResources()
    {
        return springResources;
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        XmlBeanDefinitionReader beanDefinitionReader;

        //If the migration module is on the classpath, lets use the MuleBeanDefinitionReader, that allws use
        //to process Mule 1.x configuration as well as Mule 2.x.
        if (ClassUtils.isClassOnPath(LEGACY_BEAN_READER_CLASS, getClass()))
        {
            try
            {
                beanDefinitionReader = (XmlBeanDefinitionReader) ClassUtils.instanciateClass(
                        LEGACY_BEAN_READER_CLASS, new Object[] {beanFactory, springResources});
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        }
        //hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(MuleBeanDefinitionDocumentReader.class);
        //add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
        beanDefinitionReader.loadBeanDefinitions(springResources);
    }

    //@Override
    protected DefaultListableBeanFactory createBeanFactory()
    {
        //Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
        DefaultListableBeanFactory bf = super.createBeanFactory();
        if(getParent()!=null)
        {
            //Copy over all processors
            AbstractBeanFactory beanFactory = (AbstractBeanFactory)getParent().getAutowireCapableBeanFactory();
            bf.copyConfigurationFrom(beanFactory);
        }
        return bf;
    }

}
