/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.ConfigResource;

import org.springframework.context.ApplicationContext;

/**
 * Spring configuration builder used to create domains.
 */
public class SpringXmlDomainConfigurationBuilder extends SpringXmlConfigurationBuilder
{

    public SpringXmlDomainConfigurationBuilder(String configResources) throws ConfigurationException
    {
        super(configResources);
        setUseMinimalConfigResource(true);
    }

    @Override
    protected ApplicationContext doCreateApplicationContext(MuleContext muleContext, ConfigResource[] configResources, OptionalObjectsController optionalObjectsController)
    {
        return new MuleDomainContext(muleContext, configResources, optionalObjectsController);
    }
}
