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

import org.mule.config.AbstractConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.registry.Registry;
import org.mule.umo.UMOManagementContext;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * <code>MuleXmlConfigurationBuilder</code> Enables Mule to be loaded from as
 * Spring context. Multiple configuration files can be loaded from this builder
 * (specified as a comma-separated list) the files can be String Beans documents or
 * Mule Xml Documents or a combination of both. Any Mule Xml documents will be
 * transformed at run-time in to Spring Bean documents before the bean definitions
 * are loaded. Make sure that the DTD definitions for each of the document types are
 * declared in the documents.
 */
public class SpringXmlConfigurationBuilder extends AbstractConfigurationBuilder
{
    private String defaultConfigResource = "default-mule-config.xml";

    private boolean used = false;

    private ApplicationContext parentContext;

    public SpringXmlConfigurationBuilder()
    {
    }

    public SpringXmlConfigurationBuilder(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

    protected void doConfigure(UMOManagementContext managementContext, String[] configResources)
        throws Exception
    {
        if (configResources == null)
        {
            throw new ConfigurationException(CoreMessages.objectIsNull("Configuration Resource"));
        }
        String[] all = new String[configResources.length + 1];
        all[0] = defaultConfigResource;
        System.arraycopy(configResources, 0, all, 1, configResources.length);
        createSpringParentRegistry(managementContext, managementContext.getRegistry(), all);
        managementContext.getRegistry().getConfiguration().setConfigResources(configResources);
    }

    /**
     * Creates a Spring ApplicationContext from the configuration resources provided
     * and sets it as the parent Registry. This releationshio is setup with the
     * MuleApplicationContext constructor to ensure that the Registry can be used
     * during the initialization phase of Spring.
     * 
     * @param managementContext
     * @param registry
     * @param all
     * @see MuleApplicationContext#setupParentSpringRegistry(Registry registry
     */
    protected void createSpringParentRegistry(UMOManagementContext managementContext,
                                              Registry registry,
                                              String[] all)
    {
        try
        {
            if (parentContext != null)
            {
                new MuleApplicationContext(managementContext, registry, all, parentContext);
            }
            else
            {
                new MuleApplicationContext(managementContext, registry, all);
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

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     * 
     * @return <code>true</code> if this ConfigurationBulder has been configured
     */
    public boolean isConfigured()
    {
        return used;
    }

    /**
     * Attempt to load a configuration resource from the file system, classpath, or
     * as a URL, in that order.
     * 
     * @param configResource Mule configuration resources
     * @return an InputStream to the resource
     * @throws ConfigurationException if the resource could not be loaded by any
     *             means
     */
    protected InputStream loadConfig(String configResource) throws ConfigurationException
    {
        InputStream is;
        try
        {
            is = IOUtils.getResourceAsStream(configResource, getClass());
        }
        catch (IOException e)
        {
            throw new ConfigurationException(CoreMessages.cannotLoadFromClasspath(configResource), e);
        }

        if (is != null)
        {
            return is;
        }
        else
        {
            throw new ConfigurationException(CoreMessages.cannotLoadFromClasspath(configResource));
        }
    }

    public String getDefaultConfigResource()
    {
        return defaultConfigResource;
    }

    public void setDefaultConfigResource(String defaultConfigResource)
    {
        this.defaultConfigResource = defaultConfigResource;
    }

    public ApplicationContext getParentContext()
    {
        return parentContext;
    }

    public void setParentContext(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

}
