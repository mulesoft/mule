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
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract {@link ConfigurationBuilder} implementation used for
 * ConfigurationBuider's that use one of more configuration resources of the same
 * type that are defined using strings. <br/><br/> TODO Extend/improve this to use
 * some sort of <code>Resource</code> abstraction that supports files, uri's etc.
 */
public abstract class AbstractResourceConfigurationBuilder extends AbstractConfigurationBuilder
{

    protected static final Log logger = LogFactory.getLog(AutoConfigurationBuilder.class);

    protected String[] configResources;

    /**
     * @param configResources a comma separated list of configuration files to load,
     *            this should be accessible on the classpath or filesystem
     */
    public AbstractResourceConfigurationBuilder(String configResources)
    {
        this.configResources = StringUtils.splitAndTrim(configResources, ",; ");
    }

    /**
     * @param configResources an array of configuration files to load, this should be
     *            accessible on the classpath or filesystem
     */
    public AbstractResourceConfigurationBuilder(String[] configResources)
    {
        this.configResources = configResources;
    }

    /**
     * Override to check for existence of configResouce before invocation, and set
     * resources n configuration afterwards.
     */
    public void configure(MuleContext muleContext) throws ConfigurationException
    {
        if (configResources == null)
        {
            throw new ConfigurationException(CoreMessages.objectIsNull("Configuration Resource"));
        }

        super.configure(muleContext);

        logger.info(CoreMessages.configurationBuilderSuccess(this, createConfigResourcesString()));
        
        muleContext.getRegistry().getConfiguration().setConfigResources(configResources);
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

    protected String createConfigResourcesString()
    {
        StringBuffer configResourcesString = new StringBuffer();
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
