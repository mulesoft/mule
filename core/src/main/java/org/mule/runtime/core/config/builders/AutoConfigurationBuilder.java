/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.builders;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configures Mule from a configuration resource or comma seperated list of configuration resources by
 * auto-detecting the ConfigurationBuilder to use for each resource. This is resolved by either checking the
 * classpath for config modules e.g. spring-config or by using the file extention or a combination.
 */
public class AutoConfigurationBuilder extends AbstractResourceConfigurationBuilder implements DomainMuleContextAwareConfigurationBuilder
{
    private MuleContext domainContext;

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
        autoConfigure(muleContext, artifcatConfigResources);
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
            Properties props = new Properties();
            props.load(ClassUtils.getResource("configuration-builders.properties", this.getClass()).openStream());

            for (Map.Entry<String, List<ConfigResource>> e : configsMap.entrySet())
            {
                String extension = e.getKey();
                List<ConfigResource> configs = e.getValue();

                String className = (String) props.get(extension);

                if (className == null || !ClassUtils.isClassOnPath(className, this.getClass()))
                {
                    throw new ConfigurationException(
                        CoreMessages.configurationBuilderNoMatching(createConfigResourcesString()));
                }

                ConfigResource[] constructorArg = new ConfigResource[configs.size()];
                System.arraycopy(configs.toArray(), 0, constructorArg, 0, configs.size());
                ConfigurationBuilder cb = (ConfigurationBuilder) ClassUtils.instanciateClass(className, new Object[] {constructorArg});
                if (domainContext != null && cb instanceof DomainMuleContextAwareConfigurationBuilder)
                {
                    ((DomainMuleContextAwareConfigurationBuilder) cb).setDomainContext(domainContext);
                }
                else if (domainContext != null)
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("ConfigurationBuilder %s does not support domain context", cb.getClass().getCanonicalName())));
                }
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
}
