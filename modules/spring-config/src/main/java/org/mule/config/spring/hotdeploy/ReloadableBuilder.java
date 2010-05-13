/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.hotdeploy;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.ConfigResource;
import org.mule.config.StartupContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class ReloadableBuilder extends SpringXmlConfigurationBuilder
{

    /**
     * A logical root for this application.
     */
    protected static final ClassLoader CLASSLOADER_ROOT = Thread.currentThread().getContextClassLoader();

    protected static final URL[] CLASSPATH_EMPTY = new URL[0];

    protected final transient Log logger = LogFactory.getLog(getClass());

    // TODO multiple resource monitoring
    protected File monitoredResource;

    public ReloadableBuilder(ConfigResource[] configResources)
    {
        super(configResources);
    }

    public ReloadableBuilder(String s) throws ConfigurationException
    {
        super(s);
    }


    public ReloadableBuilder(String[] strings) throws ConfigurationException
    {
        super(strings);
    }


    @Override
    public void configure(final MuleContext muleContext) throws ConfigurationException
    {
        final boolean redeploymentEnabled = !StartupContext.get().getStartupOptions().containsKey("production");

        try
        {
            // TODO dup
            final ConfigResource[] allResources;
            if (useDefaultConfigResource)
            {
                allResources = new ConfigResource[configResources.length + 2];
                allResources[0] = new ConfigResource(MULE_SPRING_CONFIG);
                allResources[1] = new ConfigResource(MULE_DEFAULTS_CONFIG);
                System.arraycopy(configResources, 0, allResources, 2, configResources.length);
                // need getUrl().getFile(), otherwise lastModified timestamp always returns 0
                this.monitoredResource = new File(allResources[2].getUrl().getFile());
            }
            else
            {
                allResources = new ConfigResource[configResources.length + 1];
                allResources[0] = new ConfigResource(MULE_SPRING_CONFIG);
                System.arraycopy(configResources, 0, allResources, 1, configResources.length);
                // need getUrl().getFile(), otherwise lastModified timestamp always returns 0
                this.monitoredResource = new File(allResources[1].getUrl().getFile());
            }
            // end dup

            // TODO this is really a job of a deployer and deployment descriptor info
            /*ClassLoader parent = MuleBootstrapUtils.isStandalone()
                                                        ? new DefaultMuleSharedDomainClassLoader(CLASSLOADER_ROOT)
                                                        : CLASSLOADER_ROOT;
            ClassLoader cl = new MuleApplicationClassLoader(this.monitoredResource, parent);
            Thread.currentThread().setContextClassLoader(cl);*/

            super.configure(muleContext);
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }

    protected void doConfigure(final MuleContext muleContext) throws Exception
    {
        final ConfigResource[] allResources;
        if (useDefaultConfigResource)
        {
            allResources = new ConfigResource[configResources.length + 2];
            allResources[0] = new ConfigResource(MULE_SPRING_CONFIG);
            allResources[1] = new ConfigResource(MULE_DEFAULTS_CONFIG);
            System.arraycopy(configResources, 0, allResources, 2, configResources.length);
        }
        else
        {
            allResources = new ConfigResource[configResources.length + 1];
            allResources[0] = new ConfigResource(MULE_SPRING_CONFIG);
            System.arraycopy(configResources, 0, allResources, 1, configResources.length);
        }

        createSpringRegistry(muleContext, createApplicationContext(muleContext, allResources));

    }

}
