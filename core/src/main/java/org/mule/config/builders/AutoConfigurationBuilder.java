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
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;

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

    public AutoConfigurationBuilder(String resource)
    {
        super(resource);
    }

    protected void doConfigure(MuleContext muleContext) throws ConfigurationException
    {
        autoConfigure(muleContext, configResources);
    }

    /**
     * @param muleContext
     * @param resource
     * @return
     * @throws ConfigurationException
     */
    protected void autoConfigure(MuleContext muleContext, String[] configResources) throws ConfigurationException
    {

        ConfigurationBuilder configurationBuilder = null;

        if (ClassUtils.isClassOnPath("org.mule.config.spring.SpringXmlConfigurationBuilder", this.getClass()))
        {
            try
            {
                configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                    "org.mule.config.spring.SpringXmlConfigurationBuilder", new Object[]{configResources});
            }
            catch (Exception e)
            {
                throw new ConfigurationException(e);
            }
        }
        else if (ClassUtils.isClassOnPath("org.mule.config.scripting.ScriptingConfigurationBuilder", this.getClass()))
        {
            try
            {
                configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(
                    "org.mule.config.spring.SpringXmlConfigurationBuilder", new Object[]{configResources});
            }
            catch (Exception e)
            {
                throw new ConfigurationException(e);
            }
        }
        else
        {
            throw new ConfigurationException(CoreMessages.configurationBuilderNoMatching(createConfigResourcesString()));
        }
        configurationBuilder.configure(muleContext);
    }

}
