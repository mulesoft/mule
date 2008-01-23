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

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.util.ClassUtils;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configures Mule from a configuration resource or comma seperated list of
 * configuration resources by auto-detecting the ConfigurationBuilder to use for each
 * resource. This is resolved by either checking the classpath for config modules
 * e.g. spring-config or by using the file extention or a combination.
 */
public class AutoConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    protected static final Log logger = LogFactory.getLog(AutoConfigurationBuilder.class);

    public AutoConfigurationBuilder(String resource)
    {
        super(resource);
    }

    protected void doConfigure(MuleContext muleContext) throws ConfigurationException
    {
        int count = 0;
        for (int i = 0; i < configResources.length; i++)
        {
            if (autoConfigure(muleContext, configResources[i]))
            {
                count++;
            }
        }
        logger.info("Configured Mule using AutoConfigurationBuilder with " + count + " of "
                    + configResources.length + " configuration resouces,");
    }

    /**
     * @param muleContext
     * @param resource
     * @return
     * @throws ConfigurationException
     */
    protected boolean autoConfigure(MuleContext muleContext, String resource) throws ConfigurationException
    {

        ConfigurationBuilder configurationBuilder = null;

        boolean remoteURL = false;
        String resourceExtension = null;

        // Work out if the resouce is a non-file url
        try
        {
            URL url = new URL(resource);
            String protocol = url.getProtocol();
            if (!protocol.equals("file"))
            {
                remoteURL = true;
            }
            else
            {
                remoteURL = false;
            }
        }
        catch (MalformedURLException e1)
        {
            remoteURL = false;
        }

        // And the file name/extension
        if (!remoteURL)
        {
            String[] splitResouce = resource.split("\\.");

            // Assume simple filename is when split by "." array size is 2
            if (splitResouce.length == 2)
            {
                resourceExtension = splitResouce[1];
            }
        }

        // Resolve configuration builder
        if (remoteURL
            && ClassUtils.isClassOnPath("org.mule.galaxy.mule2.config.GalaxyConfigurationBuilder",
                this.getClass()))
        {
            try
            {
                configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                    "org.mule.galaxy.mule2.config.GalaxyConfigurationBuilder", new Object[]{resource});
            }
            catch (Exception e)
            {
                throw new ConfigurationException(e);
            }
        }
        else if (ClassUtils.isClassOnPath("org.mule.config.spring.SpringXmlConfigurationBuilder",
            this.getClass())
                 && "xml".equals(resourceExtension))
        {
            try
            {
                configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                    "org.mule.config.spring.SpringXmlConfigurationBuilder", new Object[]{resource});
            }
            catch (Exception e)
            {
                throw new ConfigurationException(e);
            }
        }
        else if (ClassUtils.isClassOnPath("org.mule.config.scripting.ScriptingConfigurationBuilder",
            this.getClass())
                 && "groovy".equals(resourceExtension))
        {
            try
            {
                configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                    "org.mule.config.spring.SpringXmlConfigurationBuilder", new Object[]{resource});
            }
            catch (Exception e)
            {
                throw new ConfigurationException(e);
            }
        }
        else
        {
            logger.warn("No configuration builders available for configuration resource: \"" + resource
                        + "\"");
            return false;
        }
        configurationBuilder.configure(muleContext);
        logger.info("Configured Mule with configuration resource \"" + resource
                    + "\" using ConfigurationBuilder \"" + configurationBuilder.getClass().getName());
        return true;
    }
}
