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
import org.mule.util.ClassUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Provides {@link ConfigurationBuilder} instances that are declared in the configuration-builders.properties
 * file.
 */
public class MuleConfigurationBuilderService implements ConfigurationBuilderService
{

    private final LoadingCache<String, ConfigurationBuilderFactory> configurationBuilderFactories = CacheBuilder.newBuilder().build(new CacheLoader<String, ConfigurationBuilderFactory>()
    {
        @Override
        public ConfigurationBuilderFactory load(String fileExtension) throws Exception
        {
            return createConfigurationBuilderFactory(fileExtension);
        }
    });

    @Override
    public ConfigurationBuilder createConfigurationBuilder(String fileExtension, MuleContext domainContext, List<ConfigResource> configs) throws ConfigurationException
    {
        try
        {
            ConfigurationBuilderFactory configurationBuilderFactory = configurationBuilderFactories.get(fileExtension);

            return configurationBuilderFactory.createConfigurationBuilder(domainContext, configs);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof ConfigurationException)
            {
                throw (ConfigurationException) e.getCause();
            }
            else
            {
                throw new ConfigurationException(e);
            }
        }
    }

    private ConfigurationBuilderFactory createConfigurationBuilderFactory(String fileExtension) throws ConfigurationException
    {
        Properties props = new Properties();
        try
        {
            props.load(ClassUtils.getResource("configuration-builders.properties", this.getClass()).openStream());
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }

        String className = (String) props.get(fileExtension);

        return new MuleConfigurationBuilderFactory(className);
    }

}
