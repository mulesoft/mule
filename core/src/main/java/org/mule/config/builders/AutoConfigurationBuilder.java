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
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.config.ConfigResource;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configures Mule from a configuration resource or comma seperated list of configuration resources by
 * auto-detecting the ConfigurationBuilder to use for each resource. This is resolved by either checking the
 * classpath for config modules e.g. spring-config or by using the file extention or a combination.
 */
public class AutoConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    protected static final Log logger = LogFactory.getLog(AutoConfigurationBuilder.class);

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

    protected void doConfigure(MuleContext muleContext) throws ConfigurationException
    {
        autoConfigure(muleContext, configResources);
    }

    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception
    {
        // nothing to do
    }
    
    /**
     * @param muleContext
     * @return
     * @throws ConfigurationException
     */
    protected void autoConfigure(MuleContext muleContext, ConfigResource[] configResources) throws ConfigurationException
    {

        Map configsMap = new LinkedHashMap();

        for (int i = 0; i < configResources.length; i++)
        {
            String configExtension = StringUtils.substringAfterLast(
                (configResources[i]).getUrl().getFile(), ".");
            List configs = (List) configsMap.get(configExtension);
            if (configs == null)
            {
                configs = new ArrayList();
                configsMap.put(configExtension, configs);
            }
            configs.add(configResources[i]);
        }

        try
        {
            Properties props = new Properties();
            props.load(ClassUtils.getResource("configuration-builders.properties", this.getClass()).openStream());

            Iterator iterator = configsMap.entrySet().iterator();
            while (iterator.hasNext())
            {
                Map.Entry e = (Map.Entry) iterator.next();
                String extension = (String) e.getKey();
                List configs = (List) e.getValue();

                String className = (String) props.get(extension);

                if (className == null || !ClassUtils.isClassOnPath(className, this.getClass()))
                {
                    throw new ConfigurationException(
                        CoreMessages.configurationBuilderNoMatching(createConfigResourcesString()));
                }

                ConfigResource[] constructorArg = new ConfigResource[configs.size()];
                System.arraycopy(configs.toArray(), 0, constructorArg, 0, configs.size());
                ConfigurationBuilder cb = (ConfigurationBuilder) ClassUtils.instanciateClass(className, new Object[] {constructorArg});
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

}
