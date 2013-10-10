/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.registry.Registry;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.i18n.MessageFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Adds an existing Spring ApplicationContext to Mule's internal collection of Registries.
 */
public class SpringConfigurationBuilder extends AbstractConfigurationBuilder
{
    private ApplicationContext appContext;

    private ApplicationContext parentContext;
    
    public SpringConfigurationBuilder(ApplicationContext appContext)
    {
        this.appContext = appContext;
    }

    public SpringConfigurationBuilder(ConfigurableApplicationContext appContext, ApplicationContext parentContext)
    {
        this.appContext = appContext;
        this.parentContext = parentContext;
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        Registry registry;
        
        if (parentContext != null)
        {
            if (appContext instanceof ConfigurableApplicationContext)
            {
                registry = new SpringRegistry((ConfigurableApplicationContext) appContext, parentContext, muleContext);
            }
            else
            {
                throw new ConfigurationException(MessageFactory.createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
            }
        }
        else
        {
            registry = new SpringRegistry(appContext, muleContext);
        }

        // Note: The SpringRegistry must be created before applicationContext.refresh() gets called because
        // some beans may try to look up other beans via the Registry during preInstantiateSingletons().
        muleContext.addRegistry(registry);
        registry.initialise();
    }

}
