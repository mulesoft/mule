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
import org.mule.api.config.ConfigurationException;
import org.mule.api.registry.Registry;
import org.mule.config.ConfigResource;
import org.mule.config.builders.AbstractResourceConfigurationBuilder;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a
 * Spring XML Configuration file used with Mule name-spaces. Multiple configuration
 * files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    protected String defaultConfigResourceName = "default-mule-config.xml";

    protected ApplicationContext parentContext;
    
    /** Prepend "default-mule-config.xml" to the list of config resources. */
    private boolean useDefaultConfigResource = true;

    public SpringXmlConfigurationBuilder(String configResources, ApplicationContext parentContext) throws ConfigurationException
    {
        super(configResources);
        this.parentContext = parentContext;
    }

    public SpringXmlConfigurationBuilder(String[] configResources, ApplicationContext parentContext) throws ConfigurationException
    {
        super(configResources);
        this.parentContext = parentContext;
    }

    public SpringXmlConfigurationBuilder(String[] configResources) throws ConfigurationException
    {
        this(configResources, null);
    }

    public SpringXmlConfigurationBuilder(String configResources) throws ConfigurationException
    {
        this(configResources, null);
    }

    public SpringXmlConfigurationBuilder(ConfigResource[] configResources, ApplicationContext parentContext)
    {
        super(configResources);
        this.parentContext = parentContext;
    }

    public SpringXmlConfigurationBuilder(ConfigResource[] configResources)
    {
        this(configResources, null);
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        ConfigResource[] allResources;
        if (useDefaultConfigResource)
        {
            allResources = new ConfigResource[configResources.length + 1];
            allResources[0] = new ConfigResource(defaultConfigResourceName);
            System.arraycopy(configResources, 0, allResources, 1, configResources.length);
        }
        else
        {
            allResources = configResources;
        }
        createSpringParentRegistry(muleContext, muleContext.getRegistry(), allResources);
    }

    /**
     * Creates a Spring ApplicationContext from the configuration resources provided
     * and sets it as the parent Registry. This releationshio is setup with the
     * MuleApplicationContext constructor to ensure that the Registry can be used
     * during the initialization phase of Spring.
     * 
     * @param muleContext
     * @param registry
     * @param all
     * @see MuleApplicationContext#setupParentSpringRegistry(Registry registry
     */
    protected void createSpringParentRegistry(MuleContext muleContext, Registry registry, ConfigResource[] all)
    {
        try
        {
            if (parentContext != null)
            {
                new MuleApplicationContext(muleContext, registry, all, parentContext);
            }
            else
            {
                new MuleApplicationContext(muleContext, registry, all);
            }
        }
        catch (BeansException e)
        {
            // If creation of MuleApplicationContext fails, remove
            // TransientRegistry->SpringRegistry parent relationship
            registry.setParent(null);
            throw e;
        }
    }

    public void setDefaultConfigResourceName(String defaultConfigResourceName)
    {
        this.defaultConfigResourceName = defaultConfigResourceName;
    }

    public void setParentContext(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

    public boolean isUseDefaultConfigResource()
    {
        return useDefaultConfigResource;
    }

    public void setUseDefaultConfigResource(boolean useDefaultConfigResource)
    {
        this.useDefaultConfigResource = useDefaultConfigResource;
    }

}
