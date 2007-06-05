/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring;

import org.mule.RegistryContext;
import org.mule.config.CachedResource;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderInputStream;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassUtils;

import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.InputStreamResource;

/**
 * Provides an acess facade to the Spring bean factory
 * @deprecated No longer required in Mule 2.0 since Mule's Xml configuration is based on Spring
 */
public class SpringContainerContext extends AbstractContainerContext
{
    public static final String SPRING_DOCTYPE_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\">\n";
    public static final String DEFAULT_CONTAINER_NAME = "spring";

    /**
     * the application contect to use when resolving components
     */
    protected BeanFactory beanFactory;

    protected BeanFactory externalBeanFactory;

    protected String configFile;

    protected String configuration = null;

    public SpringContainerContext()
    {
        super(DEFAULT_CONTAINER_NAME);
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

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.model.UMOContainerContext#getComponent(java.lang.Object)
     */
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

    public String getConfigFile()
    {
        return configFile;
    }

    /**
     * @param configFile The configFile to set.
     */
    public void setConfigFile(String configFile) throws ConfigurationException
    {
        this.configFile = configFile;
    }

    public void configure(Reader configuration) throws ContainerException
    {
        BeanFactory bf = new XmlBeanFactory(new InputStreamResource(new ReaderInputStream(configuration)));
        setExternalBeanFactory(bf);
    }

    /**
     * Configure Spring by passing an in-memory XML Spring config.
     *
     * @param configurationXmlAsString XML config contents
     * @throws ContainerException in case of any error
     */
    public void configure(String configurationXmlAsString) throws ContainerException
    {
        final String encoding = RegistryContext.getConfiguration().getDefaultEncoding();
        try
        {
            BeanFactory bf = new XmlBeanFactory(new CachedResource(configurationXmlAsString, encoding));
            setExternalBeanFactory(bf);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ContainerException(CoreMessages.failedToConvertStringUsingEncoding(encoding), e);
        }
    }

    public void initialise() throws InitialisationException
    {
        if (configFile == null)
        {
            if (configuration != null)
            {
                try
                {
                    configure(configuration);
                    return;
                }
                catch (ContainerException e)
                {
                    throw new InitialisationException(e, this);
                }
            }
            else
            {
                return;
            }
        }

        try
        {
            if (ClassUtils.getResource(configFile, getClass()) == null)
            {
                logger.warn("Spring config resource: " + configFile
                            + " not found on class path, attempting to load it from local file");
                setExternalBeanFactory(new FileSystemXmlApplicationContext(configFile));
            }
            else
            {
                logger.info("Loading Spring config from classpath, resource is: " + configFile);
                setExternalBeanFactory(new ClassPathXmlApplicationContext(configFile));
            }
        }
        catch (BeansException e)
        {
            throw new InitialisationException(new ConfigurationException(CoreMessages.failedToLoad("Application Context: " + configFile), e), this);
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

    public String getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(String configuration)
    {
        this.configuration = configuration;
    }
}
