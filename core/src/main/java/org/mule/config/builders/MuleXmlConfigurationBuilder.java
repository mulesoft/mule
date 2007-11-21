/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.config.AbstractConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.umo.UMOManagementContext;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.context.ApplicationContext;

/**
 * <code>MuleXmlConfigurationBuilder</code> Enables Mule to be loaded from as Spring
 * context. Multiple configuration files can be loaded from this builder (specified
 * as a comma-separated list) the files can be String Beans documents or Mule Xml
 * Documents or a combination of both. Any Mule Xml documents will be transformed at
 * run-time in to Spring Bean documents before the bean definitions are loaded. Make
 * sure that the DTD definitions for each of the document types are declared in the
 * documents.
 */
public class MuleXmlConfigurationBuilder extends AbstractConfigurationBuilder
{
    private String defaultConfigResource = "default-mule-config.xml";

    private boolean used = false;

    private ApplicationContext parentContext;

    /**
     * Start the ManagementContext once it's configured (defaults to true).
     * TODO MULE-1988
     */
    private boolean startContext = true;

    public MuleXmlConfigurationBuilder()
    {
    }

    public MuleXmlConfigurationBuilder(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources   - An array list of configuration files to
     *                          load, these should be accessible on the classpath or filesystem
     * @param startupProperties - Optional properties to be set before configuring
     *                          the Mule server. This is useful for managing different environments
     *                          (dev, test, production)
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String[] configResources, Properties startupProperties) throws ConfigurationException
    {
        if (configResources == null)
        {
            throw new ConfigurationException(CoreMessages.objectIsNull("Configuration Resource"));
        }

        // Load startup properties.
        Registry registry = RegistryContext.getOrCreateRegistry();
        try
        {
            registry.registerObjects(startupProperties);
        }
        catch (RegistrationException e)
        {
            throw new ConfigurationException(e);
        }

        String[] all = new String[configResources.length + 1];
        all[0] = defaultConfigResource;
        System.arraycopy(configResources, 0, all, 1, configResources.length);


        MuleApplicationContext context;
        if (parentContext != null)
        {
            context = new MuleApplicationContext(all, parentContext);
        }
        else
        {
            context = new MuleApplicationContext(all);
        }

        try
        {
            // TODO MULE-2163 It doesn't make sense for Spring to create the Registry, it should have already been created.
            UMOManagementContext mc = context.getManagementContext();
            MuleServer.setManagementContext(mc);
            // TODO MULE-1988
            if (startContext)
            {
                mc.start();
            }

            registry.getConfiguration().setConfigResources(configResources);

            return mc;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(CoreMessages.failedToInvokeLifecycle("start", "Mule"), e);
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
     *                                means
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

    public boolean isStartContext()
    {
        return startContext;
    }

    public void setStartContext(boolean startContext)
    {
        this.startContext = startContext;
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
