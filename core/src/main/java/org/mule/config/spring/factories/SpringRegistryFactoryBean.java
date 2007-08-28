/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.RegistryContext;
import org.mule.config.spring.SpringRegistry;
import org.mule.impl.registry.TransientRegistry;
import org.mule.registry.Registry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * TODO MULE-2163
 */
public class SpringRegistryFactoryBean extends AbstractFactoryBean implements ApplicationContextAware
{

    private ApplicationContext context;
    private Registry registry;

    public Class getObjectType()
    {
        return SpringRegistry.class;
    }

    protected Object createInstance() throws Exception
    {
        registry = new TransientRegistry(new SpringRegistry(context));
        RegistryContext.setRegistry(registry);
        return registry;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
    }


    //@Override
//    public void afterPropertiesSet() throws Exception
//    {
//        super.afterPropertiesSet();
//        registry.initialise();
//    }
//
//
//    //@Override
//    public void destroy() throws Exception
//    {
//        super.destroy();
//        RegistryContext.setRegistry(null);
//        registry.dispose();
//    }
}
