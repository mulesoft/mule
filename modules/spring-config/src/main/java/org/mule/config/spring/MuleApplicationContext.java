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

import org.mule.registry.Registry;
import org.mule.umo.UMOManagementContext;
import org.mule.util.ClassUtils;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * <code>MuleApplicationContext</code> is a simple extension application context
 * that allows resources to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 *
 */
public class MuleApplicationContext extends AbstractXmlApplicationContext
{
    public static final String LEGACY_BEAN_READER_CLASS = "org.mule.config.spring.MuleBeanDefinitionReader";

    private UMOManagementContext managementContext;
    private final Resource[] configResources;
    private final String[] configLocations;

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     * 
     * @param registry
     * @param configLocations
     * @see org.mule.config.spring.SpringRegistry
     */
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, String[] configLocations)
    {
        this(managementContext, registry, configLocations, true);
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
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, String[] configLocations, ApplicationContext parent)
    {
        super(parent);
        setupParentSpringRegistry(registry);
        this.managementContext = managementContext;
        this.configLocations = configLocations;
        this.configResources = null;
        refresh();
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
        beanFactory.addBeanPostProcessor(new ManagementContextPostProcessor(managementContext));
    }
    
    /**
     * @param registry
     * @param configLocations
     * @deprecated Do we need all these constructors when only our
     *             SpringConfigurationBuilder creates this?
     */
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, Resource[] configResources)
    {
        this(managementContext, registry, configResources, true);
    }

    /**
     * @param registry
     * @param configLocations
     * @param refresh
     * @throws BeansException
     * @deprecated Do we need all these constructors when only our
     *             SpringConfigurationBuilder creates this?
     */
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, String[] configLocations, boolean refresh)
        throws BeansException
    {
        this.managementContext = managementContext;
        setupParentSpringRegistry(registry);
        this.configLocations = configLocations;
        this.configResources = null;
        if (refresh)
        {
            refresh();
        }
    }

    /**
     * @param registry
     * @param configLocations
     * @param parent 
     * @deprecated Do we need all these constructors when only our
     *             SpringConfigurationBuilder creates this?
     */
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, Resource[] configResources, ApplicationContext parent)
    {
        super(parent);
        this.managementContext = managementContext;
        setupParentSpringRegistry(registry);
        this.configLocations = null;
        this.configResources = configResources;
        refresh();
    }

    /**
     * @param registry
     * @param configLocations
     * @param refresh
     * @throws BeansException 
     * @deprecated Do we need all these constructors when only our
     *             SpringConfigurationBuilder creates this?
     */
    public MuleApplicationContext(UMOManagementContext managementContext, Registry registry, Resource[] configResources, boolean refresh)
        throws BeansException
    {
        setupParentSpringRegistry(registry);
        this.managementContext = managementContext;
        this.configLocations = null;
        this.configResources = configResources;
        if (refresh)
        {
            refresh();
        }
    }

    //@Override
    protected Resource[] getConfigResources()
    {
        return configResources;
    }

    //@Override
    protected String[] getConfigLocations()
    {
        return configLocations;
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
                        LEGACY_BEAN_READER_CLASS, new Object[] {beanFactory, configLocations});
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
        beanDefinitionReader.loadBeanDefinitions(configLocations);
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
