/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring;

import org.mule.config.CachedResource;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ArrayUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

/**
 * <code>SpringContainerContext</code> is a Spring Context that can expose
 * spring-managed components for use in the Mule framework.
 */
public class SpringContainerContext extends AbstractContainerContext implements BeanFactoryAware
{
    public static final String SPRING_DOCTYPE_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\">\n";

    /**
     * the application context to use when resolving components
     */
    protected BeanFactory beanFactory;

    protected BeanFactory externalBeanFactory;

    /** One or more Spring XML config files */
    protected String configResources;

    /** The Spring XML itself */
    protected String configXml;

    public SpringContainerContext()
    {
        super("spring");
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        if (getBeanFactory() == null)
        {
            throw new IllegalStateException("Spring Application context has not been set");
        }
        if (key == null)
        {
            throw new ObjectNotFoundException("Component not found for null key");
        }

        if (key instanceof Class)
        {
            // We will assume that there should only be one object of
            // this class in the container for now
            // String[] names = getBeanFactory().getBeanDefinitionNames((Class)
            // key);
            // if (names == null || names.length == 0 || names.length > 1)
            // {
            throw new ObjectNotFoundException("The container is unable to build single instance of "
                                              + ((Class)key).getName() + " number of instances found was: 0");
            // }
            // else
            // {
            // key = names[0];
            // }
        }
        try
        {
            return getBeanFactory().getBean(key.toString());
        }
        catch (BeansException e)
        {
            throw new ObjectNotFoundException("Component not found for key: " + key.toString(), e);
        }
    }

    //@Override
    public void configure(Reader reader) throws ContainerException
    {
        Resource[] resources;
        try 
        {
            final String encoding = managementContext.getRegistry().getConfiguration().getDefaultEncoding();
            resources = new Resource[]{ new CachedResource(reader, encoding)};
        }
        catch (IOException e)
        {
            throw new ContainerException(MessageFactory.createStaticMessage("Unable to read resource"), e);
        }
        setExternalBeanFactory(new MuleApplicationContext(resources));
    }

    public void initialise() throws InitialisationException
    {
        // Load Spring XML in-memory
        if (configXml != null)
        {
            final String encoding = managementContext.getRegistry().getConfiguration().getDefaultEncoding();
            Resource[] resources;
            try 
            {
                resources = new Resource[]{new CachedResource(configXml, encoding)};
            }
            catch (UnsupportedEncodingException e)
            {
                throw new InitialisationException(CoreMessages.failedToConvertStringUsingEncoding(encoding), e, this);
            }
            setExternalBeanFactory(new MuleApplicationContext(resources));
        }

        // Load Spring XML from one or more config files
        else if (configResources != null)
        {        
            String[] resources = StringUtils.splitAndTrim(configResources, ",");
            if (logger.isDebugEnabled())
            {
                logger.debug("There is/are " + resources.length + " configuration resource(s): " + ArrayUtils.toString(resources));
            }
            setExternalBeanFactory(new MuleApplicationContext(resources));
        }
    }

    public void dispose()
    {
        if (externalBeanFactory instanceof ConfigurableApplicationContext)
        {
            ((ConfigurableApplicationContext)externalBeanFactory).close();
        }
        super.dispose();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ///////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * The spring application context used to build components
     * 
     * @return spring application context
     */
    public BeanFactory getBeanFactory()
    {
        if (externalBeanFactory != null)
        {
            return externalBeanFactory;
        }
        return beanFactory;
    }

    /**
     * Sets the spring application context used to build components
     * 
     * @param beanFactory the context to use
     */
    public void setBeanFactory(BeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    public void setExternalBeanFactory(BeanFactory factory)
    {
        this.externalBeanFactory = factory;
    }

    /** The Spring XML itself */
    public String getConfigXml()
    {
        return configXml;
    }

    /** The Spring XML itself */
    public void setConfigXml(String configXml)
    {
        this.configXml = configXml;
    }

    /** 
     * The Spring XML itself.
     * @deprecated use getConfigXml() instead 
     */
    public String getConfiguration()
    {
        return configXml;
    }

    /** 
     * The Spring XML itself.
     * @deprecated use setConfigXml() instead 
     */
    public void setConfiguration(String configuration)
    {
        this.configXml = configuration;
    }

    /** One or more Spring XML config files */
    public String getConfigResources()
    {
        return configResources;
    }

    /** One or more Spring XML config files */
    public void setConfigResources(String configResources)
    {
        this.configResources = configResources;
    }

    /**
     * @deprecated use getConfigResources() instead
     */
    public String getConfigFile()
    {
        return configResources;
    }

    /**
     * @deprecated use setConfigResources() instead
     */
    public void setConfigFile(String configFile) throws ConfigurationException
    {
        this.configResources = configFile;
    }
}
