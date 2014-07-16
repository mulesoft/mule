/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.config.ConfigResource;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.io.IOException;

/**
 * Abstract {@link ConfigurationBuilder} implementation used for
 * ConfigurationBuider's that use one of more configuration resources of the same
 * type that are defined using strings or {@link ConfigResource} objects.  It is recommended that
 * {@link org.mule.config.ConfigResource} objects are used over strings since they can be more descriptive, but
 * Strings will be supported for quite some time.
 */
public abstract class AbstractResourceConfigurationBuilder extends AbstractConfigurationBuilder
{
    protected ConfigResource[] configResources;

    /**
     * @param configResources a comma separated list of configuration files to load,
     *            this should be accessible on the classpath or filesystem
     * @throws org.mule.api.config.ConfigurationException usually if the config resources cannot be loaded
     */
    public AbstractResourceConfigurationBuilder(String configResources) throws ConfigurationException
    {
        this.configResources = loadConfigResources(StringUtils.splitAndTrim(configResources, ",; "));
    }

    /**
     * @param configResources an array of configuration files to load, this should be
     *            accessible on the classpath or filesystem
     * @throws org.mule.api.config.ConfigurationException usually if the config resources cannot be loaded
     */
    public AbstractResourceConfigurationBuilder(String[] configResources) throws ConfigurationException
    {
        this.configResources = loadConfigResources(configResources);
    }

    /**
     * @param configResources an array Reader oject that provides acces to a configuration either locally or remotely
     */
    public AbstractResourceConfigurationBuilder(ConfigResource[] configResources)
    {
        this.configResources = configResources;
    }

    /**
     * Override to check for existence of configResouce before invocation, and set
     * resources n configuration afterwards.
     */
    @Override
    public void configure(MuleContext muleContext) throws ConfigurationException
    {
        if (configResources == null)
        {
            throw new ConfigurationException(CoreMessages.objectIsNull("Configuration Resources"));
        }

        super.configure(muleContext);

        logger.info(CoreMessages.configurationBuilderSuccess(this, createConfigResourcesString()));
    }

    protected ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException
    {
        try
        {
            configResources = new ConfigResource[configs.length];
            for (int i = 0; i < configs.length; i++)
            {
                configResources[i] = new ConfigResource(configs[i]);
            }
            return configResources;
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }

    protected String createConfigResourcesString()
    {
        StringBuilder configResourcesString = new StringBuilder();
        configResourcesString.append("[");
        for (int i = 0; i < configResources.length; i++)
        {
            configResourcesString.append(configResources[i]);
            if (i < configResources.length - 1)
            {
                configResourcesString.append(", ");
            }
        }
        configResourcesString.append("]");
        return configResourcesString.toString();
    }
}
