/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.extras.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ConfigurationException;
import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.UMOContainerContext;
import org.mule.util.ClassHelper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * <code>SpringContainerContext</code> is a Spring Context that can expose spring-managed
 * components for use in the Mule framework.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SpringContainerContext implements UMOContainerContext, ApplicationContextAware
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SpringContainerContext.class);

    /**
     * the application contect to use when resolving components
     */
    protected ApplicationContext applicationContext;

    protected ApplicationContext externalContext;

    protected String configFile;

    /**
     * Sets the spring application context used to build components
     *
     * @param applicationContext the context to use
     */
    public void setApplicationContext(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public void setExternalApplicationContext(ApplicationContext applicationContext)
    {
        this.externalContext = applicationContext;
    }

    /**
     * The spring application context used to build components
     *
     * @return spring application context
     */
    public ApplicationContext getApplicationContext()
    {
        if(externalContext !=null) return externalContext;
        return applicationContext;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.model.UMOContainerContext#getComponent(java.lang.Object)
     */
    public Object getComponent(Object key) throws ComponentNotFoundException
    {
        if (getApplicationContext() == null)
        {
            throw new IllegalStateException("Spring Application context has not been set");
        }
        if (key == null)
        {
            throw new ComponentNotFoundException("Component not found for null key");
        }

        if (key instanceof Class)
        {
            //We will assume that there should only be one object of
            //this class in the container for now
            String[] names = getApplicationContext().getBeanDefinitionNames((Class) key);
            if (names == null || names.length == 0 || names.length > 1)
            {
                throw new ComponentNotFoundException("The container is unable to build single instance of " +
                        ((Class) key).getName() + " number of instances found was: " +
                        names.length);
            }
            else
            {
                key = names[0];
            }
        }
        try
        {
            return getApplicationContext().getBean(key.toString());
        }
        catch (BeansException e)
        {
            throw new ComponentNotFoundException("Component not found for key: " + key.toString(), e);
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
        try
        {
            if(ClassHelper.getResource(configFile, getClass())==null) {
                logger.warn("Spring config resource: " + configFile + " not found on class path, attempting to load it from local file");
                setExternalApplicationContext(new FileSystemXmlApplicationContext(configFile));
            } else {
                logger.info("Loading Spring config from classpath, resource is: " + configFile);
                setExternalApplicationContext(new ClassPathXmlApplicationContext(configFile));
            }
        } catch (BeansException e)
        {
            throw new ConfigurationException("Failed to load Application Context: " + e.getMessage(), e);
        }
    }
}
