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
import org.mule.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.config.ConfigResource;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configures Mule from a configuration resource or comma seperated list of configuration resources by
 * auto-detecting the ConfigurationBuilder to use for each resource. This is resolved by either checking the
 * classpath for config modules e.g. spring-config or by using the file extention or a combination.
 */
public class AutoConfigurationBuilder extends AbstractResourceConfigurationBuilder implements DomainMuleContextAwareConfigurationBuilder
{
    private MuleContext domainContext;
    private ConfigurationBuilderService configurationBuilderService = new MuleConfigurationBuilderService();

    public AutoConfigurationBuilder(String resource) throws ConfigurationException
    {
        super(resource);
    }

    public AutoConfigurationBuilder(String[] resources) throws ConfigurationException
    {
        super(resources);
    }

    public AutoConfigurationBuilder(ConfigResource[] resources)
    {
        super(resources);
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws ConfigurationException
    {
        autoConfigure(muleContext, configResources);
    }

    protected void autoConfigure(MuleContext muleContext, ConfigResource[] resources) throws ConfigurationException
    {
        Map<String, List<ConfigResource>> configsMap = new LinkedHashMap<String, List<ConfigResource>>();

        for (int i = 0; i < resources.length; i++)
        {
            String configExtension = StringUtils.substringAfterLast(
                (resources[i]).getUrl().getFile(), ".");
            List<ConfigResource> configs = configsMap.get(configExtension);
            if (configs == null)
            {
                configs = new ArrayList<ConfigResource>();
                configsMap.put(configExtension, configs);
            }
            configs.add(resources[i]);
        }

        try
        {
            for (Map.Entry<String, List<ConfigResource>> e : configsMap.entrySet())
            {
                String extension = e.getKey();
                List<ConfigResource> configs = e.getValue();

                ConfigurationBuilder cb = configurationBuilderService.createConfigurationBuilder(extension, domainContext, configs);

                cb.configure(muleContext);
            }
        }
        catch (ConfigurationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setDomainContext(MuleContext domainContext)
    {
        this.domainContext  = domainContext;
    }

    public void setConfigurationBuilderService(ConfigurationBuilderService configurationBuilderService)
    {
        this.configurationBuilderService = configurationBuilderService;
    }
}
