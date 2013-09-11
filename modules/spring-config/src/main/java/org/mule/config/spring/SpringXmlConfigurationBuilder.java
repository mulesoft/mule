/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.Startable;
import org.mule.api.registry.Registry;
import org.mule.config.ConfigResource;
import org.mule.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.config.i18n.MessageFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a
 * Spring XML Configuration file used with Mule name-spaces. Multiple configuration
 * files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    public static final String MULE_DEFAULTS_CONFIG = "default-mule-config.xml";
    public static final String MULE_SPRING_CONFIG = "mule-spring-config.xml";

    /** Prepend "default-mule-config.xml" to the list of config resources. */
    protected boolean useDefaultConfigResource = true;

    protected Registry registry;

    protected ApplicationContext parentContext;

    public SpringXmlConfigurationBuilder(String[] configResources) throws ConfigurationException
    {
        super(configResources);
    }

    public SpringXmlConfigurationBuilder(String configResources) throws ConfigurationException
    {
        super(configResources);
    }

    public SpringXmlConfigurationBuilder(ConfigResource[] configResources)
    {
        super(configResources);
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        ConfigResource[] allResources;
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

    public void unconfigure(MuleContext muleContext)
    {
        registry.dispose();
        if (muleContext != null)
        {
        	muleContext.removeRegistry(registry);
        }
        registry = null;
        configured = false;
    }

    protected ApplicationContext createApplicationContext(MuleContext muleContext,
                                                          ConfigResource[] configResources) throws Exception
    {
        return new MuleApplicationContext(muleContext, configResources);
    }

    protected void createSpringRegistry(MuleContext muleContext, ApplicationContext applicationContext)
        throws Exception
    {
        if (parentContext != null)
        {
            if (applicationContext instanceof ConfigurableApplicationContext)
            {
                registry = new SpringRegistry((ConfigurableApplicationContext) applicationContext,
                    parentContext, muleContext);
            }
            else
            {
                throw new ConfigurationException(
                    MessageFactory.createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
            }
        }
        else
        {
            registry = new SpringRegistry(applicationContext, muleContext);
        }

        // Note: The SpringRegistry must be created before
        // applicationContext.refresh() gets called because
        // some beans may try to look up other beans via the Registry during
        // preInstantiateSingletons().
        muleContext.addRegistry(registry);
        registry.initialise();
    }

    @Override
    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception
    {
        // If the MuleContext is started, start all objects in the new Registry.
        if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME))
        {
            lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
        }
    }

    public boolean isUseDefaultConfigResource()
    {
        return useDefaultConfigResource;
    }

    public void setUseDefaultConfigResource(boolean useDefaultConfigResource)
    {
        this.useDefaultConfigResource = useDefaultConfigResource;
    }

    protected ApplicationContext getParentContext()
    {
        return parentContext;
    }

    public void setParentContext(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

}
