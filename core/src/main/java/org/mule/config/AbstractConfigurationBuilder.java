/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.umo.UMOManagementContext;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * A support class for {@link org.mule.config.ConfigurationBuilder} implementations that handles the logic of creating
 * config arrays and {@link java.util.Properties} arguments
 *
 * @see org.mule.config.ConfigurationBuilder
 */
public abstract class AbstractConfigurationBuilder implements ConfigurationBuilder
{

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources a comma separated list of configuration files to load,
     *                        this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String configResources) throws ConfigurationException
    {
        return configure(StringUtils.splitAndTrim(configResources, ",; "), new Properties());
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources   - A comma-separated list of configuration files to
     *                          load, these should be accessible on the classpath or filesystem
     * @param startupProperties - Optional properties to be set before configuring
     *                          the Mule server. This is useful for managing different environments
     *                          (dev, test, production)
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String configResources, Properties startupProperties) throws ConfigurationException
    {
        return configure(StringUtils.splitAndTrim(configResources, ",; "), startupProperties);
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources an array of configuration files to load,
     *                        this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String[] configResources) throws ConfigurationException
    {
        return configure(configResources, new Properties());
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources       - An array list of configuration files to
     *                              load, these should be accessible on the classpath or filesystem
     * @param startupPropertiesFile - An optional file containing startup properties.
     *                              This is useful for managing different environments (dev, test,
     *                              production)
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String[] configResources, String startupPropertiesFile) throws ConfigurationException
    {
        try
        {
            Properties props = PropertiesUtils.loadProperties(startupPropertiesFile, getClass());
            return configure(configResources, props);
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources       - A comma-separated list of configuration files to
     *                              load, these should be accessible on the classpath or filesystem
     * @param startupPropertiesFile - An optional file containing startup properties.
     *                              This is useful for managing different environments (dev, test,
     *                              production)
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String configResources, String startupPropertiesFile) throws ConfigurationException
    {
        try
        {
            if (startupPropertiesFile != null)
            {
                Properties props = PropertiesUtils.loadProperties(startupPropertiesFile, getClass());
                return configure(configResources, props);
            }
            else
            {
                return configure(configResources);
            }
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }
}
