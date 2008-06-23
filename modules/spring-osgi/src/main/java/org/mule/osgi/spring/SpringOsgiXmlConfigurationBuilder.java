/*
 * $Id: SpringXmlConfigurationBuilder.java 11018 2008-02-25 20:56:00Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi.spring;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.Registry;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.spring.SpringRegistry;

import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;

public class SpringOsgiXmlConfigurationBuilder extends AbstractConfigurationBuilder
{
    private String[] configLocations;
    private BundleContext bundleContext;
    
    public SpringOsgiXmlConfigurationBuilder(String[] configLocations, BundleContext bundleContext)
    {
        this.configLocations = configLocations;
        this.bundleContext = bundleContext;
    }
    
    //@Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        ApplicationContext applicationContext = new MuleOsgiApplicationContext(configLocations, muleContext, bundleContext);
        createSpringRegistry(muleContext, applicationContext);
    }

    protected void createSpringRegistry(MuleContext muleContext, ApplicationContext applicationContext) throws Exception
    {
        Registry reg = new SpringRegistry(applicationContext);
        // Note: The SpringRegistry must be created before applicationContext.refresh() gets called because
        // some beans may try to look up other beans via the Registry during preInstantiateSingletons().
        muleContext.addRegistry(1, reg);
        reg.initialise();
    }    

    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception 
    {
        // nothing to do
    }
}
